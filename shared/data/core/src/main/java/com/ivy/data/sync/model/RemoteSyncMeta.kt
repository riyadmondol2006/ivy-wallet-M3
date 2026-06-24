package com.ivy.data.sync.model

import kotlinx.serialization.Serializable

/**
 * Small record stored next to the backup in Redis (key `ivy:meta`) describing who wrote the
 * current cloud backup and when. Fetched on its own to decide whether to pull without downloading
 * the whole backup.
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
)
