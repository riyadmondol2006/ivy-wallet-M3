package com.ivy.wallet.update

import com.ivy.wallet.update.AppUpdateChecker.AssetDto
import com.ivy.wallet.update.AppUpdateChecker.Companion.pickUpdate
import com.ivy.wallet.update.AppUpdateChecker.ReleaseDto
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class AppUpdateCheckerTest {

    private val apkUrl =
        "https://github.com/riyadmondol2006/ivy-wallet-M3/releases/download/v1.0.9/app-release.apk"
    private val pageUrl = "https://github.com/riyadmondol2006/ivy-wallet-M3/releases/tag/v1.0.9"

    private fun release(
        tag: String = "v1.0.9",
        draft: Boolean = false,
        prerelease: Boolean = false,
        htmlUrl: String = pageUrl,
        assets: List<AssetDto> = listOf(AssetDto("app-release.apk", apkUrl)),
    ) = ReleaseDto(
        tagName = tag,
        htmlUrl = htmlUrl,
        draft = draft,
        prerelease = prerelease,
        assets = assets,
    )

    @Test
    fun `offers a newer release with its apk`() {
        pickUpdate(release(), "1.0.8", skippedVersion = null) shouldBe AppUpdate(
            version = "1.0.9",
            currentVersion = "1.0.8",
            downloadUrl = apkUrl,
        )
    }

    @Test
    fun `does not offer the same or an older release`() {
        pickUpdate(release(tag = "v1.0.8"), "1.0.8", null).shouldBeNull()
        pickUpdate(release(tag = "v1.0.7"), "1.0.8", null).shouldBeNull()
    }

    @Test
    fun `compares double-digit parts numerically`() {
        pickUpdate(release(tag = "v1.0.10"), "1.0.9", null)?.version shouldBe "1.0.10"
        pickUpdate(release(tag = "v1.0.9"), "1.0.10", null).shouldBeNull()
    }

    @Test
    fun `hides the skipped version but offers a newer one`() {
        pickUpdate(release(tag = "v1.0.9"), "1.0.8", skippedVersion = "1.0.9").shouldBeNull()
        pickUpdate(release(tag = "v1.0.10"), "1.0.8", skippedVersion = "1.0.9")
            ?.version shouldBe "1.0.10"
    }

    @Test
    fun `never offers drafts or pre-releases`() {
        pickUpdate(release(draft = true), "1.0.8", null).shouldBeNull()
        pickUpdate(release(prerelease = true), "1.0.8", null).shouldBeNull()
    }

    @Test
    fun `ignores unparseable versions`() {
        pickUpdate(release(tag = "nightly"), "1.0.8", null).shouldBeNull()
        pickUpdate(release(), "unknown", null).shouldBeNull()
    }

    @Test
    fun `prefers app-release apk, then any apk, then the release page`() {
        val otherApk = "https://github.com/x/y/releases/download/v1.0.9/ivy.apk"
        pickUpdate(
            release(
                assets = listOf(
                    AssetDto("ivy.apk", otherApk),
                    AssetDto("app-release.apk", apkUrl),
                )
            ),
            "1.0.8",
            null,
        )?.downloadUrl shouldBe apkUrl
        pickUpdate(
            release(assets = listOf(AssetDto("checksums.txt", "https://x/c"), AssetDto("ivy.APK", otherApk))),
            "1.0.8",
            null,
        )?.downloadUrl shouldBe otherApk
        pickUpdate(release(assets = emptyList()), "1.0.8", null)?.downloadUrl shouldBe pageUrl
    }

    @Test
    fun `only opens https links`() {
        pickUpdate(
            release(assets = listOf(AssetDto("app-release.apk", "http://evil/app.apk"))),
            "1.0.8",
            null,
        )?.downloadUrl shouldBe pageUrl
        pickUpdate(
            release(htmlUrl = "javascript:alert(1)", assets = emptyList()),
            "1.0.8",
            null,
        ).shouldBeNull()
    }
}
