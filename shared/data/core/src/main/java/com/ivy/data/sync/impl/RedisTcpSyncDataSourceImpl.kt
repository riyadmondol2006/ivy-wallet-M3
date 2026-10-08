package com.ivy.data.sync.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.sync.RedisConnectionString
import com.ivy.data.sync.RedisSyncDataSource
import com.ivy.data.sync.RedisSyncDataSource.Companion.BACKUP_KEY
import com.ivy.data.sync.RedisSyncDataSource.Companion.META_KEY
import com.ivy.data.sync.RemoteSnapshot
import com.ivy.data.sync.SyncErrorMessages
import com.ivy.data.sync.model.RemoteSyncMeta
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Implements [RedisSyncDataSource] over a native Redis TLS (TCP) connection, for users who prefer
 * the standard Redis endpoint over the REST API. Speaks a minimal subset of the RESP protocol
 * (AUTH/PING/GET/SET/DEL) directly over an [SSLSocket] — no third-party Redis client needed.
 *
 * The `url` is anything [RedisConnectionString] reads (`rediss://…`, `redis://…`, a pasted
 * `redis-cli --tls -u …` command or `host:port`), and `token` is the password, used when the URL
 * has none. The connection always uses TLS, as Upstash requires.
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
                ?.let { RemoteSyncMeta.parseLenient(json, it) }
                // A backup can exist without its meta record (see RemoteSnapshot).
                ?: RemoteSyncMeta.unknown()
                    .takeIf { conn.command("EXISTS", BACKUP_KEY).asInteger() == 1L }
        }

    override suspend fun getSnapshot(url: String, token: String): Either<String, RemoteSnapshot> =
        withConnection(url, token) { conn ->
            val values = conn.command("MGET", BACKUP_KEY, META_KEY).asArray()
            val storedBackup = values.getOrNull(0)?.asBulk()
            RemoteSnapshot(
                storedBackup = storedBackup,
                meta = values.getOrNull(1)?.asBulk()?.let { RemoteSyncMeta.parseLenient(json, it) }
                    ?: storedBackup?.let { RemoteSyncMeta.unknown() },
            )
        }

    override suspend fun putBackup(
        url: String,
        token: String,
        storedBackup: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit> = withConnection(url, token) { conn ->
        // Write both keys atomically so the backup and its meta record can never disagree.
        conn.command("MULTI").asStatus()
        conn.command("SET", BACKUP_KEY, storedBackup).asStatus()
        conn.command("SET", META_KEY, json.encodeToString(RemoteSyncMeta.serializer(), meta))
            .asStatus()
        val results = conn.command("EXEC").asArray()
        results.forEach { it.asStatus() }
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
            val target = RedisConnectionString.parse(url)
                ?: return@catch "Enter the Redis endpoint, e.g. rediss://host:6379".left()
            val password = target.password ?: token.trim()
            openTlsSocket(target.host, target.port).use { socket ->
                val conn = RespConnection(
                    input = BufferedInputStream(socket.inputStream),
                    output = socket.outputStream,
                )
                conn.authenticate(target.user, password)
                block(conn).right()
            }
        }) { e ->
            when (e) {
                is RedisServerException -> SyncErrorMessages.describeRedisReply(e.message.orEmpty())
                else -> SyncErrorMessages.describe(e)
            }.left()
        }
    }

    private fun openTlsSocket(host: String, port: Int): SSLSocket {
        val socket = SSLSocketFactory.getDefault().createSocket() as SSLSocket
        // A raw SSLSocket only checks the certificate chain. Also check that the certificate is
        // for this host, or any valid certificate could intercept the password and the backup.
        socket.sslParameters = socket.sslParameters.apply {
            endpointIdentificationAlgorithm = "HTTPS"
        }
        socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        socket.soTimeout = READ_TIMEOUT_MS
        socket.startHandshake()
        return socket
    }

    companion object {
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

            '*' -> {
                val count = line.toInt()
                if (count < 0) RespReply.Array(emptyList()) else RespReply.Array(List(count) { readReply() })
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
    data class Array(val items: List<RespReply>) : RespReply

    fun asStatus(): String = (this as? Status)?.value
        ?: error("Expected a Redis status reply but got $this")

    fun asInteger(): Long = (this as? Integer)?.value
        ?: error("Expected a Redis integer reply but got $this")

    fun asArray(): List<RespReply> = (this as? Array)?.items
        ?: error("Expected a Redis array reply but got $this")

    fun asBulk(): String? = when (this) {
        is Bulk -> value
        is Status -> value
        else -> error("Expected a Redis bulk reply but got $this")
    }
}

private class RedisServerException(message: String) : Exception(message)
