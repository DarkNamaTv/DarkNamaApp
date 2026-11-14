package com.dark.darknama.data.model

import java.util.Locale

/** Display order for every real movie link, independent of Compose. */
data class MovieSourceGroup(val quality: String?, val sources: List<Source>)

object MovieSourceGroups {
    private val resolution = Regex("""(?<![a-zA-Z\d])(\d{3,4})\s*p?(?!\d)""", RegexOption.IGNORE_CASE)

    fun forMovie(sources: List<Source>): List<MovieSourceGroup> {
        val named = linkedMapOf<String, MutableList<Source>>()
        val direct = mutableListOf<Source>()
        val directUrls = mutableSetOf<String>()
        for (source in MediaSourceSelection.available(sources)) {
            val quality = source.quality.trim()
            if (MediaSourceSelection.hasNamedQuality(quality)) {
                named.getOrPut(quality.lowercase(Locale.ROOT)) { mutableListOf() }.add(source)
            } else if (directUrls.add(source.url.trim())) {
                direct.add(source)
            }
        }
        val groups = named.values.map { group ->
            MovieSourceGroup(group.first().quality.trim(), group.toList())
        }.sortedWith(compareByDescending<MovieSourceGroup> { qualityRank(it.quality.orEmpty()) }
            .thenBy { it.quality?.lowercase(Locale.ROOT) })
        return groups + if (direct.isEmpty()) emptyList()
            else listOf(MovieSourceGroup(null, direct))
    }

    fun allSources(groups: List<MovieSourceGroup>): List<Source> = groups.flatMap { it.sources }

    private fun qualityRank(quality: String): Int {
        val label = quality.lowercase(Locale.ROOT)
        return when {
            "8k" in label -> 4320
            "4k" in label || "uhd" in label -> 2160
            else -> resolution.findAll(label).mapNotNull { it.groupValues[1].toIntOrNull() }
                .maxOrNull() ?: when {
                "fhd" in label || "full hd" in label -> 1080
                "hd" in label -> 720
                "sd" in label -> 480
                else -> 0 // Other named qualities still precede the unlabeled group.
            }
        }
    }
}
