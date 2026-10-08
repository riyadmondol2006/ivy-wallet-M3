package com.ivy.data.sync

import arrow.core.Either
import com.ivy.data.sync.model.RemoteSyncMeta

/**
 * Thin client over the user's Redis database. Every call takes the connection details explicitly
 * so the UI can verify a URL+token before it's saved. All calls return [Either] with a
 * human-readable error message on the left.
 */
interface RedisSyncDataSource {
    /** Verifies the connection (`PING`). Left on bad URL/token or no network. */
    suspend fun ping(url: String, token: String): Either<String, Unit>

    /** Reads the tiny meta record, or null if no backup exists yet. */
    suspend fun getMeta(url: String, token: String): Either<String, RemoteSyncMeta?>

    /**
     * Reads the stored backup and its meta record in one `MGET`, so they always belong together
     * even if another device is pushing at the same time.
     */
    suspend fun getSnapshot(url: String, token: String): Either<String, RemoteSnapshot>

    /** Writes the backup and its meta record. */
    suspend fun putBackup(
        url: String,
        token: String,
        storedBackup: String,
        meta: RemoteSyncMeta,
    ): Either<String, Unit>

    /** Removes the backup and its meta record. */
    suspend fun deleteBackup(url: String, token: String): Either<String, Unit>

    companion object {
        // The key names are shared with every app version since 1.0.1; never change them.
        const val BACKUP_KEY = "ivy_wallet_backup"
        const val META_KEY = "ivy_wallet_meta"
    }
}

/**
 * The backup as stored in Redis (see [CloudBackupCodec]) and its meta record.
 * The meta can be missing next to an existing backup: versions before 1.0.4 wrote the two keys
 * separately, so an interrupted upload could leave only the backup.
 */
data class RemoteSnapshot(
    val storedBackup: String?,
    val meta: RemoteSyncMeta?,
)
