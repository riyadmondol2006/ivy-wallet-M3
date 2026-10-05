package com.ivy.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.ivy.ui.R

/**
 * Outcome of a cloud-sync action, resolved to a localized string by the UI with [text].
 * ViewModels produce these instead of hard-coded English so every screen reports sync results
 * the same way.
 */
@Immutable
sealed interface SyncMessage {
    data object ConnectionSaved : SyncMessage
    data object ConnectionRemoved : SyncMessage
    data object BackedUp : SyncMessage
    data object CloudDataDeleted : SyncMessage
    data class Restored(val transactions: Int) : SyncMessage

    /** The cloud holds newer data from another device; nothing was uploaded. */
    data object RemoteNewer : SyncMessage

    /** Anything else went wrong; [reason] comes from the data layer. */
    data class Failed(val reason: String) : SyncMessage
}

@Composable
fun SyncMessage.text(): String = when (this) {
    SyncMessage.ConnectionSaved -> stringResource(R.string.cloud_sync_connection_saved)
    SyncMessage.ConnectionRemoved -> stringResource(R.string.cloud_sync_connection_removed)
    SyncMessage.BackedUp -> stringResource(R.string.cloud_sync_backed_up)
    SyncMessage.CloudDataDeleted -> stringResource(R.string.cloud_sync_cloud_data_deleted)
    is SyncMessage.Restored -> stringResource(R.string.cloud_sync_restored, transactions)
    SyncMessage.RemoteNewer -> stringResource(R.string.cloud_sync_remote_newer)
    is SyncMessage.Failed -> reason
}
