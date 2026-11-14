package com.dark.darknama.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.navigation.LocalTvFocusEntry
import com.dark.darknama.navigation.LocalTvFocusRegistry
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.VideoPlayerActivity
import com.dark.darknama.components.SourceDownloadOptions
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.ExpandableText
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.copyTitleOnClick
import com.dark.darknama.data.model.FavoriteItem
import com.dark.darknama.data.model.MediaSourceSelection
import com.dark.darknama.data.model.Episode
import com.dark.darknama.data.model.EpisodeId
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.data.model.Season
import com.dark.darknama.data.model.SeriesLinkSelector
import com.dark.darknama.data.model.SeriesLinkSelection
import com.dark.darknama.data.model.Series
import com.dark.darknama.data.repository.EpisodeLinkGroups
import com.dark.darknama.data.model.Source
import com.dark.darknama.ui.series.SeasonsViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.DownloadUtils
import com.dark.darknama.utils.StorageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSeriesScreen(
    seriesId: Int,
    navController: NavController
) {
    var series by remember { mutableStateOf<Series?>(null) }
    val context = LocalContext.current
    val seasonsViewModel: SeasonsViewModel = viewModel()
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var selectedEpisodeSeason by remember { mutableStateOf<Season?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showDownloadMenu by remember { mutableStateOf(false) }
    var downloadSources by remember { mutableStateOf<List<Source>>(emptyList()) }
    var linkSelection by remember(seriesId) {
        mutableStateOf(StorageUtils.loadSeriesLinkSelection(context, seriesId))
    }
    val selection = remember(seasonsViewModel.seasons, linkSelection) {
        SeriesLinkSelector.normalize(seasonsViewModel.seasons, linkSelection)
    }
    var episodeHistory by remember(seriesId) { mutableStateOf<Map<EpisodeId, PlaybackRecord>>(emptyMap()) }
    var startedEpisodeIds by remember(seriesId) { mutableStateOf<Set<EpisodeId>>(emptySet()) }

    LaunchedEffect(seriesId) {
        series = StorageUtils.loadSeriesFromFile(context, seriesId)
        seasonsViewModel.loadSeasons(seriesId)
    }
    // Focus the new back control when the initial loading layout is replaced.
    val focusRegistry = LocalTvFocusRegistry.current
    val focusEntry = LocalTvFocusEntry.current
    LaunchedEffect(series?.id, focusEntry) {
        if (series != null && DeviceUtils.isTv(context)) {
            withFrameNanos { }
            focusRegistry?.restore(focusEntry)
        }
    }

    LaunchedEffect(seasonsViewModel.loadedSeriesId, selection) {
        if (seasonsViewModel.loadedSeriesId == seriesId && selection != linkSelection) {
            linkSelection = selection // Keep a valid choice after season/quality data changes.
            StorageUtils.saveSeriesLinkSelection(context, seriesId, selection)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, seriesId) {
        fun refreshProgress() {
            episodeHistory = StorageUtils.loadPlaybackHistory(context)
                .filter { it.type == "series" && it.contentId == seriesId &&
                    it.seasonId != null && it.episodeId != null }
                .associateBy { EpisodeId(it.seasonId!!, it.episodeId!!) }
            startedEpisodeIds = StorageUtils.loadAllWatchedEpisodes(context)
                .filter { it.seriesId == seriesId }
                .map { EpisodeId(it.seasonId, it.episodeId) }.toSet()
        }
        refreshProgress()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun chooseSeason(seasonId: Int) {
        linkSelection = SeriesLinkSelector.chooseSeason(seasonsViewModel.seasons, linkSelection, seasonId)
        StorageUtils.saveSeriesLinkSelection(context, seriesId, linkSelection)
    }

    fun chooseQuality(quality: String) {
        linkSelection = SeriesLinkSelector.chooseQuality(seasonsViewModel.seasons, linkSelection, quality)
        StorageUtils.saveSeriesLinkSelection(context, seriesId, linkSelection)
    }
    
    // Source selection dialog
    if (showSourceDialog && selectedEpisode != null && series != null && selectedEpisodeSeason != null) {
        val selectedSeason = selectedEpisodeSeason!!
        SourceOptionsDialog(
            episode = selectedEpisode!!,
            onDismiss = { showSourceDialog = false },
            onPlay = { source ->
                showSourceDialog = false
                // Launch video player activity with the selected source URL and episode info
                VideoPlayerActivity.startWithEpisodeInfo(
                    context,
                    source.url,
                    series!!.id,
                    selectedSeason.id,
                    selectedEpisode!!.id,
                    source.type,
                    seriesTitle = series!!.title, seasonTitle = selectedSeason.title,
                    episodeTitle = selectedEpisode!!.title,
                    image = selectedEpisode!!.image.ifBlank { series!!.image },
                    quality = source.quality
                )
            }
        )
    }
    
    // Download menu
    if (showDownloadMenu && downloadSources.isNotEmpty()) {
        DownloadMenu(
            sources = downloadSources,
            onDismiss = { showDownloadMenu = false }
        )
    }
    
    // Directly render content without Scaffold since it's already in MainScreen's Scaffold
    if (series != null) {
        SeriesDetailsContent(
            series = series!!,
            seasonsViewModel = seasonsViewModel,
            selection = selection,
            episodeHistory = episodeHistory,
            startedEpisodeIds = startedEpisodeIds,
            onSeasonSelected = ::chooseSeason,
            onQualitySelected = ::chooseQuality,
            onCopyLinks = { links ->
                if (links.isNotEmpty()) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                        context.getString(R.string.episode_links_clipboard), links.joinToString("\n")
                    ))
                    Toast.makeText(
                        context,
                        context.resources.getQuantityString(
                            R.plurals.episode_links_copied, links.size, links.size
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onBackClick = { navController.popBackStack() },
            onEpisodeClick = { season, episode ->
                val playable = episode.copy(sources = episode.sources.filter { it.url.isNotBlank() })
                if (playable.sources.isNotEmpty()) {
                    if (playable.sources.size > 1) {
                        selectedEpisode = playable
                        selectedEpisodeSeason = season
                        showSourceDialog = true
                    } else {
                        val source = playable.sources.first()
                        VideoPlayerActivity.startWithEpisodeInfo(
                            context, source.url, series!!.id, season.id, episode.id, source.type,
                            seriesTitle = series!!.title, seasonTitle = season.title,
                            episodeTitle = episode.title,
                            image = episode.image.ifBlank { series!!.image },
                            quality = source.quality
                        )
                    }
                }
            },
            onDownloadClick = { _, episode ->
                val links = episode.sources.filter { it.url.isNotBlank() }
                if (links.isNotEmpty()) {
                    downloadSources = links
                    showDownloadMenu = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Show loading or error state
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { navController.popBackStack() },
                    modifier = Modifier.tvFocusItem("series-back", initial = true)
                        .tvFocusBorder(CircleShape)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = stringResource(R.string.series_not_found),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SourceOptionsDialog(
    episode: Episode,
    onDismiss: () -> Unit,
    onPlay: (Source) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.episode_label, episode.title),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.choose_quality_play),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                episode.sources.forEachIndexed { index, source ->
                    val interaction = remember(source.url) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Button(onClick = { onPlay(source) }, interactionSource = interaction,
                        modifier = (if (index == 0) Modifier.tvDialogInitialFocus() else Modifier)
                            .fillMaxWidth().settingsFocusBorder(focused, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (MediaSourceSelection.hasNamedQuality(source.quality))
                            stringResource(R.string.play_quality, source.quality)
                            else stringResource(R.string.play_episode), modifier = Modifier.weight(1f))
                    }
                }
                TextButton(onClick = onDismiss,
                    modifier = (if (episode.sources.isEmpty()) Modifier.tvDialogInitialFocus()
                        else Modifier).fillMaxWidth().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp
    )
}

/** Pick one real link; the next step keeps the existing browser/ADM/external-player actions. */
@Composable
fun DownloadMenu(
    sources: List<Source>, onDismiss: () -> Unit
) {
    if (sources.isEmpty()) return
    var selectedSource by remember(sources) {
        mutableStateOf(if (sources.size == 1) sources.first() else null)
    }
    selectedSource?.let { source ->
        SourceDownloadOptions(source, onDismiss)
    } ?: AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.media_pick_link),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.choose_download_link),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEachIndexed { index, source ->
                    val interaction = remember(source.url) { MutableInteractionSource() }
                    val focused by interaction.collectIsFocusedAsState()
                    Button(
                        onClick = { selectedSource = source }, interactionSource = interaction,
                        modifier = (if (index == 0) Modifier.tvDialogInitialFocus() else Modifier)
                            .fillMaxWidth().settingsFocusBorder(focused, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null,
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (MediaSourceSelection.hasNamedQuality(source.quality))
                            stringResource(R.string.media_source_option, index + 1, source.quality)
                            else stringResource(R.string.media_link_without_quality, index + 1),
                            modifier = Modifier.weight(1f), maxLines = 2)
                    }
                }
                TextButton(onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp
    )
}

@Composable
fun SeriesDetailsContent(
    series: Series,
    seasonsViewModel: SeasonsViewModel,
    selection: SeriesLinkSelection,
    episodeHistory: Map<EpisodeId, PlaybackRecord>,
    startedEpisodeIds: Set<EpisodeId>,
    onSeasonSelected: (Int) -> Unit,
    onQualitySelected: (String) -> Unit,
    onCopyLinks: (List<String>) -> Unit,
    onBackClick: () -> Unit,
    onEpisodeClick: (Season, Episode) -> Unit,
    onDownloadClick: (Season, Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedSeason = seasonsViewModel.seasons.firstOrNull { it.id == selection.seasonId }
    val episodeRows = remember(selectedSeason) {
        selectedSeason?.let { EpisodeLinkGroups.orderedEpisodes(it.episodes) }.orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            // Series header with background cover and foreground image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // Background cover image (blurred)
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(series.cover)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop
                )
                
                // Gradient overlay for better text visibility
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                
                // Row to contain poster and series details side by side
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Foreground series poster
                    var showImageDialog by remember { mutableStateOf(false) }
                    
                    Image(
                        painter = rememberAsyncImagePainter(
                            ImageRequest.Builder(LocalContext.current)
                                .data(series.image)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = series.title,
                        modifier = Modifier
                            .height(200.dp)
                            .tvFocusItem("series-poster")
                            .tvFocusBorder(RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showImageDialog = true },
                        contentScale = ContentScale.Fit
                    )
                    
                    // Image URL dialog
                    if (showImageDialog) {
                        AlertDialog(
                            onDismissRequest = { showImageDialog = false },
                            title = { Text("Image Options") },
                            text = { Text("Choose an action for this image") },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.tvFocusBorder(CircleShape),
                                    onClick = {
                                        DownloadUtils.copyToClipboard(context, series.image)
                                        showImageDialog = false
                                    }
                                ) {
                                    Text("Copy Image URL")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showImageDialog = false },
                                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                                ) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                    
                    // Series details to the right of the poster
                    Column(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(1f)
                    ) {
                        // Series title
                        Text(
                            text = series.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.tvFocusItem("series-copy-title")
                                .copyTitleOnClick(series.title)
                        )
                        
                        // Country and year
                        val countryText = if (series.country.isNotEmpty()) {
                            "${series.country.joinToString(", ") { it.title }} (${series.year})"
                        } else {
                            "(${series.year})"
                        }
                        
                        Text(
                            text = countryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Rating
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color.Yellow,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = String.format("%.1f", series.imdb),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Back button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .tvFocusItem("series-back", initial = true)
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                        .tvFocusBorder(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Favorite button
                var isFavorite by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val seriesId = series.id
                
                // Check if series is already favorite
                LaunchedEffect(seriesId) {
                    isFavorite = StorageUtils.isFavorite(context, seriesId, "series")
                }
                
                var showRemoveFavoriteDialog by remember { mutableStateOf(false) }
                
                // Confirmation dialog for removing from favorites
                if (showRemoveFavoriteDialog) {
                    AlertDialog(
                        onDismissRequest = { showRemoveFavoriteDialog = false },
                        title = { Text("Remove from Favorites") },
                        text = { Text("Are you sure you want to remove this series from your favorites?") },
                        confirmButton = {
                            TextButton(
                                modifier = Modifier.tvFocusBorder(CircleShape),
                                onClick = {
                                    StorageUtils.removeFavorite(context, seriesId, "series")
                                    isFavorite = false
                                    showRemoveFavoriteDialog = false
                                    // Show toast
                                    android.widget.Toast.makeText(context, "Removed from favorites", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Remove")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRemoveFavoriteDialog = false },
                                modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
                
                IconButton(
                    onClick = {
                        if (isFavorite) {
                            // Show confirmation dialog instead of directly removing
                            showRemoveFavoriteDialog = true
                        } else {
                            // Convert series to favorite item
                            val favoriteItem = FavoriteItem(
                                id = series.id,
                                type = "series",
                                title = series.title,
                                description = series.description,
                                year = series.year,
                                imdb = series.imdb,
                                rating = series.rating,
                                duration = series.duration,
                                image = series.image,
                                cover = series.cover,
                                genres = series.genres,
                                country = series.country
                            )
                            StorageUtils.saveFavorite(context, favoriteItem)
                            isFavorite = true
                            // Show toast
                            android.widget.Toast.makeText(context, "Added to favorites", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.tvFocusItem("series-favorite")
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .tvFocusBorder(CircleShape)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        item {
            // Genres
            if (series.genres.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.genres_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
                
                // Improved genres display with better wrapping and styling
                LazyRow(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(series.genres) { genre ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(50.dp), // More rounded corners
                            modifier = Modifier
                                .height(32.dp) // Fixed height for consistency
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = genre.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            // Description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.description_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        item {
            // Set layout direction to RTL for the description text
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ExpandableText(
                    text = series.description,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                )
            }
        }
        
        item(key = "episode-header") {
            SeriesEpisodeHeader(
                seasons = seasonsViewModel.seasons,
                isLoading = seasonsViewModel.isLoading,
                errorMessage = seasonsViewModel.errorMessage,
                selection = selection,
                onRetry = { seasonsViewModel.loadSeasons(series.id) },
                onSeasonSelected = onSeasonSelected,
                onQualitySelected = onQualitySelected,
                onCopyLinks = onCopyLinks,
                onDownloadClick = onDownloadClick
            )
        }
        if (selectedSeason != null && seasonsViewModel.loadedSeriesId == series.id &&
            !seasonsViewModel.isLoading && seasonsViewModel.errorMessage == null) {
            itemsIndexed(episodeRows,
                key = { index, episode -> "episode-${selectedSeason.id}-${episode.id}-$index" }
            ) { index, episode ->
                val episodeId = EpisodeId(selectedSeason.id, episode.id)
                SeriesEpisodeRow(
                    episode = episode,
                    focusKey = "episode-${selectedSeason.id}-${episode.id}-$index",
                    selectedQuality = selection.quality,
                    history = episodeHistory[episodeId],
                    legacyStarted = episodeId in startedEpisodeIds,
                    onPlay = { chosen -> onEpisodeClick(selectedSeason, chosen) },
                    onDownload = { chosen -> onDownloadClick(selectedSeason, chosen) },
                    onCopyLinks = onCopyLinks
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun openUrlSeries(context: Context, url: String) {
    DownloadUtils.openUrl(context, url)
}

