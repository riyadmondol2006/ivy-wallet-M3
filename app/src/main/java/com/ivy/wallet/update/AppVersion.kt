package com.ivy.wallet.update

/**
 * A dotted numeric version such as `1.0.9`, parsed from a version name or a GitHub tag (`v1.0.9`).
 * Missing trailing parts count as zero, so `1.1` == `1.1.0`.
 */
class AppVersion private constructor(private val parts: List<Int>) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val size = maxOf(parts.size, other.parts.size)
        for (i in 0 until size) {
            val diff = parts.getOrElse(i) { 0 }.compareTo(other.parts.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0

    override fun hashCode(): Int = parts.dropLastWhile { it == 0 }.hashCode()

    override fun toString(): String = parts.joinToString(".")

    companion object {
        private val VERSION_REGEX = Regex("""^[vV]?(\d+(?:\.\d+)*)""")

        /**
         * Reads the leading `x.y.z` of [raw]; a build suffix like `-beta` or `+42` is ignored.
         * Returns null when [raw] does not start with a version number.
         */
        fun parse(raw: String?): AppVersion? {
            val match = VERSION_REGEX.find(raw?.trim().orEmpty()) ?: return null
            val parts = match.groupValues[1].split('.').map { it.toIntOrNull() ?: return null }
            return AppVersion(parts)
        }
    }
}
