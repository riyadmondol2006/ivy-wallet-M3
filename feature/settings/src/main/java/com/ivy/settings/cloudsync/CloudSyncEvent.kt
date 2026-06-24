package com.ivy.settings.cloudsync

import com.ivy.data.sync.SyncEndpointType
import com.ivy.data.sync.SyncMode

sealed interface CloudSyncEvent {
    data class UrlChanged(val url: String) : CloudSyncEvent
    data class TokenChanged(val token: String) : CloudSyncEvent
    data class SetEndpointType(val type: SyncEndpointType) : CloudSyncEvent
    data object TestConnection : CloudSyncEvent

    /** Persist the tested connection. */
    data object Save : CloudSyncEvent
    data class SetMode(val mode: SyncMode) : CloudSyncEvent

    /** Push the local data to the cloud now. */
    data object SyncNow : CloudSyncEvent

    /** Pull the cloud backup and import it now. */
    data object RestoreNow : CloudSyncEvent
    data object RemoveConnection : CloudSyncEvent
    data object DismissMessage : CloudSyncEvent

    /** Onboarding: restore the existing cloud backup, then finish onboarding. */
    data object OnboardingRestore : CloudSyncEvent

    /** Onboarding: ignore the cloud backup and continue with a fresh setup. */
    data object OnboardingStartFresh : CloudSyncEvent

    /** Consume the one-shot [CompletionSignal] after the composable handled it. */
    data object ConsumeCompletion : CloudSyncEvent
}
