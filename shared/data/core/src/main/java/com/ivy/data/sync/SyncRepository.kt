package com.ivy.data.sync

import android.content.Context
import android.content.pm.PackageManager
import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.ImportResult
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.sync.impl.RedisSyncDataSourceImpl
import com.ivy.data.sync.impl.RedisTcpSyncDataSourceImpl
import com.ivy.data.sync.model.RemoteSyncMeta
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates cloud backup/restore between the local database ([BackupDataUseCase]) and the user's
 * Upstash Redis ([RedisSyncDataSource]), using the saved [SyncConfigDataSource] connection.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val backupDataUseCase: BackupDataUseCase,
    private val restDataSource: RedisSyncDataSourceImpl,
    private val tcpDataSource: RedisTcpSyncDataSourceImpl,
    private val configDataSource: SyncConfigDataSource,
    private val autoSyncGate: AutoSyncGate,
    private val accountDao: AccountDao,
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
    ): Either<String, Unit> =
        sourceFor(endpointType).ping(url.trim().trimEnd('/'), token.trim())

    /** Uploads a full backup of the local data to Redis and records the new revision. */
    suspend fun push(): Either<String, Unit> = withContext(dispatchers.io) {
        val config = configDataSource.get()
        if (!config.isConfigured) return@withContext NOT_CONFIGURED.left()

        val redis = sourceFor(config.endpointType)
        val backupJson = backupDataUseCase.generateJsonBackup()
        val updatedAt = System.currentTimeMillis()
        val meta = RemoteSyncMeta(
            deviceId = config.deviceId,
            updatedAt = updatedAt,
            accounts = accountDao.findAll().size,
            appVersion = appVersion(),
        )
        redis.putBackup(config.endpointUrl!!, config.token!!, backupJson, meta)
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
                ?: raise("No cloud backup found yet")
            val meta = redis.getMeta(url, token).bind()
            val result = autoSyncGate.suppressing(SUPPRESS_WINDOW_MS) {
                backupDataUseCase.importJson(backupJson)
            }
            configDataSource.setLastSyncedUpdatedAt(meta?.updatedAt ?: config.lastSyncedUpdatedAt)
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

    /**
     * Marks the current remote revision as "seen" without pulling, so a dismissed prompt won't
     * nag again for the same change.
     */
    suspend fun markRemoteSeen(updatedAt: Long) {
        configDataSource.setLastSyncedUpdatedAt(updatedAt)
    }

    private fun appVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
        ""
    }

    companion object {
        private const val NOT_CONFIGURED = "Cloud sync is not set up"

        /** Keep auto-sync suppressed a bit longer than the push debounce after an import. */
        private const val SUPPRESS_WINDOW_MS = 10_000L
    }
}
