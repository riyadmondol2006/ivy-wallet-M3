package com.ivy.data.sync

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class SyncErrorMessagesTest {

    @Test
    fun `explains an unknown host instead of echoing it`() {
        SyncErrorMessages.describe(UnknownHostException("db.upstash.io")) shouldBe
            "No internet connection, or the host db.upstash.io doesn't exist"
    }

    @Test
    fun `explains timeouts and refused connections`() {
        SyncErrorMessages.describe(SocketTimeoutException("timeout")) shouldContain "Could not connect"
        SyncErrorMessages.describe(ConnectException("refused")) shouldContain "Could not connect"
    }

    @Test
    fun `explains certificate problems`() {
        SyncErrorMessages.describe(SSLHandshakeException("Hostname db.upstash.io not verified")) shouldBe
            "Secure connection failed: Hostname db.upstash.io not verified"
    }

    @Test
    fun `keeps other messages and never returns an empty one`() {
        SyncErrorMessages.describe(IllegalStateException("Unexpected Redis reply")) shouldBe
            "Unexpected Redis reply"
        SyncErrorMessages.describe(IllegalStateException()) shouldBe "Connection error"
    }

    @Test
    fun `explains redis authentication replies`() {
        SyncErrorMessages.describeRedisReply(
            "WRONGPASS invalid username-password pair or user is disabled."
        ) shouldContain "Wrong password"
        SyncErrorMessages.describeRedisReply("NOAUTH Authentication required.") shouldContain "password"
        SyncErrorMessages.describeRedisReply("ERR something") shouldBe "Redis error: ERR something"
    }
}
