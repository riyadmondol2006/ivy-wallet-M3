package com.ivy.data.sync

import com.ivy.data.sync.model.RemoteSyncMeta
import io.kotest.matchers.shouldBe
import org.junit.Test

class RemoteStatusTest {

    private val meta = RemoteSyncMeta(
        deviceId = "other-device",
        updatedAt = 123L,
        accounts = 2,
        appVersion = "2.1.0",
    )

    @Test
    fun `prompts to pull when a newer backup was written by another device`() {
        val status = RemoteStatus(
            exists = true,
            meta = meta,
            isFromOtherDevice = true,
            isNewer = true,
        )

        status.shouldPromptPull shouldBe true
    }

    @Test
    fun `does not prompt when this device wrote the latest backup`() {
        val status = RemoteStatus(
            exists = true,
            meta = meta,
            isFromOtherDevice = false,
            isNewer = true,
        )

        status.shouldPromptPull shouldBe false
    }

    @Test
    fun `does not prompt when the remote revision is already synced`() {
        val status = RemoteStatus(
            exists = true,
            meta = meta,
            isFromOtherDevice = true,
            isNewer = false,
        )

        status.shouldPromptPull shouldBe false
    }

    @Test
    fun `does not prompt when there is no remote backup`() {
        RemoteStatus.empty().shouldPromptPull shouldBe false
    }
}
