package com.dark.darknama.data.model

import com.dark.darknama.data.repository.EpisodeLinkGroups
import kotlinx.serialization.Serializable

/** Persisted by series ID: season and quality are stable identifiers, not list indices. */
@Serializable
data class SeriesLinkSelection(
    val seasonId: Int? = null,
    val quality: String? = null,
    val qualityBySeason: Map<Int, String> = emptyMap()
)

/** Only the API's real seasons/tracks and nonblank source links are selectable. */
object SeriesLinkSelector {
    fun qualities(season: Season): List<String> =
        EpisodeLinkGroups.forSeason(season).map { it.quality }
            .filter(MediaSourceSelection::hasNamedQuality)

    fun normalize(seasons: List<Season>, selection: SeriesLinkSelection): SeriesLinkSelection {
        // Drop remembered choices that disappeared or are unnamed, even on another season.
        val remembered = selection.qualityBySeason.mapNotNull { (id, chosen) ->
            val option = seasons.firstOrNull { it.id == id }?.let { season ->
                qualities(season).firstOrNull { it.equals(chosen, ignoreCase = true) }
            }
            option?.let { id to it }
        }.toMap()
        val season = seasons.firstOrNull { it.id == selection.seasonId } ?: seasons.firstOrNull()
            ?: return SeriesLinkSelection()
        val options = qualities(season)
        val preferred = if (season.id == selection.seasonId) selection.quality
            else remembered[season.id] ?: selection.quality
        val quality = options.firstOrNull { it.equals(preferred, ignoreCase = true) }
            ?: options.firstOrNull()
        return SeriesLinkSelection(
            seasonId = season.id,
            quality = quality,
            qualityBySeason = if (quality != null) remembered + (season.id to quality)
                else remembered - season.id
        )
    }

    fun chooseSeason(seasons: List<Season>, selection: SeriesLinkSelection, seasonId: Int): SeriesLinkSelection {
        val current = normalize(seasons, selection)
        if (seasons.none { it.id == seasonId }) return current
        // Restore this season's own choice; only carry the current quality if it has
        // never been selected here. Normalization then handles unavailable qualities.
        val preferred = current.qualityBySeason[seasonId] ?: current.quality
        return normalize(seasons, current.copy(seasonId = seasonId, quality = preferred))
    }

    fun chooseQuality(seasons: List<Season>, selection: SeriesLinkSelection, quality: String): SeriesLinkSelection {
        val current = normalize(seasons, selection)
        val season = seasons.firstOrNull { it.id == current.seasonId } ?: return current
        val available = qualities(season).firstOrNull { it.equals(quality, ignoreCase = true) }
            ?: return current
        return current.copy(quality = available,
            qualityBySeason = current.qualityBySeason + (season.id to available))
    }

    /** Only real episode links at the selected quality belong in the season download picker. */
    fun downloadableEpisodes(season: Season, quality: String?): List<Episode> {
        val directLinksOnly = qualities(season).isEmpty()
        return EpisodeLinkGroups.orderedEpisodes(season.episodes).mapNotNull { episode ->
            val sources = if (directLinksOnly) MediaSourceSelection.available(episode.sources)
                else MediaSourceSelection.forQuality(episode.sources, quality)
            if (sources.isEmpty()) null else episode.copy(sources = sources)
        }
    }

    /** Playable links at the selected quality, or direct episode URLs when unnamed. */
    fun links(seasons: List<Season>, selection: SeriesLinkSelection): List<String> {
        val current = normalize(seasons, selection)
        val season = seasons.firstOrNull { it.id == current.seasonId } ?: return emptyList()
        if (current.quality == null) return EpisodeLinkGroups.orderedEpisodes(season.episodes)
            .flatMap { episode -> MediaSourceSelection.available(episode.sources)
                .map { it.url.trim() }.distinct() }
        return EpisodeLinkGroups.forSeason(season)
            .firstOrNull { it.quality.equals(current.quality, ignoreCase = true) }
            ?.links.orEmpty()
    }

    /** Copy each episode's selected link in order, including URLs shared between episodes. */
    fun copyAllLinks(seasons: List<Season>, selection: SeriesLinkSelection): List<String> =
        links(seasons, selection)
}
