package com.ivy.data.sync.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.ivy.data.sync.RedisSyncDataSource
import com.ivy.data.sync.RedisSyncDataSource.Companion.BACKUP_KEY
import com.ivy.data.sync.RedisSyncDataSource.Companion.META_KEY
import com.ivy.data.sync.RemoteSnapshot
import com.ivy.data.sync.model.RemoteSyncMeta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

/**
 * Implements [RedisSyncDataSource] against the Upstash Redis REST API.
 *
 * Upstash exposes Redis commands over HTTPS:
 *  - `GET  {url}/get/{key}`         -> `{"result": "<value>" | null}`
 *  - `GET  {url}/mget/{k1}/{k2}`    -> `{"result": ["<value>" | null, ...]}`
 *  - `POST {url}/multi-exec` with a JSON array of commands -> `[{"result": "OK"}, ...]`
 *  - `GET  {url}/ping`              -> `{"result": "PONG"}`
 *  - `POST {url}/del/{key}/{key}`   -> `{"result": <count>}`
 *
 * The token is sent as `Authorization: Bearer <token>`. The full backup is stored in the request
 * body (not the path) to avoid URL length limits.
 */
class RedisSyncDataSourceImpl @Inject constructor(
    private val ktorClient: dagger.Lazy<HttpClient>,
    private val json: Json,
) : RedisSyncDataSource {

    override suspend fun ping(url: String, token: String): Either<String, Unit> = catch({
        val response = ktorClient.get().get("$url/ping") { bearer(token) }
        response.errorOrNull()?.left() ?: Unit.right()
    }) { e -> networkError(e) }

    override suspend fun getMeta(
        url: String,
        token: String,
    ): Either<String, RemoteSyncMeta?> = catch({
        val response = ktorClient.get().get("$url/get/$META_KEY") { bearer(token) }
        response.errorOrNull()?.left() ?: run {
            val raw = response.body<UpstashResult>().result.asStringOrNull()
            val meta = raw?.let { RemoteSyncMeta.parseLenient(json, it) }
            if (meta != null) return@run meta.right()
            // A backup can exist without its meta record (see RemoteSnapshot).
            val exists = ktorClient.get().get("$url/exists/$BACKUP_KEY") { bearer(token) }
            exists.errorOrNull()?.left()
                ?: RemoteSyncMeta.unknown()
                    .takeIf { exists.body<UpstashResult>().result.asLongOrNull() == 1L }
                    .right()
        }
    }) { e -> networkError(e) }

    override suspend fun getSnapshot(
        url: String,
        token: String,
    ): Either<String, RemoteSnapshot> = catch({
        val response = ktorClient.get().get("$url/mget/$BACKUP_KEY/$META_KEY") { bearer(token) }
        response.errorOrNull()?.left() ?: run {
            val values = response.body<UpstashResult>().result as? JsonArray
                ?: return@run "Unexpected reply from Upstash".left()
            val storedBackup = values.getOrNull(0).asStringOrNull()
            RemoteSnapshot(
                storedBackup = storedBackup,
                meta = values.getOrNull(1).asStringOrNull()
                    ?.let { RemoteSyncMeta.parseLenient(json, it) }
                    ?: storedBackup?.let { RemoteSyncMeta.unknown() },
            ).right()
        }
    }) { e -> networkError(e) }

    override suspend fun putBackup(
        url: String,
        token: String,
        storedBackup: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit> = catch({
        // Both keys are written in one MULTI/EXEC transaction so the backup and its meta record
        // can never disagree if the connection drops between them.
        val commands = listOf(
            listOf("SET", BACKUP_KEY, storedBackup),
            listOf("SET", META_KEY, json.encodeToString(RemoteSyncMeta.serializer(), meta)),
        )
        val response = ktorClient.get().post("$url/multi-exec") {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(commandsSerializer, commands))
        }
        response.errorOrNull()?.left() ?: run {
            val results = response.body<List<UpstashResult>>()
            results.firstNotNullOfOrNull { it.error }
                ?.let { "Upstash error: $it".left() }
                ?: Unit.right()
        }
    }) { e -> networkError(e) }

    override suspend fun deleteBackup(url: String, token: String): Either<String, Unit> = catch({
        val response = ktorClient.get().post("$url/del/$BACKUP_KEY/$META_KEY") { bearer(token) }
        response.errorOrNull()?.left() ?: Unit.right()
    }) { e -> networkError(e) }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun HttpResponse.errorOrNull(): String? = when {
        status == HttpStatusCode.Unauthorized ->
            "Unauthorized — double-check your Upstash REST token"

        status == HttpStatusCode.NotFound ->
            "Not found — double-check your Upstash REST URL"

        // Upstash explains other failures (e.g. "max request size exceeded") in the body.
        !status.isSuccess() -> upstashErrorText()?.let { "Upstash error: $it" }
            ?: "Upstash error (${status.value})"

        else -> null
    }

    private suspend fun HttpResponse.upstashErrorText(): String? = try {
        json.decodeFromString<UpstashResult>(bodyAsText()).error
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun JsonElement?.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonElement?.asLongOrNull(): Long? =
        (this as? JsonPrimitive)?.takeIf { !it.isString }?.content?.toLongOrNull()

    private fun networkError(e: Throwable): Either<String, Nothing> =
        (e.message ?: "Network error — check the URL and your connection").left()

    @Serializable
    @Suppress("DataClassDefaultValues")
    private data class UpstashResult(
        // Upstash returns only one of these per response, so both need defaults to deserialize.
        val result: JsonElement? = null,
        val error: String? = null,
    )

    companion object {
        private val commandsSerializer = ListSerializer(ListSerializer(String.serializer()))
    }
}
