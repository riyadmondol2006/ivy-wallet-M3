package com.ivy.data.sync

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class RedisConnectionStringTest {

    @Test
    fun `reads the redis-cli command from the Upstash console`() {
        RedisConnectionString.parse(
            "redis-cli --tls -u redis://default:AbC123xyz@example-db-1.upstash.io:6379"
        ) shouldBe RedisAddress(
            host = "example-db-1.upstash.io",
            port = 6379,
            user = "default",
            password = "AbC123xyz",
        )
    }

    @Test
    fun `accepts redis and rediss urls`() {
        RedisConnectionString.parse("redis://default:pw@h.upstash.io:6380")?.port shouldBe 6380
        RedisConnectionString.parse("rediss://default:pw@h.upstash.io:6379")?.password shouldBe "pw"
        RedisConnectionString.parse("REDISS://h.upstash.io")?.port shouldBe 6379
    }

    @Test
    fun `reads urls without credentials or with only a password`() {
        RedisConnectionString.parse("rediss://h.upstash.io:6379")?.password.shouldBeNull()
        RedisConnectionString.parse("rediss://:secret@h.upstash.io:6379")?.let {
            it.user shouldBe "default"
            it.password shouldBe "secret"
        }
    }

    @Test
    fun `decodes url-encoded passwords`() {
        RedisConnectionString.parse("rediss://default:p%40ss%3Aw%2Bd@h:6379")?.password shouldBe
            "p@ss:w+d"
    }

    @Test
    fun `reads host and port`() {
        RedisConnectionString.parse("h.upstash.io:6379") shouldBe
            RedisAddress("h.upstash.io", 6379, "default", null)
        RedisConnectionString.parse("h.upstash.io")?.port shouldBe 6379
    }

    @Test
    fun `builds the urls for both connection types`() {
        val address = RedisAddress("h.upstash.io", 6379, "default", "pw")
        address.tlsUrl shouldBe "rediss://h.upstash.io:6379"
        address.restUrl shouldBe "https://h.upstash.io"
    }

    @Test
    fun `rejects things that are not redis addresses`() {
        RedisConnectionString.parse("").shouldBeNull()
        RedisConnectionString.parse("   ").shouldBeNull()
        RedisConnectionString.parse("https://h.upstash.io").shouldBeNull()
        RedisConnectionString.parse("h.upstash.io:notaport").shouldBeNull()
        RedisConnectionString.parse("some words here").shouldBeNull()
        RedisConnectionString.isRedisConnectionString("https://h.upstash.io") shouldBe false
        RedisConnectionString.isRedisConnectionString("redis-cli --tls -u redis://a@b:1") shouldBe true
    }
}
