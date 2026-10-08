package com.ivy.data.sync

import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Test

class CloudBackupCodecTest {

    @Test
    fun `normal backups stay plain json, as every app version wrote them`() {
        val json = """{"accounts":[{"name":"Cash"}]}"""

        CloudBackupCodec.encode(json) shouldBe json
        CloudBackupCodec.decode(json) shouldBe json
    }

    @Test
    fun `backups too large for one request are compressed and read back`() {
        val transaction = """{"accountId":"11f0e312-c29d-4ab8-a75e-3d5d643117c3","type":"EXPENSE",""" +
            """"amount":210.0,"title":"Groceries","dateTime":1791489704810},"""
        val json = """{"transactions":[""" +
            transaction.repeat(CloudBackupCodec.COMPRESS_THRESHOLD_BYTES / transaction.length + 1) +
            "]}"

        val stored = CloudBackupCodec.encode(json)

        stored shouldStartWith CloudBackupCodec.COMPRESSED_PREFIX
        stored.length shouldBeLessThan json.length / 10
        CloudBackupCodec.decode(stored) shouldBe json
    }

    @Test
    fun `non-ascii text survives compression`() {
        val json = "{\"title\":\"" + "কেনাকাটা 🛒 ".repeat(CloudBackupCodec.COMPRESS_THRESHOLD_BYTES / 10) + "\"}"

        CloudBackupCodec.decode(CloudBackupCodec.encode(json)) shouldBe json
    }
}
