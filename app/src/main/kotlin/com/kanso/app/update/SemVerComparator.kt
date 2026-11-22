package com.kanso.app.update

object SemVerComparator {

    fun normalizeVersion(raw: String): String {
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        val remote = parse(remoteVersion) ?: return false
        val current = parse(currentVersion) ?: return false

        val maxLen = maxOf(remote.numbers.size, current.numbers.size)
        for (i in 0 until maxLen) {
            val r = remote.numbers.getOrElse(i) { 0 }
            val c = current.numbers.getOrElse(i) { 0 }
            if (r != c) return r > c
        }

        // Same numeric version: stable (empty preRelease) > pre-release (e.g. 1.0.0 > 1.0.0-m2)
        if (remote.preRelease.isEmpty() && current.preRelease.isNotEmpty()) return true
        if (remote.preRelease.isNotEmpty() && current.preRelease.isEmpty()) return false
        if (remote.preRelease.isNotEmpty() && current.preRelease.isNotEmpty()) {
            return remote.preRelease.compareTo(current.preRelease, ignoreCase = true) > 0
        }
        return false
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val preRelease: String
    )

    private fun parse(raw: String): ParsedVersion? {
        val normalized = normalizeVersion(raw)
        if (normalized.isEmpty()) return null
        val dashIdx = normalized.indexOf('-')
        val core = if (dashIdx >= 0) normalized.substring(0, dashIdx) else normalized
        val pre = if (dashIdx >= 0) normalized.substring(dashIdx + 1) else ""
        if (core.isBlank()) return null

        val parts = core.split('.')
        val numbers = parts.map { part ->
            part.toIntOrNull() ?: return null
        }
        return ParsedVersion(numbers = numbers, preRelease = pre)
    }
}
