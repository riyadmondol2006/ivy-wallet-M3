package com.ivy.settings.cloudsync

import com.ivy.data.sync.SyncEndpointType
import com.ivy.data.sync.SyncMode

/**
 * UI state for the Cloud Sync setup screen.
 */
data class CloudSyncState(
    val url: String,
    val token: String,
    val endpointType: SyncEndpointType,
    val mode: SyncMode,
    /** A connection has been tested and saved at least once. */
    val savedConfigured: Boolean,
    val testStatus: TestStatus,
    /** The current URL+token passed a test and differs from what's saved → Save is allowed. */
    val canSave: Boolean,
    /** A network operation (test/push/pull) is in progress. */
    val busy: Boolean,
    val remoteSummary: RemoteSummary?,
    /** Transient message shown in a snackbar. */
    val message: String?,
    val launchedFromOnboarding: Boolean,
    val onboardingRestore: OnboardingRestoreUi,
    val completion: CompletionSignal,
)

sealed interface TestStatus {
    data object Idle : TestStatus
    data object Testing : TestStatus
    data object Success : TestStatus
    data class Error(val message: String) : TestStatus
}

/**
 * Info about the backup currently in the cloud, shown under the connection.
 */
data class RemoteSummary(
    val updatedAtMillis: Long,
    val accounts: Int,
    val fromThisDevice: Boolean,
)

/**
 * Onboarding-only flow shown after a connection is saved: restore the existing cloud backup or
 * start fresh.
 */
sealed interface OnboardingRestoreUi {
    data object Hidden : OnboardingRestoreUi
    data object Checking : OnboardingRestoreUi
    data class BackupFound(val accounts: Int, val updatedAtMillis: Long) : OnboardingRestoreUi
    data object NoBackup : OnboardingRestoreUi
}

/**
 * One-shot signal telling the composable to hand control back to the onboarding flow.
 */
enum class CompletionSignal {
    NONE,

    /** A cloud restore succeeded during onboarding → finish onboarding and open the app. */
    FINISH_ONBOARDING,

    /** The user chose to start fresh → continue the normal onboarding steps. */
    RETURN_TO_ONBOARDING,
}
