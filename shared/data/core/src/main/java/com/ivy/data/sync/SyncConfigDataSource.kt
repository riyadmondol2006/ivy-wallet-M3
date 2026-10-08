package com.ivy.data.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * Persists the Upstash Redis sync config in the app's [DataStore]. Plain (unencrypted) storage,
 * consistent with the rest of the app's preferences. The token only grants access to the user's
 * own Redis database.
 */
class SyncConfigDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val config: Flow<SyncConfig> = dataStore.data.map { prefs ->
        prefs.toSyncConfig()
    }

    suspend fun get(): SyncConfig = ensureDeviceId().let { config.first() }

    suspend fun setConnection(
        endpointUrl: String,
        token: String,
        endpointType: SyncEndpointType,
    ) {
        dataStore.edit { prefs ->
            prefs[EndpointUrlKey] = endpointUrl.trim().trimEnd('/')
            prefs[TokenKey] = token.trim()
            prefs[EndpointTypeKey] = endpointType.name
        }
    }

    suspend fun setMode(mode: SyncMode) {
        dataStore.edit { prefs ->
            prefs[ModeKey] = mode.name
        }
    }

    suspend fun setLastSyncedUpdatedAt(updatedAt: Long) {
        dataStore.edit { prefs ->
            prefs[LastSyncedUpdatedAtKey] = updatedAt
        }
    }

    /** Records that local data changed; keeps the time of the first unsynced change. */
    suspend fun markLocalChange(at: Long) {
        dataStore.edit { prefs ->
            if ((prefs[LocalChangedAtKey] ?: 0L) == 0L) prefs[LocalChangedAtKey] = at
        }
    }

    /**
     * Forgets local changes once they are in the cloud. [since] is when the synced snapshot was
     * read from the database: a change marked after it is newer than the cloud and is kept.
     */
    suspend fun clearLocalChange(since: Long) {
        dataStore.edit { prefs ->
            if ((prefs[LocalChangedAtKey] ?: 0L) <= since) prefs.remove(LocalChangedAtKey)
        }
    }

    suspend fun clearConnection() {
        dataStore.edit { prefs ->
            prefs.remove(EndpointUrlKey)
            prefs.remove(TokenKey)
            prefs.remove(EndpointTypeKey)
            prefs[ModeKey] = SyncMode.OFF.name
            prefs.remove(LastSyncedUpdatedAtKey)
            prefs.remove(LocalChangedAtKey)
        }
    }

    /**
     * Generates and persists a stable device id on first use so it survives later reads.
     */
    private suspend fun ensureDeviceId() {
        val existing = dataStore.data.map { it[DeviceIdKey] }.first()
        if (existing == null) {
            dataStore.edit { prefs ->
                if (prefs[DeviceIdKey] == null) {
                    prefs[DeviceIdKey] = UUID.randomUUID().toString()
                }
            }
        }
    }

    private fun Preferences.toSyncConfig(): SyncConfig = SyncConfig(
        endpointUrl = this[EndpointUrlKey],
        token = this[TokenKey],
        endpointType = this[EndpointTypeKey]?.let(::endpointTypeOrHttps) ?: SyncEndpointType.HTTPS,
        mode = this[ModeKey]?.let(::modeOrOff) ?: SyncMode.OFF,
        deviceId = this[DeviceIdKey] ?: "",
        lastSyncedUpdatedAt = this[LastSyncedUpdatedAtKey] ?: 0L,
        localChangedAt = this[LocalChangedAtKey] ?: 0L,
    )

    private fun modeOrOff(name: String): SyncMode =
        SyncMode.entries.firstOrNull { it.name == name } ?: SyncMode.OFF

    private fun endpointTypeOrHttps(name: String): SyncEndpointType =
        SyncEndpointType.entries.firstOrNull { it.name == name } ?: SyncEndpointType.HTTPS

    companion object {
        private val EndpointUrlKey = stringPreferencesKey("sync_upstash_url")
        private val TokenKey = stringPreferencesKey("sync_upstash_token")
        private val EndpointTypeKey = stringPreferencesKey("sync_endpoint_type")
        private val ModeKey = stringPreferencesKey("sync_mode")
        private val DeviceIdKey = stringPreferencesKey("sync_device_id")
        private val LastSyncedUpdatedAtKey = longPreferencesKey("sync_last_updated_at")
        private val LocalChangedAtKey = longPreferencesKey("sync_local_changed_at")
    }
}
