package com.dark.darknama.data.repository

import com.dark.darknama.data.model.Episode
import com.dark.darknama.data.model.EpisodeQualityLinks
import com.dark.darknama.data.model.Season
import java.util.Locale

/** Keeps download-link grouping/order independent of the clipboard and Compose UI. */
object EpisodeLinkGroups {
    fun forSeason(season: Season): List<EpisodeQualityLinks> {
        val groups = linkedMapOf<String, MutableGroup>()
        for (episode in orderedEpisodes(season.episodes)) {
            // Several sources of the same quality can exist; ignore only identical links
            // within one episode, not across different episodes.
            val seenInEpisode = mutableSetOf<Pair<String, String>>()
            for (source in episode.sources) {
                val url = source.url.trim()
                if (url.isEmpty()) continue
                val quality = source.quality.trim()
                val key = quality.lowercase(Locale.ROOT)
                if (!seenInEpisode.add(key to url)) continue
                groups.getOrPut(key) { MutableGroup(quality) }.links.add(url)
            }
        }
        return groups.values.map { EpisodeQualityLinks(it.quality, it.links.toList()) }
    }

    // Some API responses list episodes in reverse order. Sort only when every title
    // explicitly contains an episode number; otherwise preserve the API's order.
    /** Shared display/download order: sort explicit episode numbers, preserve unknown API order. */
    fun orderedEpisodes(episodes: List<Episode>): List<Episode> {
        if (episodes.any { episodeNumber(it.title) == null }) return episodes
        return episodes.withIndex()
            .sortedWith(compareBy<IndexedValue<Episode>> { episodeNumber(it.value.title)!! }
                .thenBy { it.index })
            .map { it.value }
    }

    private val episodePattern = Regex(
        """(?i)(?:^|[\s._-])(?:episode|ep|قسمت)\s*[:#-]?\s*([0-9۰-۹٠-٩]+)"""
    )
    private val seasonEpisodePattern = Regex("""(?i)s[0-9۰-۹٠-٩]+e([0-9۰-۹٠-٩]+)""")

    private fun episodeNumber(title: String): Int? {
        val digits = seasonEpisodePattern.find(title)?.groupValues?.get(1)
            ?: episodePattern.find(title)?.groupValues?.get(1)
            ?: return null
        val ascii = digits.map { Character.digit(it, 10) }.joinToString("")
        return ascii.toIntOrNull()
    }

    private class MutableGroup(val quality: String, val links: MutableList<String> = mutableListOf())
}
