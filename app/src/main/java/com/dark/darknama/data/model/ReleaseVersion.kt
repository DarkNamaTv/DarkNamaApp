package com.dark.darknama.data.model

/** Compares version tags numerically, with SemVer prereleases below their stable version. */
object ReleaseVersion {
    private val pattern = Regex("""^(\d+(?:\.\d+)*)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?$""")

    fun isNewer(installed: String, candidate: String): Boolean {
        val current = parse(installed) ?: return false
        val latest = parse(candidate) ?: return false
        val count = maxOf(current.first.size, latest.first.size)
        for (index in 0 until count) {
            val result = compareNumber(latest.first.getOrElse(index) { "0" },
                current.first.getOrElse(index) { "0" })
            if (result != 0) return result > 0
        }
        val candidatePrerelease = latest.second
        val installedPrerelease = current.second
        if (candidatePrerelease == null) return installedPrerelease != null
        if (installedPrerelease == null) return false
        for (index in 0 until minOf(candidatePrerelease.size, installedPrerelease.size)) {
            val newer = candidatePrerelease[index]
            val older = installedPrerelease[index]
            val result = when {
                newer.all(Char::isDigit) && older.all(Char::isDigit) -> compareNumber(newer, older)
                newer.all(Char::isDigit) -> -1
                older.all(Char::isDigit) -> 1
                else -> newer.compareTo(older)
            }
            if (result != 0) return result > 0
        }
        return candidatePrerelease.size > installedPrerelease.size
    }

    private fun parse(version: String): Pair<List<String>, List<String>?>? {
        val value = version.trim().removePrefix("v").removePrefix("V")
        val match = pattern.matchEntire(value) ?: return null
        val prerelease = match.groupValues[2].takeIf { it.isNotEmpty() }?.split('.')
        if (prerelease?.any { it.isEmpty() } == true) return null
        return match.groupValues[1].split('.') to prerelease
    }

    private fun compareNumber(left: String, right: String): Int {
        val a = left.trimStart('0').ifEmpty { "0" }
        val b = right.trimStart('0').ifEmpty { "0" }
        return a.length.compareTo(b.length).takeIf { it != 0 } ?: a.compareTo(b)
    }
}
