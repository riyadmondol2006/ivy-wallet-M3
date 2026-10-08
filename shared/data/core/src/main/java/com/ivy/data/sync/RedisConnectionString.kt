package com.ivy.data.sync

import java.net.URI
import java.net.URISyntaxException

/**
 * A Redis server address read from what the user typed or pasted.
 *
 * @param password the password embedded in the input, or null when it has none.
 */
data class RedisAddress(
    val host: String,
    val port: Int,
    val user: String,
    val password: String?,
) {
    /** The TLS URL for the TCP connection, without credentials. */
    val tlsUrl: String get() = "rediss://$host:$port"

    /** The Upstash REST URL for the same database (Upstash serves REST on the same host). */
    val restUrl: String get() = "https://$host"
}

/**
 * Reads the forms in which Upstash and other providers show a Redis connection:
 *  - `redis-cli --tls -u redis://default:PASSWORD@host:6379` (Upstash console "Connect" tab)
 *  - `rediss://default:PASSWORD@host:6379` or `redis://…` (client libraries)
 *  - `host:port` or a bare `host`
 *
 * The app always connects with TLS, so `redis://` is accepted and upgraded rather than rejected.
 */
object RedisConnectionString {
    private const val DEFAULT_PORT = 6379
    private const val DEFAULT_USER = "default"
    private val SCHEME_URL = Regex("""redis(s)?://\S+""", RegexOption.IGNORE_CASE)

    /** Returns the address in [input], or null when it is blank or not a Redis address. */
    fun parse(input: String): RedisAddress? {
        val trimmed = input.trim()
        // A pasted redis-cli command: take the URL after -u (or any redis:// URL in it).
        val url = SCHEME_URL.find(trimmed)?.value
        return when {
            url != null -> parseUrl(url)
            trimmed.isEmpty() || trimmed.contains("://") || trimmed.contains(' ') -> null
            else -> parseHostPort(trimmed)
        }
    }

    /** True when [input] looks like a Redis connection string rather than a REST URL. */
    fun isRedisConnectionString(input: String): Boolean =
        SCHEME_URL.containsMatchIn(input.trim())

    private fun parseUrl(url: String): RedisAddress? = try {
        val uri = URI(url)
        val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
        val userInfo = uri.rawUserInfo?.split(":", limit = 2).orEmpty().map(::decode)
        val (user, password) = when (userInfo.size) {
            // "redis://:password@host" or "redis://password@host"
            1 -> DEFAULT_USER to userInfo[0]
            2 -> userInfo[0].ifBlank { DEFAULT_USER } to userInfo[1]
            else -> DEFAULT_USER to null
        }
        RedisAddress(
            host = host,
            port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT,
            user = user,
            password = password?.takeIf { it.isNotBlank() },
        )
    } catch (_: URISyntaxException) {
        null
    }

    private fun parseHostPort(value: String): RedisAddress? {
        val host = value.substringBeforeLast(':').takeIf { ':' in value } ?: value
        val port = if (':' in value) value.substringAfterLast(':').toIntOrNull() else DEFAULT_PORT
        if (host.isBlank() || port == null || port <= 0) return null
        return RedisAddress(host = host, port = port, user = DEFAULT_USER, password = null)
    }

    private fun decode(value: String): String =
        java.net.URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8.name())
}
