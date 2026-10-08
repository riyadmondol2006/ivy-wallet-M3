package com.ivy.data.sync.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Small record stored next to the backup in Redis (key `ivy_wallet_meta`) describing who wrote the
 * current cloud backup and when. Fetched on its own to decide whether to pull without downloading
 * the whole backup.
 *
 * Every field is required so it is always written: app versions since 1.0.1 decode it strictly.
 *
 * @param deviceId the [com.ivy.data.sync.SyncConfig.deviceId] of the device that last pushed.
 * @param updatedAt epoch millis of the last push (also used as a revision marker).
 * @param accounts number of accounts in the backup (shown to the user before restoring).
 * @param appVersion the app version that produced the backup (informational).
 */
@Serializable
data class RemoteSyncMeta(
    val deviceId: String,
    val updatedAt: Long,
    val accounts: Int,
    val appVersion: String,
) {
    companion object {
        /**
         * Revision of a backup whose meta record is missing or has no `updatedAt`. It is never 0,
         * which means "never synced", so such a backup still counts as another device's data
         * and is protected from being overwritten without asking.
         */
        const val UNKNOWN_UPDATED_AT = 1L

        /** Describes a backup that exists without a readable meta record. */
        fun unknown(): RemoteSyncMeta = RemoteSyncMeta(
            deviceId = "",
            updatedAt = UNKNOWN_UPDATED_AT,
            accounts = 0,
            appVersion = "",
        )

        /**
         * Reads a stored meta record without failing on missing or mistyped fields, so a damaged
         * record never blocks restoring the backup next to it. Returns null when [raw] is not a
         * JSON object.
         */
        fun parseLenient(json: Json, raw: String): RemoteSyncMeta? {
            val obj = try {
                json.parseToJsonElement(raw) as? JsonObject
            } catch (_: SerializationException) {
                null
            } ?: return null

            fun field(name: String) = obj[name] as? JsonPrimitive
            return RemoteSyncMeta(
                deviceId = field("deviceId")?.contentOrNull.orEmpty(),
                updatedAt = field("updatedAt")?.longOrNull?.takeIf { it > 0L }
                    ?: UNKNOWN_UPDATED_AT,
                accounts = field("accounts")?.intOrNull ?: 0,
                appVersion = field("appVersion")?.contentOrNull.orEmpty(),
            )
        }
    }
}
