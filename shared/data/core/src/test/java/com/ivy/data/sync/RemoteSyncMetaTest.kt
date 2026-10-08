package com.ivy.data.sync

import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.data.sync.model.RemoteSyncMeta
import io.kotest.matchers.shouldBe
import org.junit.Test

class RemoteSyncMetaTest {
    private val json = KotlinxSerializationModule.provideJson()

    @Test
    fun `round-trips through json`() {
        val meta = RemoteSyncMeta(
            deviceId = "device-a",
            updatedAt = 1_700_000_000_000L,
            accounts = 4,
            appVersion = "2.1.0",
        )

        val encoded = json.encodeToString(RemoteSyncMeta.serializer(), meta)
        val decoded = json.decodeFromString(RemoteSyncMeta.serializer(), encoded)

        decoded shouldBe meta
    }

    @Test
    fun `tolerates unknown fields written by a newer app version`() {
        val raw = """
            {"deviceId":"device-a","updatedAt":123,"accounts":4,"appVersion":"9.9","future":"x"}
        """.trimIndent()

        val decoded = json.decodeFromString(RemoteSyncMeta.serializer(), raw)

        decoded.deviceId shouldBe "device-a"
        decoded.accounts shouldBe 4
    }

    @Test
    fun `reads the meta records written by v1_0_1 and v1_0_8`() {
        listOf("m3-v1.0.1-cloud-meta", "m3-v1.0.8-cloud-meta").forEach { name ->
            val raw = com.ivy.data.testResource("backups/$name.json").readText()

            val strict = json.decodeFromString(RemoteSyncMeta.serializer(), raw)

            RemoteSyncMeta.parseLenient(json, raw) shouldBe strict
            strict.accounts shouldBe 2
        }
    }

    @Test
    fun `encoding still writes every field, so older versions can decode it`() {
        val encoded = json.encodeToString(
            RemoteSyncMeta.serializer(),
            RemoteSyncMeta(deviceId = "d", updatedAt = 1L, accounts = 0, appVersion = ""),
        )

        encoded shouldBe """{"deviceId":"d","updatedAt":1,"accounts":0,"appVersion":""}"""
    }

    @Test
    fun `a damaged meta record is read leniently instead of failing`() {
        val meta = RemoteSyncMeta.parseLenient(json, """{"deviceId":"d","accounts":"two"}""")

        meta shouldBe RemoteSyncMeta(
            deviceId = "d",
            updatedAt = RemoteSyncMeta.UNKNOWN_UPDATED_AT,
            accounts = 0,
            appVersion = "",
        )
        RemoteSyncMeta.parseLenient(json, "not json") shouldBe null
        RemoteSyncMeta.parseLenient(json, "[1,2]") shouldBe null
    }
}
