package com.dark.darknama.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.components.MediaLinkArrow
import com.dark.darknama.components.MediaLinkBadge
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.MovieSourceGroups
import com.dark.darknama.data.model.Source

@Composable
fun MovieLinksSection(
    movie: Movie,
    onSourceClick: (Source) -> Unit, onDownloadSources: (List<Source>) -> Unit
) {
    val groups = remember(movie.sources) { MovieSourceGroups.forMovie(movie.sources) }
    val playable = remember(groups) { MovieSourceGroups.allSources(groups) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.movie_links_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        TextButton(onClick = { onDownloadSources(playable) }, enabled = playable.isNotEmpty(),
            interactionSource = interaction,
            modifier = (if (playable.isNotEmpty()) Modifier.tvFocusItem("movie-links-download")
                else Modifier).settingsFocusBorder(focused, CircleShape)) {
            Text(stringResource(R.string.choose_download_link), maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        if (playable.isEmpty()) {
            Text(stringResource(R.string.no_movie_links),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            groups.forEach { group ->
                group.quality?.let { quality ->
                    Text(quality, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                group.sources.forEachIndexed { index, source ->
                    val interaction = remember(source.url, source.quality) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Card(
                        onClick = { onSourceClick(source) }, interactionSource = interaction,
                        modifier = Modifier.tvFocusItem("movie-link-${group.quality}-$index-${source.url}")
                            .fillMaxWidth().settingsFocusBorder(focused, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            MediaLinkBadge()
                            Spacer(Modifier.width(12.dp))
                            Text(if (group.quality == null)
                                stringResource(R.string.movie_direct_link_number, index + 1)
                                else stringResource(R.string.media_link_number, index + 1, group.quality),
                                modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                            MediaLinkArrow()
                        }
                    }
                }
            }
        }
    }
}
