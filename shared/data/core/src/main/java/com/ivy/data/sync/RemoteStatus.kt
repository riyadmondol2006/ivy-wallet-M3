package com.ivy.data.sync

import com.ivy.data.sync.model.RemoteSyncMeta

/**
 * Snapshot of the cloud backup's state relative to this device, used to drive the onboarding
 * "restore or start fresh" choice and the app-open "pull newer changes?" prompt.
 *
 * @param exists whether a backup is present in Redis.
 * @param meta the remote meta record (null when [exists] is false).
 * @param isFromOtherDevice the backup was last written by a different device.
 * @param isNewer the remote revision differs from the one this device last synced.
 */
data class RemoteStatus(
    val exists: Boolean,
    val meta: RemoteSyncMeta?,
    val isFromOtherDevice: Boolean,
    val isNewer: Boolean,
) {
    /** True when the app should prompt the user to pull changes made elsewhere. */
    val shouldPromptPull: Boolean
        get() = exists && isFromOtherDevice && isNewer

    companion object {
        fun empty(): RemoteStatus = RemoteStatus(
            exists = false,
            meta = null,
            isFromOtherDevice = false,
            isNewer = false,
        )
    }
}
