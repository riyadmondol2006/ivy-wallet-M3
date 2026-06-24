package com.ivy.wallet.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.ivy.data.sync.SyncConfigDataSource
import com.ivy.data.sync.SyncMode
import com.ivy.data.sync.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Retries a cloud-sync push with exponential backoff when an immediate auto-sync fails (e.g. the
 * device was offline). Only runs while sync is configured and in AUTO mode.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val configDataSource: SyncConfigDataSource,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = configDataSource.get()
        if (config.mode != SyncMode.AUTO || !config.isConfigured) {
            return Result.success()
        }
        return syncRepository.push().fold(
            ifLeft = {
                Timber.w("Cloud sync retry failed: $it")
                Result.retry()
            },
            ifRight = { Result.success() },
        )
    }

    companion object {
        private const val WORK_NAME = "ivy_cloud_sync_retry"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
