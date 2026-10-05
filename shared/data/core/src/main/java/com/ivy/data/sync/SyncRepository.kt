package com.ivy.data.sync

import android.content.Context
import android.content.pm.PackageManager
import arrow.core.Either
import arrow.core.flatMap
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
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates cloud backup/restore between the local database ([BackupDataUseCase]) and the user's
 * Upstash Redis ([RedisSyncDataSource]), using the saved [SyncConfigDataSource] connection.
 *
 * Every public call catches exceptions and reports them on the left side, so callers never have
 * to guard against a crash from a malformed backup or a broken connection.
 */
@Singleton
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
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext PushError.Failed(NOT_CONFIGURED).left()

        val redis = sourceFor(config.endpointType)
        val url = config.endpointUrl!!
        val token = config.token!!

        if (!force) {
            val remote = when (val meta = redis.getMeta(url, token)) {
                is Either.Left -> return@withContext PushError.Failed(meta.value).left()
                is Either.Right -> meta.value
            }
            if (remote != null && remote.isNewerForeignRevision(config)) {
                return@withContext PushError.RemoteNewer(remote).left()
            }
        }

        val updatedAt = System.currentTimeMillis()
        Either.catch { backupDataUseCase.generateJsonBackup() }
            .mapLeft<PushError> { e ->
                Timber.e(e, "Cloud sync: generating the backup failed")
                PushError.Failed(e.message ?: BACKUP_FAILED)
            }
            .flatMap { backupJson ->
                val meta = RemoteSyncMeta(
                    deviceId = config.deviceId,
                    updatedAt = updatedAt,
                    accounts = accountDao.findAll().size,
                    appVersion = appVersion(),
                )
                redis.putBackup(url, token, backupJson, meta).mapLeft { PushError.Failed(it) }
            }
            .onRight { configDataSource.setLastSyncedUpdatedAt(updatedAt) }
    }

    /** Downloads the cloud backup and imports it into the local database. */
    suspend fun pull(): Either<String, ImportResult> = withContext(dispatchers.io) {
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext NOT_CONFIGURED.left()

        val redis = sourceFor(config.endpointType)
        either {
            val url = config.endpointUrl!!
            val token = config.token!!
            val backupJson = redis.getBackup(url, token).bind()
                ?: raise(NO_BACKUP)
            val meta = redis.getMeta(url, token).bind()
            val result = Either.catch {
                autoSyncGate.suppressing(SUPPRESS_WINDOW_MS) {
                    backupDataUseCase.importJson(backupJson)
                }
            }.mapLeft { e ->
                Timber.e(e, "Cloud sync: importing the backup failed")
                "$IMPORT_FAILED ${e.message.orEmpty()}".trim()
            }.bind()
            configDataSource.setLastSyncedUpdatedAt(meta?.updatedAt ?: config.lastSyncedUpdatedAt)
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
                )
            },
        )
    }

    /** Deletes the backup and its meta record from the cloud. The local data is untouched. */
    suspend fun deleteRemote(): Either<String, Unit> = withContext(dispatchers.io) {
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext NOT_CONFIGURED.left()
        sourceFor(config.endpointType).deleteBackup(config.endpointUrl!!, config.token!!)
            .onRight { configDataSource.setLastSyncedUpdatedAt(0L) }
    }

    /**
     * Marks the current remote revision as "seen" without pulling, so a dismissed prompt won't
     * nag again for the same change.
     */
    suspend fun markRemoteSeen(updatedAt: Long) {
        configDataSource.setLastSyncedUpdatedAt(updatedAt)
    }

    private fun RemoteSyncMeta.isNewerForeignRevision(config: SyncConfig): Boolean =
        deviceId != config.deviceId && updatedAt != config.lastSyncedUpdatedAt

    /** Returns an error message when the endpoint is unusable, or null when it is fine. */
    private fun validateEndpoint(endpointType: SyncEndpointType, url: String): String? = when {
        url.isBlank() -> URL_REQUIRED
        endpointType == SyncEndpointType.HTTPS && !url.startsWith("https://", ignoreCase = true) ->
            HTTPS_REQUIRED

        endpointType == SyncEndpointType.TCP && url.startsWith("redis://", ignoreCase = true) ->
            TLS_REQUIRED

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
        private const val TLS_REQUIRED = "Use the rediss:// (TLS) endpoint, not redis://"

        /** Keep auto-sync suppressed a bit longer than the push debounce after an import. */
        private const val SUPPRESS_WINDOW_MS = 10_000L
    }
}
