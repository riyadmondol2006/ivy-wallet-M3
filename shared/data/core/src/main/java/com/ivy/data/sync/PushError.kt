package com.ivy.data.sync

import com.ivy.data.sync.model.RemoteSyncMeta

/**
 * Why a cloud push did not happen.
 */
sealed interface PushError {
    /** Human-readable explanation suitable for showing to the user. */
    val message: String

    /**
     * The cloud holds a revision written by another device that this device has not pulled yet.
     * Pushing would silently overwrite it, so the caller must either pull first or push with
     * `force = true` after the user confirms.
     */
    data class RemoteNewer(val remote: RemoteSyncMeta) : PushError {
        override val message: String =
            "The cloud has newer data from another device. Pull it first or choose to overwrite."
    }

    /** Anything else: not configured, network, serialization, server error. */
    data class Failed(override val message: String) : PushError
}
