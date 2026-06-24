package com.ivy.data.sync.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.sync.RedisSyncDataSource
import com.ivy.data.sync.model.RemoteSyncMeta
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import javax.inject.Inject
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Implements [RedisSyncDataSource] over a native Redis TLS (TCP) connection, for users who prefer
 * the standard Redis endpoint over the REST API. Speaks a minimal subset of the RESP protocol
 * (AUTH/PING/GET/SET/DEL) directly over an [SSLSocket] — no third-party Redis client needed.
 *
 * The `url` is either a `rediss://[user]:[password]@host:port` URL or a bare `host:port`, and
 * `token` is the password (used when not embedded in the URL). Upstash TCP endpoints always use TLS.
 */
class RedisTcpSyncDataSourceImpl @Inject constructor(
    private val json: Json,
    private val dispatchers: DispatchersProvider,
) : RedisSyncDataSource {

    override suspend fun ping(url: String, token: String): Either<String, Unit> =
        withConnection(url, token) { conn ->
            conn.command("PING").asStatus()
            Unit
        }

    override suspend fun getMeta(url: String, token: String): Either<String, RemoteSyncMeta?> =
        withConnection(url, token) { conn ->
            conn.command("GET", META_KEY).asBulk()
                ?.let { json.decodeFromString<RemoteSyncMeta>(it) }
        }

    override suspend fun getBackup(url: String, token: String): Either<String, String?> =
        withConnection(url, token) { conn ->
            conn.command("GET", BACKUP_KEY).asBulk()
        }

    override suspend fun putBackup(
        url: String,
        token: String,
        backupJson: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit> = withConnection(url, token) { conn ->
        conn.command("SET", BACKUP_KEY, backupJson).asStatus()
        conn.command("SET", META_KEY, json.encodeToString(RemoteSyncMeta.serializer(), meta))
            .asStatus()
        Unit
    }

    override suspend fun deleteBackup(url: String, token: String): Either<String, Unit> =
        withConnection(url, token) { conn ->
            conn.command("DEL", BACKUP_KEY, META_KEY)
            Unit
        }

    private suspend fun <T> withConnection(
        url: String,
        token: String,
        block: (RespConnection) -> T,
    ): Either<String, T> = withContext(dispatchers.io) {
        catch({
            val target = parseTarget(url, token)
            openTlsSocket(target.host, target.port).use { socket ->
                val conn = RespConnection(
                    input = BufferedInputStream(socket.inputStream),
                    output = socket.outputStream,
                )
                conn.authenticate(target.user, target.password)
                block(conn).right()
            }
        }) { e -> (e.message ?: "Redis connection error").left() }
    }

    private fun openTlsSocket(host: String, port: Int): SSLSocket {
        val socket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        socket.soTimeout = READ_TIMEOUT_MS
        socket.startHandshake()
        return socket
    }

    private fun parseTarget(url: String, token: String): RedisTarget {
        val trimmed = url.trim()
        return if (trimmed.startsWith("redis://") || trimmed.startsWith("rediss://")) {
            val uri = URI(trimmed)
            val userInfo = uri.userInfo?.split(":", limit = 2).orEmpty()
            val user = userInfo.getOrNull(0)?.takeIf { it.isNotBlank() } ?: DEFAULT_USER
            val password = userInfo.getOrNull(1)?.takeIf { it.isNotBlank() } ?: token.trim()
            RedisTarget(uri.host, uri.port.takeIf { it > 0 } ?: DEFAULT_PORT, user, password)
        } else {
            val host = trimmed.substringBeforeLast(':')
            val port = trimmed.substringAfterLast(':', "").toIntOrNull() ?: DEFAULT_PORT
            RedisTarget(host, port, DEFAULT_USER, token.trim())
        }
    }

    private data class RedisTarget(
        val host: String,
        val port: Int,
        val user: String,
        val password: String,
    )

    companion object {
        private const val BACKUP_KEY = "ivy_wallet_backup"
        private const val META_KEY = "ivy_wallet_meta"
        private const val DEFAULT_USER = "default"
        private const val DEFAULT_PORT = 6379
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 20_000
    }
}

/**
 * A tiny RESP (REdis Serialization Protocol) connection: writes commands as arrays of bulk strings
 * and reads back the common reply types. Blocking — call only from an IO dispatcher.
 */
private class RespConnection(
    private val input: BufferedInputStream,
    private val output: OutputStream,
) {
    fun authenticate(user: String, password: String) {
        if (password.isBlank()) return
        val reply = command("AUTH", user, password)
        reply.asStatus()
    }

    fun command(vararg args: String): RespReply {
        write(args)
        return readReply()
    }

    private fun write(args: Array<out String>) {
        val buffer = ByteArrayOutputStream()
        buffer.write("*${args.size}\r\n".toByteArray(Charsets.UTF_8))
        for (arg in args) {
            val bytes = arg.toByteArray(Charsets.UTF_8)
            buffer.write("$${bytes.size}\r\n".toByteArray(Charsets.UTF_8))
            buffer.write(bytes)
            buffer.write(CRLF)
        }
        output.write(buffer.toByteArray())
        output.flush()
    }

    private fun readReply(): RespReply {
        val prefix = input.read()
        if (prefix == -1) throw EOFException("Connection closed by server")
        val line = readLine()
        return when (prefix.toChar()) {
            '+' -> RespReply.Status(line)
            '-' -> throw RedisServerException(line)
            ':' -> RespReply.Integer(line.toLong())
            '$' -> {
                val length = line.toInt()
                if (length < 0) RespReply.Bulk(null) else RespReply.Bulk(readBulk(length))
            }

            else -> error("Unexpected Redis reply: ${prefix.toChar()}$line")
        }
    }

    private fun readBulk(length: Int): String {
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(data, offset, length - offset)
            if (read < 0) throw EOFException("Truncated Redis reply")
            offset += read
        }
        input.read() // \r
        input.read() // \n
        return String(data, Charsets.UTF_8)
    }

    private fun readLine(): String {
        val builder = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) throw EOFException("Connection closed by server")
            if (b == '\r'.code) {
                input.read() // consume \n
                break
            }
            builder.append(b.toChar())
        }
        return builder.toString()
    }

    companion object {
        private val CRLF = "\r\n".toByteArray(Charsets.UTF_8)
    }
}

private sealed interface RespReply {
    data class Status(val value: String) : RespReply
    data class Integer(val value: Long) : RespReply
    data class Bulk(val value: String?) : RespReply

    fun asStatus(): String = (this as? Status)?.value
        ?: error("Expected a Redis status reply but got $this")

    fun asBulk(): String? = when (this) {
        is Bulk -> value
        is Status -> value
        else -> error("Expected a Redis bulk reply but got $this")
    }
}

private class RedisServerException(message: String) : Exception(message)
