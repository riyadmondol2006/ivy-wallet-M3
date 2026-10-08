package com.ivy.data.sync

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * How the backup JSON is stored in Redis.
 *
 * Backups are stored as plain JSON, exactly as every earlier app version wrote and reads them.
 * Only a backup too large for one Upstash request (10 MB) is gzip-compressed and tagged with
 * [COMPRESSED_PREFIX]. Earlier versions could not upload a backup that large at all, so this never
 * changes what an older version on another device can read.
 */
object CloudBackupCodec {
    const val COMPRESSED_PREFIX = "ivy-gzip-b64:"

    /**
     * Raw size above which a backup is compressed. Upstash rejects requests over 10 MB, and the
     * REST API sends the backup JSON-escaped inside the request body, which adds some overhead.
     */
    const val COMPRESS_THRESHOLD_BYTES = 7 * 1024 * 1024

    fun encode(backupJson: String): String {
        val raw = backupJson.toByteArray(Charsets.UTF_8)
        if (raw.size <= COMPRESS_THRESHOLD_BYTES) return backupJson

        val gzipped = ByteArrayOutputStream().use { bytes ->
            GZIPOutputStream(bytes).use { it.write(raw) }
            bytes.toByteArray()
        }
        return COMPRESSED_PREFIX + Base64.getEncoder().encodeToString(gzipped)
    }

    /** Returns the backup JSON from a stored value written by any app version. */
    fun decode(stored: String): String {
        if (!stored.startsWith(COMPRESSED_PREFIX)) return stored

        val gzipped = Base64.getDecoder().decode(stored.substring(COMPRESSED_PREFIX.length))
        return GZIPInputStream(ByteArrayInputStream(gzipped)).use {
            it.readBytes().toString(Charsets.UTF_8)
        }
    }
}
