package com.ivy.data.sync

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared flag that lets a pull/restore temporarily silence auto-sync, so the `AllDataChange` event
 * emitted by importing a backup doesn't immediately bounce back as a push of the just-pulled data.
 *
 * Lives in the data layer so both [SyncRepository] (which imports) and the app-level auto-sync
 * observer (which pushes) can see it.
 */
@Singleton
class AutoSyncGate @Inject constructor() {
    @Volatile
    private var suppressUntil = 0L

    fun suppressFor(millis: Long) {
        suppressUntil = System.currentTimeMillis() + millis
    }

    fun isSuppressed(): Boolean = System.currentTimeMillis() < suppressUntil

    /**
     * Runs [block] with auto-sync suppressed, keeping the window open for [windowMillis] after it
     * finishes to also cover the debounced push that the resulting data-change event would trigger.
     */
    suspend fun <T> suppressing(windowMillis: Long, block: suspend () -> T): T {
        suppressFor(windowMillis)
        try {
            return block()
        } finally {
            suppressFor(windowMillis)
        }
    }
}
