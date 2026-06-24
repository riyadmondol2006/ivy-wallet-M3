package com.ivy.data.sync

import arrow.core.Either
import com.ivy.data.sync.model.RemoteSyncMeta

/**
 * Thin client over the Upstash Redis REST API. Every call takes the connection details explicitly
 * so the UI can verify a URL+token before it's saved. All calls return [Either] with a
 * human-readable error message on the left.
 */
interface RedisSyncDataSource {
    /** Verifies the connection (`PING`). Left on bad URL/token or no network. */
    suspend fun ping(url: String, token: String): Either<String, Unit>

    /** Reads the tiny meta record, or null if no backup exists yet. */
    suspend fun getMeta(url: String, token: String): Either<String, RemoteSyncMeta?>

    /** Reads the full backup JSON, or null if none exists. */
    suspend fun getBackup(url: String, token: String): Either<String, String?>

    /** Writes the backup JSON and its meta record. */
    suspend fun putBackup(
        url: String,
        token: String,
        backupJson: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit>

    /** Removes the backup and its meta record. */
    suspend fun deleteBackup(url: String, token: String): Either<String, Unit>
}
