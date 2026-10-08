package com.ivy.wallet.update

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class AppVersionTest {

    private fun v(raw: String) = requireNotNull(AppVersion.parse(raw)) { raw }

    @Test
    fun `parses tags with and without the v prefix`() {
        v("v1.0.9") shouldBe v("1.0.9")
        v("V1.0.9") shouldBe v("1.0.9")
        v(" v1.0.9 ").toString() shouldBe "1.0.9"
    }

    @Test
    fun `compares numerically, not as text`() {
        v("1.0.10") shouldBeGreaterThan v("1.0.9")
        v("1.10.0") shouldBeGreaterThan v("1.9.9")
        v("2.0.0") shouldBeGreaterThan v("1.99.99")
    }

    @Test
    fun `older and equal versions are not newer`() {
        v("v1.0.7") shouldBeLessThan v("1.0.8")
        v("v1.0.8").compareTo(v("1.0.8")) shouldBe 0
    }

    @Test
    fun `missing trailing parts count as zero`() {
        v("1.1") shouldBe v("1.1.0")
        v("1.1").hashCode() shouldBe v("1.1.0").hashCode()
        v("1.1.1") shouldBeGreaterThan v("1.1")
    }

    @Test
    fun `ignores build suffixes`() {
        v("1.0.9-beta") shouldBe v("1.0.9")
        v("1.0.9+217") shouldBe v("1.0.9")
    }

    @Test
    fun `rejects values that are not versions`() {
        AppVersion.parse(null).shouldBeNull()
        AppVersion.parse("").shouldBeNull()
        AppVersion.parse("latest").shouldBeNull()
        AppVersion.parse("release-1.0.9").shouldBeNull()
        AppVersion.parse("v99999999999.0").shouldBeNull()
    }
}
