package com.ivy.settings.cloudsync

import com.ivy.data.sync.SyncEndpointType
import com.ivy.data.sync.SyncMode

sealed interface CloudSyncEvent {
    data class UrlChanged(val url: String) : CloudSyncEvent
    data class TokenChanged(val token: String) : CloudSyncEvent
    data class SetEndpointType(val type: SyncEndpointType) : CloudSyncEvent

    /** Set up, test and save the database from one pasted connection line. */
    data class QuickAdd(val connection: String) : CloudSyncEvent
    data object TestConnection : CloudSyncEvent

    /** Persist the tested connection. */
    data object Save : CloudSyncEvent
    data class SetMode(val mode: SyncMode) : CloudSyncEvent

    /** Push the local data to the cloud now (refused if another device wrote newer data). */
    data object SyncNow : CloudSyncEvent

    /** Push even though the cloud holds newer data from another device. */
    data object ForceSyncNow : CloudSyncEvent

    /** Pull the cloud backup and import it now. */
    data object RestoreNow : CloudSyncEvent

    /** The newly added database already holds a backup: bring it into this device. */
    data object RestoreExisting : CloudSyncEvent

    /** The newly added database already holds a backup: replace it with this device's data. */
    data object OverwriteExisting : CloudSyncEvent
    data object DismissExisting : CloudSyncEvent

    data object RemoveConnection : CloudSyncEvent
    data object DismissMessage : CloudSyncEvent

    /** Onboarding: restore the existing cloud backup, then finish onboarding. */
    data object OnboardingRestore : CloudSyncEvent

    /** Onboarding: ignore the cloud backup and continue with a fresh setup. */
    data object OnboardingStartFresh : CloudSyncEvent

    /** Consume the one-shot [CompletionSignal] after the composable handled it. */
    data object ConsumeCompletion : CloudSyncEvent
}
