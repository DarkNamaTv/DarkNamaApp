package com.dark.darknama.data.model

import java.util.Locale

/** A quality choice changes which *actual* source link is used for each row/action. */
object MediaSourceSelection {
    /** API quality is optional; a literal "null" may come from older JSON parsers. */
    fun hasNamedQuality(quality: String?): Boolean = !quality.isNullOrBlank() &&
        !quality.trim().equals("Unknown", ignoreCase = true) &&
        !quality.trim().equals("null", ignoreCase = true)

    fun available(sources: List<Source>): List<Source> =
        sources.filter { it.url.isNotBlank() }
            .distinctBy { it.quality.trim().lowercase(Locale.ROOT) to it.url.trim() }

    fun forQuality(sources: List<Source>, quality: String?): List<Source> =
        if (quality == null) emptyList() else available(sources).filter {
            it.quality.trim().equals(quality.trim(), ignoreCase = true)
        }
}
