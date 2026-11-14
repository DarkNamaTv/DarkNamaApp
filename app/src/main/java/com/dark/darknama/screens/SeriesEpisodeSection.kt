package com.dark.darknama.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.MediaCopyAllButton
import com.dark.darknama.components.MediaLinkSelector
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.data.model.Episode
import com.dark.darknama.data.model.MediaSourceSelection
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.data.model.Season
import com.dark.darknama.data.model.SeriesLinkSelection
import com.dark.darknama.data.model.SeriesLinkSelector
import com.dark.darknama.formatTime

/** The selector header is one lazy-list item; episodes below it are independent lazy rows. */
@Composable
fun SeriesEpisodeHeader(
    seasons: List<Season>, isLoading: Boolean, errorMessage: String?,
    selection: SeriesLinkSelection,
    onRetry: () -> Unit, onSeasonSelected: (Int) -> Unit,
    onQualitySelected: (String) -> Unit, onCopyLinks: (List<String>) -> Unit,
    onDownloadClick: (Season, Episode) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        val links = remember(seasons, selection) { SeriesLinkSelector.links(seasons, selection) }
        val copyableLinks = remember(seasons, selection) {
            SeriesLinkSelector.copyAllLinks(seasons, selection)
        }
        Text(stringResource(R.string.episode_links_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        when {
            isLoading -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.loading_seasons),
                    color = MaterialTheme.colorScheme.onBackground)
            }
            errorMessage != null -> Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.failed_load_seasons), color = MaterialTheme.colorScheme.error)
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Button(onClick = onRetry, interactionSource = interaction,
                    modifier = Modifier.tvFocusItem("series-episodes-retry")
                        .settingsFocusBorder(focused, CircleShape).padding(top = 8.dp)) {
                    Text(stringResource(R.string.retry))
                }
            }
            seasons.isEmpty() -> Text(stringResource(R.string.no_seasons_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp))
            else -> {
                val season = seasons.firstOrNull { it.id == selection.seasonId } ?: seasons.first()
                val qualities = remember(season) { SeriesLinkSelector.qualities(season) }
                val downloadable = remember(season, selection.quality) {
                    SeriesLinkSelector.downloadableEpisodes(season, selection.quality)
                }
                var showDownloads by remember(season.id, selection.quality) { mutableStateOf(false) }
                SeriesLinkControls(
                    seasons = seasons, selectedSeason = season, selectedQuality = selection.quality,
                    qualities = qualities, downloadable = downloadable,
                    copyableLinks = copyableLinks, onCopyLinks = onCopyLinks,
                    onSeasonSelected = onSeasonSelected, onQualitySelected = onQualitySelected,
                    onDownloadClick = { if (downloadable.size == 1) onDownloadClick(season, downloadable.first())
                        else showDownloads = true }
                )
                if (season.episodes.isEmpty()) {
                    Text(stringResource(R.string.no_episodes_available),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp))
                } else if (links.isEmpty()) {
                    Text(stringResource(R.string.no_episode_links),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                if (showDownloads) {
                    SeasonDownloadPicker(season, downloadable,
                        onDismiss = { showDownloads = false },
                        onDownload = { episode ->
                            showDownloads = false
                            onDownloadClick(season, episode)
                        })
                }
            }
        }
    }
}

@Composable
private fun SeriesLinkControls(
    seasons: List<Season>, selectedSeason: Season, selectedQuality: String?,
    qualities: List<String>, downloadable: List<Episode>, copyableLinks: List<String>,
    onSeasonSelected: (Int) -> Unit, onQualitySelected: (String) -> Unit,
    onCopyLinks: (List<String>) -> Unit, onDownloadClick: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        // In RTL the copy-all icon precedes the season/track and quality selectors.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            MediaCopyAllButton(enabled = copyableLinks.isNotEmpty(),
                onClick = { onCopyLinks(copyableLinks) },
                modifier = if (copyableLinks.isNotEmpty())
                    Modifier.tvFocusItem("series-copy-all") else Modifier)
            MediaLinkSelector(
                options = seasons, selected = selectedSeason,
                placeholder = stringResource(R.string.no_seasons_available),
                description = stringResource(R.string.season_track_value, selectedSeason.title),
                label = { it.title }, onSelect = { onSeasonSelected(it.id) },
                modifier = Modifier.weight(1f), focusKey = "series-season-selector"
            )
            if (qualities.isNotEmpty()) {
                val quality = qualities.firstOrNull { it.equals(selectedQuality, ignoreCase = true) }
                    ?: qualities.first()
                MediaLinkSelector(
                    options = qualities, selected = quality, placeholder = quality,
                    description = stringResource(R.string.quality_value, quality),
                    label = { it }, onSelect = onQualitySelected,
                    modifier = Modifier.weight(1f), focusKey = "series-quality-selector"
                )
            }
        }
        // The season download picker remains available as a separate, labeled action.
        if (downloadable.isNotEmpty()) {
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            TextButton(onClick = onDownloadClick, interactionSource = interaction,
                modifier = Modifier.tvFocusItem("series-download-episodes")
                    .padding(horizontal = 16.dp).settingsFocusBorder(focused, CircleShape)) {
                Text(stringResource(R.string.download_selected_episodes))
            }
        }
    }
}

