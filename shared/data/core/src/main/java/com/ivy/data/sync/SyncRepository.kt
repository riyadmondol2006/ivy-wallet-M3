package com.ivy.data.sync

import android.content.Context
import android.content.pm.PackageManager
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.ImportResult
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.sync.impl.RedisSyncDataSourceImpl
import com.ivy.data.sync.impl.RedisTcpSyncDataSourceImpl
import com.ivy.data.sync.model.RemoteSyncMeta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates cloud backup/restore between the local database ([BackupDataUseCase]) and the user's
 * Redis database ([RedisSyncDataSource]), using the saved [SyncConfigDataSource] connection.
 *
 * The cloud holds one full backup. Pulling makes this device match it, unless this device has
 * changes the cloud doesn't have: then both are merged and the result is pushed back, so neither
 * side's changes are lost.
 *
 * Every public call catches exceptions and reports them on the left side, so callers never have
 * to guard against a crash from a malformed backup or a broken connection.
 */
@Singleton
@Suppress("LongParameterList")
class SyncRepository @Inject constructor(
    private val backupDataUseCase: BackupDataUseCase,
    private val restDataSource: RedisSyncDataSourceImpl,
    private val tcpDataSource: RedisTcpSyncDataSourceImpl,
    private val configDataSource: SyncConfigDataSource,
    private val autoSyncGate: AutoSyncGate,
    private val accountDao: AccountDao,
    private val dataObserver: DataObserver,
    private val dispatchers: DispatchersProvider,
    @ApplicationContext private val context: Context,
) {
    /**
     * Push and pull never overlap: a push that read the database before a pull replaced it would
     * otherwise upload the old data over what was just pulled.
     */
    private val syncMutex = Mutex()

    private fun sourceFor(endpointType: SyncEndpointType): RedisSyncDataSource =
        if (endpointType == SyncEndpointType.TCP) tcpDataSource else restDataSource

    /** Verifies a URL+token before it's saved (used by the "Test connection" button). */
    suspend fun testConnection(
        endpointType: SyncEndpointType,
        url: String,
        token: String,
    ): Either<String, Unit> {
        val cleanUrl = url.trim().trimEnd('/')
        validateEndpoint(endpointType, cleanUrl)?.let { return it.left() }
        return sourceFor(endpointType).ping(cleanUrl, token.trim())
    }

    /**
     * Uploads a full backup of the local data to Redis and records the new revision.
     *
     * Unless [force] is set, the push is refused with [PushError.RemoteNewer] when the cloud holds
     * a revision from another device that this device has not pulled yet, so one device can never
     * silently overwrite another device's changes.
     */
    suspend fun push(force: Boolean = false): Either<PushError, Unit> = withContext(dispatchers.io) {
        syncMutex.withLock { pushLocked(force) }
    }

    private suspend fun pushLocked(force: Boolean): Either<PushError, Unit> {
        val config = configDataSource.get()
        if (!config.isConfigured) return PushError.Failed(NOT_CONFIGURED).left()

        val redis = sourceFor(config.endpointType)
        val url = config.endpointUrl!!
        val token = config.token!!

        if (!force) {
            val remote = when (val meta = redis.getMeta(url, token)) {
                is Either.Left -> return PushError.Failed(meta.value).left()
                is Either.Right -> meta.value
            }
            if (remote != null && remote.isNewerForeignRevision(config)) {
                return PushError.RemoteNewer(remote).left()
            }
        }

        // Read before the snapshot is taken: a change marked after this is not in the upload.
        val snapshotAt = System.currentTimeMillis()
        val backupJson = try {
            backupDataUseCase.generateJsonBackup()
        } catch (e: Exception) {
            Timber.e(e, "Cloud sync: generating the backup failed")
            return PushError.Failed(e.message ?: BACKUP_FAILED).left()
        }
        val meta = RemoteSyncMeta(
            deviceId = config.deviceId,
            updatedAt = snapshotAt,
            accounts = accountDao.findAll().size,
            appVersion = appVersion(),
        )
        return redis.putBackup(url, token, CloudBackupCodec.encode(backupJson), meta)
            .mapLeft<PushError> { PushError.Failed(it) }
            .onRight {
                configDataSource.setLastSyncedUpdatedAt(snapshotAt)
                configDataSource.clearLocalChange(since = snapshotAt)
            }
    }

    /**
     * Brings the cloud backup into this device.
     *
     * Without local changes, this device becomes an exact copy of the cloud, so items deleted on
     * another device are deleted here too. With local changes (or a device that already had data
     * before it first synced), both sides are merged and the result is pushed back, so the cloud
     * and this device end up with everything from both.
     */
    suspend fun pull(): Either<String, ImportResult> = withContext(dispatchers.io) {
        syncMutex.withLock { pullLocked() }
    }

    private suspend fun pullLocked(): Either<String, ImportResult> {
        val config = configDataSource.get()
        if (!config.isConfigured) return NOT_CONFIGURED.left()

        val redis = sourceFor(config.endpointType)
        val url = config.endpointUrl!!
        val token = config.token!!
        val merge = hasLocalChanges(config)

        return either {
            val snapshot = redis.getSnapshot(url, token).bind()
            val storedBackup = snapshot.storedBackup ?: raise(NO_BACKUP)
            val result = Either.catch {
                val backupJson = CloudBackupCodec.decode(storedBackup)
                autoSyncGate.suppressing(SUPPRESS_WINDOW_MS) {
                    if (merge) {
                        backupDataUseCase.importJson(backupJson)
                    } else {
                        backupDataUseCase.replaceAllWithJson(backupJson)
                    }
                }
            }.mapLeft { e ->
                Timber.e(e, "Cloud sync: importing the backup failed")
                "$IMPORT_FAILED ${e.message.orEmpty()}".trim()
            }.bind()

            // The cloud revision is now part of this device's data.
            snapshot.meta?.updatedAt?.let { configDataSource.setLastSyncedUpdatedAt(it) }
            if (merge) {
                // Upload the merged data so the other device gets this device's changes too. If it
                // fails (e.g. offline), the local changes stay marked and are pushed later.
                pushLocked(force = false).onLeft {
                    Timber.w("Cloud sync: pushing the merged data failed: ${it.message}")
                }
            } else {
                configDataSource.clearLocalChange(since = System.currentTimeMillis())
            }
            // Other screens cache accounts/categories; tell them everything may have changed.
            dataObserver.post(DataWriteEvent.AllDataChange)
            result
        }
    }

    /** Reads only the meta record to decide whether the cloud has newer changes worth pulling. */
    suspend fun checkRemote(): RemoteStatus = withContext(dispatchers.io) {
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext RemoteStatus.empty()

        sourceFor(config.endpointType).getMeta(config.endpointUrl!!, config.token!!).fold(
            ifLeft = { RemoteStatus.empty() },
            ifRight = { meta ->
                RemoteStatus(
                    exists = meta != null,
                    meta = meta,
                    isFromOtherDevice = meta != null && meta.deviceId != config.deviceId,
                    isNewer = meta != null && meta.updatedAt != config.lastSyncedUpdatedAt,
                    hasLocalChanges = hasLocalChanges(config),
                )
            },
        )
    }

    /** Deletes the backup and its meta record from the cloud. The local data is untouched. */
    suspend fun deleteRemote(): Either<String, Unit> = withContext(dispatchers.io) {
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext NOT_CONFIGURED.left()
        syncMutex.withLock {
            sourceFor(config.endpointType).deleteBackup(config.endpointUrl!!, config.token!!)
                .onRight { configDataSource.setLastSyncedUpdatedAt(0L) }
        }
    }

    /** Records that local data changed and is not in the cloud yet. */
    suspend fun markLocalChange() {
        if (configDataSource.get().isConfigured) {
            configDataSource.markLocalChange(System.currentTimeMillis())
        }
    }

    /**
     * True when replacing this device's data with the cloud's would lose something: a change not
     * pushed yet, or data this device had before it first synced with this database.
     */
    private suspend fun hasLocalChanges(config: SyncConfig): Boolean =
        config.localChangedAt > 0L ||
            (config.lastSyncedUpdatedAt == 0L && accountDao.findAll().isNotEmpty())

    private fun RemoteSyncMeta.isNewerForeignRevision(config: SyncConfig): Boolean =
        deviceId != config.deviceId && updatedAt != config.lastSyncedUpdatedAt

    /** Returns an error message when the endpoint is unusable, or null when it is fine. */
    private fun validateEndpoint(endpointType: SyncEndpointType, url: String): String? = when {
        url.isBlank() -> URL_REQUIRED
        endpointType == SyncEndpointType.HTTPS && !url.startsWith("https://", ignoreCase = true) ->
            HTTPS_REQUIRED

        endpointType == SyncEndpointType.TCP && RedisConnectionString.parse(url) == null ->
            REDIS_URL_REQUIRED

        else -> null
    }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }

    companion object {
        private const val NOT_CONFIGURED = "Cloud sync is not set up"
        private const val NO_BACKUP = "No cloud backup found yet"
        private const val BACKUP_FAILED = "Could not prepare the backup"
        private const val IMPORT_FAILED = "The cloud backup could not be imported."
        private const val URL_REQUIRED = "Enter the endpoint URL"
        private const val HTTPS_REQUIRED =
            "Use the https:// REST URL from the Upstash console (plain http is not allowed)"
        private const val REDIS_URL_REQUIRED =
            "Enter a Redis endpoint such as rediss://host:6379, or paste the redis-cli command"

        /**
         * Keeps change tracking quiet while a pull writes the database and briefly after, until
         * Room has delivered the resulting table invalidations.
         */
        private const val SUPPRESS_WINDOW_MS = 3_000L
    }
}
