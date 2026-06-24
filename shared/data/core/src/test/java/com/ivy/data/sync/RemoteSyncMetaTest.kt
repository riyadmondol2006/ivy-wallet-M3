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
}
