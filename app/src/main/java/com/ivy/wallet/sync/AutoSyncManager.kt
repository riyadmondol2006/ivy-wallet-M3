package com.ivy.wallet.sync

import android.content.Context
import com.ivy.base.di.AppCoroutineScope
import com.ivy.data.DataObserver
import com.ivy.data.sync.AutoSyncGate
import com.ivy.data.sync.SyncConfigDataSource
import com.ivy.data.sync.SyncMode
import com.ivy.data.sync.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches local data changes and, when AUTO sync is enabled, pushes a fresh backup to the cloud
 * (debounced so a burst of edits results in a single upload). On failure it hands off to
 * [SyncWorker] for a network-aware retry. Started once from the Application.
 */
@Singleton
class AutoSyncManager @Inject constructor(
    private val dataObserver: DataObserver,
    private val syncRepository: SyncRepository,
    private val configDataSource: SyncConfigDataSource,
    private val autoSyncGate: AutoSyncGate,
    @AppCoroutineScope private val scope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            dataObserver.writeEvents
                .debounce(DEBOUNCE_MS)
                .collect { onDataChanged() }
        }
    }

    private suspend fun onDataChanged() {
        // A pull/restore just wrote the DB; don't bounce the imported data straight back.
        if (autoSyncGate.isSuppressed()) return

        val config = configDataSource.get()
        if (config.mode != SyncMode.AUTO || !config.isConfigured) return

        syncRepository.push().onLeft {
            Timber.w("Auto-sync push failed: $it. Scheduling background retry.")
            SyncWorker.enqueue(context)
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 5_000L
    }
}
