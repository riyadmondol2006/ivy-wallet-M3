package com.ivy.data.sync

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Turns the exceptions and Redis replies behind a failed cloud sync into sentences a user can act
 * on. Raw exception messages are often just a host name or a protocol code.
 */
object SyncErrorMessages {
    private const val FALLBACK = "Connection error"

    fun describe(e: Throwable): String = when (e) {
        is UnknownHostException ->
            "No internet connection, or the host ${e.message.orEmpty()} doesn't exist"

        is SocketTimeoutException, is ConnectException ->
            "Could not connect to the database. Check your connection and try again"

        is SSLException -> "Secure connection failed: ${e.message ?: "certificate problem"}"
        else -> e.message?.takeIf { it.isNotBlank() } ?: FALLBACK
    }

    /** A Redis error reply such as `WRONGPASS invalid username-password pair`. */
    fun describeRedisReply(reply: String): String = when {
        reply.startsWith("WRONGPASS") -> "Wrong password — double-check it in the Upstash console"
        reply.startsWith("NOAUTH") -> "This database needs a password"
        reply.startsWith("NOPERM") -> "This password isn't allowed to read or write backups"
        else -> "Redis error: $reply"
    }
}
