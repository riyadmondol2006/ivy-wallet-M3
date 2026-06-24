package com.ivy.data.sync

/**
 * How the app keeps the user's data in sync with their Upstash Redis database.
 */
enum class SyncMode {
    /** Cloud sync is not used. */
    OFF,

    /** The user syncs manually by tapping the sync button. */
    MANUAL,

    /** The app pushes a backup automatically after every change (debounced). */
    AUTO,
}

/**
 * How the app talks to the Redis database.
 */
enum class SyncEndpointType {
    /** Upstash REST API over HTTPS (REST URL + REST token). */
    HTTPS,

    /** Native Redis protocol over a TLS TCP socket (host:port or rediss:// URL + password). */
    TCP,
}

/**
 * The Upstash Redis connection + sync preferences saved on this device.
 *
 * @param endpointUrl the Upstash REST URL (e.g. https://xxx.upstash.io), null until set up.
 * @param token the Upstash REST token, null until set up.
 * @param mode the [SyncMode].
 * @param deviceId a stable per-install id used to tell "this device" apart from others.
 * @param lastSyncedUpdatedAt the remote `updatedAt` value this device last pushed or pulled
 *  (0 = never). Used to decide whether the cloud has newer changes worth pulling.
 */
@Suppress("DataClassTypedIDs")
data class SyncConfig(
    val endpointUrl: String?,
    val token: String?,
    val endpointType: SyncEndpointType,
    val mode: SyncMode,
    val deviceId: String,
    val lastSyncedUpdatedAt: Long,
) {
    val isConfigured: Boolean
        get() = !endpointUrl.isNullOrBlank() && !token.isNullOrBlank()
}