@Composable
private fun SeasonDownloadPicker(
    season: Season, episodes: List<Episode>,
    onDismiss: () -> Unit, onDownload: (Episode) -> Unit
) {
    val context = LocalContext.current
    val hasNamedQuality = SeriesLinkSelector.qualities(season).isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.episode_downloads_title)) },
        text = { Text(stringResource(R.string.season_track_value, season.title)) },
        confirmButton = {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                episodes.forEachIndexed { index, episode ->
                    val interaction = remember(episode.id) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Button(onClick = { onDownload(episode) }, interactionSource = interaction,
                        modifier = (if (index == 0) Modifier.tvDialogInitialFocus() else Modifier)
                            .fillMaxWidth().settingsFocusBorder(focused, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Download, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (hasNamedQuality && episode.sources.size == 1)
                                stringResource(R.string.episode_quality_available, episode.sources.first().quality)
                                else if (hasNamedQuality) stringResource(R.string.episode_multiple_links,
                                    episode.sources.first().quality, episode.sources.size)
                                else context.resources.getQuantityString(R.plurals.episode_sources_count,
                                    episode.sources.size, episode.sources.size),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(onClick = onDismiss,
                    modifier = (if (episodes.isEmpty()) Modifier.tvDialogInitialFocus() else Modifier)
                        .fillMaxWidth().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp)
    )
}

/** Called only for visible episodes by the parent LazyColumn. The card body
 * plays; the three independent actions share its RTL-aware leading row. */
@Composable
fun SeriesEpisodeRow(
    episode: Episode, focusKey: String, selectedQuality: String?,
    history: PlaybackRecord?, legacyStarted: Boolean,
    onPlay: (Episode) -> Unit, onDownload: (Episode) -> Unit,
    onCopyLinks: (List<String>) -> Unit
) {
    val context = LocalContext.current
    val selected = remember(episode.sources, selectedQuality) {
        MediaSourceSelection.forQuality(episode.sources, selectedQuality)
    }
    val allSources = remember(episode.sources) { MediaSourceSelection.available(episode.sources) }
    val playable = allSources.isNotEmpty()
    val direct = selectedQuality == null
    val quality = selectedQuality.orEmpty()
    val sourcesForAction = if (direct) allSources else selected.ifEmpty { allSources }
    val chosenEpisode = episode.copy(sources = sourcesForAction)
    val linksToCopy = sourcesForAction.map { it.url.trim() }.distinct()
    val summary = when {
        direct && allSources.size == 1 -> stringResource(R.string.episode_direct_link)
        direct && allSources.size > 1 -> context.resources.getQuantityString(
            R.plurals.episode_sources_count, allSources.size, allSources.size)
        direct -> stringResource(R.string.no_episode_links)
        selected.size == 1 -> stringResource(R.string.episode_quality_available, quality)
        selected.size > 1 -> stringResource(R.string.episode_multiple_links, quality, selected.size)
        playable -> stringResource(R.string.episode_other_qualities, quality)
        else -> stringResource(R.string.no_episode_links)
    }
    val cardInteraction = remember { MutableInteractionSource() }
    val cardFocused by cardInteraction.collectIsFocusedAsState()
    val playDescription = stringResource(R.string.episode_play_title, episode.title)
    val shape = RoundedCornerShape(14.dp)
    Card(
        onClick = { onPlay(chosenEpisode) }, enabled = playable, interactionSource = cardInteraction,
        modifier = (if (playable) Modifier.tvFocusItem("$focusKey-card") else Modifier)
            .fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .settingsFocusBorder(cardFocused, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = if (history?.isFinished == true)
            MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Row follows LocalLayoutDirection: on RTL screens the title and the first
            // action are reached from the right without any manual icon mirroring.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(episode.title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                        modifier = if (playable) Modifier.semantics {
                            contentDescription = playDescription
                        } else Modifier,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val duration = episode.duration?.trim()?.takeIf { it.isNotBlank() }
                    Text(if (duration == null) summary else "$summary · $duration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (playable) EpisodeActionIcon(Icons.Default.PlayArrow, R.string.play_episode,
                    "$focusKey-play-action") { onPlay(chosenEpisode) }
                if (linksToCopy.isNotEmpty()) EpisodeActionIcon(Icons.Default.ContentCopy,
                    R.string.copy_episode_link, "$focusKey-copy") { onCopyLinks(linksToCopy) }
                if (playable) EpisodeActionIcon(Icons.Default.Download,
                    R.string.download_episode, "$focusKey-download") { onDownload(chosenEpisode) }
            }
            if (history != null && history.durationMs > 0L) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { history.progress }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.history_progress, (history.progress * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(if (history.isFinished) stringResource(R.string.history_finished)
                        else stringResource(R.string.history_remaining, formatTime(history.remainingMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            } else if (history?.isFinished == true) {
                Text(stringResource(R.string.history_finished), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            } else if (history != null && history.positionMs > 0L) {
                Text(stringResource(R.string.history_position, formatTime(history.positionMs)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            } else if (legacyStarted) {
                Text(stringResource(R.string.episode_started), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EpisodeActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: Int,
    focusKey: String, onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    IconButton(onClick = onClick, interactionSource = interaction,
        modifier = Modifier.tvFocusItem(focusKey).size(48.dp).settingsFocusBorder(focused, CircleShape)) {
        Icon(icon, contentDescription = stringResource(label), tint = MaterialTheme.colorScheme.primary)
    }
}
