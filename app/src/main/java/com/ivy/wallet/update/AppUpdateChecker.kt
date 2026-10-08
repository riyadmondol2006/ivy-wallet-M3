package com.ivy.wallet.update

import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ivy.wallet.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/** A GitHub release newer than the installed build. */
data class AppUpdate(
    val version: String,
    val currentVersion: String,
    /** The release's APK when it has one, otherwise the release page. */
    val downloadUrl: String,
)

/**
 * Looks up the latest GitHub release of this fork and decides whether to offer it. Pre-releases
 * and drafts are never offered (the `releases/latest` endpoint excludes them), and a version the
 * user chose to skip stays hidden until a newer one is published.
 */
class AppUpdateChecker @Inject constructor(
    private val httpClient: HttpClient,
    private val dataStore: DataStore<Preferences>,
) {

    @Keep
    @Serializable
    data class ReleaseDto(
        @SerialName("tag_name")
        val tagName: String,
        @SerialName("html_url")
        val htmlUrl: String,
        @SerialName("draft")
        val draft: Boolean = false,
        @SerialName("prerelease")
        val prerelease: Boolean = false,
        @SerialName("assets")
        val assets: List<AssetDto> = emptyList(),
    )

    @Keep
    @Serializable
    data class AssetDto(
        @SerialName("name")
        val name: String,
        @SerialName("browser_download_url")
        val downloadUrl: String,
    )

    /** Returns the update to offer, or null when up to date, skipped, offline or on any error. */
    suspend fun findUpdate(currentVersionName: String = BuildConfig.VERSION_NAME): AppUpdate? {
        val release = fetchLatestRelease() ?: return null
        return pickUpdate(release, currentVersionName, skippedVersion())
    }

    // Runs on the app scope, which has no exception handler: a storage error must not crash.
    suspend fun skipVersion(version: String) {
        try {
            dataStore.edit { it[SkippedVersionKey] = version }
        } catch (e: IOException) {
            Timber.w(e, "Couldn't save the skipped update")
        }
    }

    private suspend fun skippedVersion(): String? = try {
        dataStore.data.first()[SkippedVersionKey]
    } catch (e: IOException) {
        Timber.w(e, "Couldn't read the skipped update")
        null
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchLatestRelease(): ReleaseDto? = try {
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                val response = httpClient.get(LATEST_RELEASE_URL) {
                    header(HttpHeaders.Accept, "application/vnd.github+json")
                    header(HttpHeaders.UserAgent, "IvyWalletM3/${BuildConfig.VERSION_NAME}")
                }
                // 404 = no releases yet, 403 = rate limited. Either way there is nothing to offer.
                if (response.status.isSuccess()) response.body<ReleaseDto>() else null
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Update check failed")
        null
    }

    companion object {
        /** Decides whether [release] should be offered over [currentVersionName]. */
        internal fun pickUpdate(
            release: ReleaseDto,
            currentVersionName: String,
            skippedVersion: String?,
        ): AppUpdate? {
            if (release.draft || release.prerelease) return null
            val current = AppVersion.parse(currentVersionName) ?: return null
            val latest = AppVersion.parse(release.tagName) ?: return null
            if (latest <= current) return null
            if (AppVersion.parse(skippedVersion) == latest) return null

            val apk = release.assets.firstOrNull { it.name == RELEASE_APK_NAME }
                ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apk?.downloadUrl?.takeIf { it.isHttps() }
                ?: release.htmlUrl.takeIf { it.isHttps() }
                ?: return null

            return AppUpdate(
                version = latest.toString(),
                currentVersion = current.toString(),
                downloadUrl = downloadUrl,
            )
        }

        private fun String.isHttps() = startsWith("https://")

        private const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/riyadmondol2006/ivy-wallet-M3/releases/latest"
        private const val RELEASE_APK_NAME = "app-release.apk"
        private const val REQUEST_TIMEOUT_MS = 10_000L

        private val SkippedVersionKey = stringPreferencesKey("app_update_skipped_version")
    }
}
