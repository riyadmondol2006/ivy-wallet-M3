package com.ivy.data.sync.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.right
import com.ivy.data.sync.RedisSyncDataSource
import com.ivy.data.sync.model.RemoteSyncMeta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

/**
 * Implements [RedisSyncDataSource] against the Upstash Redis REST API.
 *
 * Upstash exposes Redis commands over HTTPS:
 *  - `GET  {url}/get/{key}`  -> `{"result": "<value>" | null}`
 *  - `POST {url}/set/{key}`  with the value in the request body -> `{"result": "OK"}`
 *  - `GET  {url}/ping`       -> `{"result": "PONG"}`
 *  - `POST {url}/del/{key}`  -> `{"result": <count>}`
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
        response.toUnitOrError()
    }) { e -> networkError(e) }

    override suspend fun getMeta(
        url: String,
        token: String,
    ): Either<String, RemoteSyncMeta?> = catch({
        val response = ktorClient.get().get("$url/get/$META_KEY") { bearer(token) }
        response.errorOrNull()?.left() ?: run {
            val raw = response.body<UpstashResult>().result.asStringOrNull()
            raw?.let { json.decodeFromString<RemoteSyncMeta>(it) }.right()
        }
    }) { e -> networkError(e) }

    override suspend fun getBackup(url: String, token: String): Either<String, String?> = catch({
        val response = ktorClient.get().get("$url/get/$BACKUP_KEY") { bearer(token) }
        response.errorOrNull()?.left()
            ?: response.body<UpstashResult>().result.asStringOrNull().right()
    }) { e -> networkError(e) }

    override suspend fun putBackup(
        url: String,
        token: String,
        backupJson: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit> = catch({
        val backupResponse = ktorClient.get().post("$url/set/$BACKUP_KEY") {
            bearer(token)
            setBody(backupJson)
        }
        backupResponse.errorOrNull()?.left() ?: run {
            val metaResponse = ktorClient.get().post("$url/set/$META_KEY") {
                bearer(token)
                setBody(json.encodeToString(RemoteSyncMeta.serializer(), meta))
            }
            metaResponse.toUnitOrError()
        }
    }) { e -> networkError(e) }

    override suspend fun deleteBackup(url: String, token: String): Either<String, Unit> = catch({
        val response = ktorClient.get().post("$url/del/$BACKUP_KEY/$META_KEY") { bearer(token) }
        response.toUnitOrError()
    }) { e -> networkError(e) }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun HttpResponse.toUnitOrError(): Either<String, Unit> =
        errorOrNull()?.left() ?: Unit.right()

    private fun HttpResponse.errorOrNull(): String? = when {
        status == HttpStatusCode.Unauthorized ->
            "Unauthorized — double-check your Upstash REST token"

        status == HttpStatusCode.NotFound ->
            "Not found — double-check your Upstash REST URL"

        !status.isSuccess() -> "Upstash error (${status.value})"
        else -> null
    }

    private fun JsonElement?.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content

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
        private const val BACKUP_KEY = "ivy_wallet_backup"
        private const val META_KEY = "ivy_wallet_meta"
    }
}
