package com.dark.darknama.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.dark.darknama.ui.theme.GlassDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.components.GenreFilterSection
import com.dark.darknama.components.copyTitleOnClick
import com.dark.darknama.components.CompactPosterCard
import com.dark.darknama.components.CompactPosterPlaceholder
import com.dark.darknama.components.TvRefreshButton
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.data.model.Poster
import com.dark.darknama.ui.country.CountryViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

@Composable
fun CountryScreen(
    countryId: Int,
    viewModel: CountryViewModel = viewModel(),
    navController: NavController? = null
) {
    // Set the country ID when the screen is first loaded
    LaunchedEffect(countryId) {
        viewModel.setCountryId(countryId)
    }
    
    val posters = viewModel.posters
    val countryName = viewModel.countryName
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val canLoadMore = viewModel.canLoadMore
    val needsManualLoadMore = viewModel.needsManualLoadMore
    val errorMessage = viewModel.errorMessage
    val selectedFilterType = viewModel.selectedFilterType
    
    // TV country controls and results share one keyed scrolling viewport.
    if (DeviceUtils.isTv(LocalContext.current)) {
        val context = LocalContext.current
        LaunchedEffect(viewModel) { viewModel.loadGenres() }
        val displayPosters = posters.distinctBy { "${it.type}:${it.id}" }
        TvPosterGrid(
            screenKey = "country-$countryId", hasPinnedHeader = false, posters = displayPosters,
            key = { "${it.type}:${it.id}" },
            header = {
                Column {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController?.popBackStack() },
                            modifier = Modifier.tvFocusItem("country-back", initial = true)
                                .tvFocusBorder(CircleShape)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(countryName, style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(start = 16.dp))
                        TvRefreshButton(onClick = viewModel::refresh)
                    }
                    GenreFilterSection(
                        genres = viewModel.genres, selectedGenreId = viewModel.selectedGenreId,
                        selectedFilterType = selectedFilterType,
                        onGenreSelected = viewModel::selectGenre,
                        onFilterTypeSelected = viewModel::selectFilterType)
                    if (selectedFilterType != FilterType.DEFAULT) {
                        Text(stringResource(when (selectedFilterType) {
                            FilterType.DEFAULT -> R.string.sort_default
                            FilterType.BY_YEAR -> R.string.sort_status_by_year
                            FilterType.BY_IMDB -> R.string.sort_status_by_imdb
                        }), color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    }
                }
            },
            card = { poster -> CountryPosterItem(poster, onClick = {
                if (poster.isMovie()) {
                    StorageUtils.saveMovieToFile(context, poster.toMovie())
                    navController?.navigate("single_movie/${poster.id}")
                } else if (poster.isSeries()) {
                    StorageUtils.saveSeriesToFile(context, poster.toSeries())
                    navController?.navigate("single_series/${poster.id}")
                }
            }) },
            empty = {
                when {
                    isLoading -> CountryModernCircularProgressIndicator()
                    errorMessage != null -> CountryErrorScreen(errorMessage,
                        onRetry = viewModel::retry)
                    else -> Text(stringResource(R.string.no_results_found),
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            footer = {
                if (isLoadingMore) CountryModernCircularProgressIndicator()
                if (errorMessage != null && displayPosters.isNotEmpty())
                    CountryErrorItem(errorMessage, onRetry = viewModel::retry)
                if (errorMessage == null && !isLoading && !isLoadingMore &&
                    (displayPosters.isEmpty() || needsManualLoadMore) && canLoadMore) {
                    Button(onClick = viewModel::loadMorePosters,
                        modifier = Modifier.tvFocusItem("country-next-batch")
                            .tvFocusBorder(CircleShape)) {
                        Text(stringResource(R.string.load_more_results))
                    }
                }
            },
            shouldLoadMore = { index -> canLoadMore && !needsManualLoadMore &&
                !isLoading && !isLoadingMore && index == displayPosters.lastIndex },
            onLoadMore = viewModel::loadMorePosters
        )
        return
    }

    val phoneHeader: @Composable () -> Unit = {
        Column {
        // Header with back button and filter icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController?.popBackStack() },
                modifier = Modifier.tvFocusItem("country-back", initial = true)
                    .tvFocusBorder(CircleShape)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Text(
                text = countryName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            
            // Pull-to-refresh is touch-only; expose the same action on TV.
            TvRefreshButton(onClick = viewModel::refresh)
            // Filter icon button
            var filterMenuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { filterMenuExpanded = true },
                    modifier = Modifier.tvFocusItem("country-filter").tvFocusBorder(CircleShape)) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.filters_title),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.sort_default),
                            color = MaterialTheme.colorScheme.onSurface) },
                        modifier = (if (selectedFilterType == FilterType.DEFAULT)
                            Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                        onClick = {
                            viewModel.selectFilterType(FilterType.DEFAULT)
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.sort_by_year),
                            color = MaterialTheme.colorScheme.onSurface) },
                        modifier = (if (selectedFilterType == FilterType.BY_YEAR)
                            Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                        onClick = {
                            viewModel.selectFilterType(FilterType.BY_YEAR)
                            filterMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.sort_by_imdb),
                            color = MaterialTheme.colorScheme.onSurface) },
                        modifier = (if (selectedFilterType == FilterType.BY_IMDB)
                            Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                        onClick = {
                            viewModel.selectFilterType(FilterType.BY_IMDB)
                            filterMenuExpanded = false
                        }
                    )
                }
            }
        }
        
        // Show selected filter type
        if (selectedFilterType != FilterType.DEFAULT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = when (selectedFilterType) {
                        FilterType.DEFAULT -> ""
                        FilterType.BY_YEAR -> androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.sort_status_by_year)
                        FilterType.BY_IMDB -> androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.sort_status_by_imdb)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        }
    }
    when {
            isLoading && posters.isEmpty() -> {
                // Show modern loading animation when initial posters are loading
                CountryLoadingScreen(header = phoneHeader)
            }
            errorMessage != null && posters.isEmpty() -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    phoneHeader()
                CountryErrorScreen(
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() }
                )
                }
            }
            else -> {
                CountryPosterGrid(
                    header = phoneHeader,
                    posters = posters,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    canLoadMore = canLoadMore,
                    needsManualLoadMore = needsManualLoadMore,
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() },
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMorePosters() },
                    navController = navController
                )
            }
        }
}

@Composable
fun CountryLoadingScreen(header: @Composable () -> Unit = {}) {
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        item(key = "loading-title", span = { GridItemSpan(maxLineSpan) }) {
            Text("Loading Content...", style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold)
        }
        items(6) { CountryShimmerPosterItem() }
    }
}

@Composable
fun CountryShimmerPosterItem() { CompactPosterPlaceholder() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPosterGrid(
    posters: List<Poster>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    needsManualLoadMore: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    navController: NavController? = null,
    header: @Composable () -> Unit = {}
) {
    val postersList = posters.distinctBy { "${it.type}:${it.id}" }
    val context = LocalContext.current
    
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        itemsIndexed(postersList, key = { _, poster -> "${poster.type}:${poster.id}" }) { index, poster ->
            CountryPosterItem(
                poster = poster,
                onClick = {
                    if (poster.isMovie()) {
                        // Save movie to storage and navigate to single movie screen
                        StorageUtils.saveMovieToFile(context, poster.toMovie())
                        navController?.navigate("single_movie/${poster.id}")
                    } else if (poster.isSeries()) {
                        // Save series to storage and navigate to single series screen
                        StorageUtils.saveSeriesToFile(context, poster.toSeries())
                        navController?.navigate("single_series/${poster.id}")
                    }
                }
            )
            
            // Load more when we're near the end of the list
            if (canLoadMore && !needsManualLoadMore && index >= postersList.size - 3) {
                LaunchedEffect(Unit) { onLoadMore() }
            }
        }
        
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Modern animated loading indicator
                    CountryModernCircularProgressIndicator()
                }
            }
        }
        
        if (errorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CountryErrorItem(errorMessage = errorMessage, onRetry = onRetry)
            }
        }
        if (errorMessage == null && !isLoading && !isLoadingMore &&
            (postersList.isEmpty() || needsManualLoadMore)) {
            item(key = "country-next-batch", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (postersList.isEmpty()) Text(stringResource(R.string.no_results_found),
                        color = MaterialTheme.colorScheme.onSurface)
                    if (canLoadMore) Button(onClick = onLoadMore,
                        modifier = Modifier.tvFocusItem("country-next-batch").tvFocusBorder(CircleShape)) {
                        Text(stringResource(R.string.load_more_results))
                    }
                }
            }
        }

        // Add a small spacer at the bottom to avoid excessive padding
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    }
}

@Composable
fun CountryModernCircularProgressIndicator() {
    val transition = rememberInfiniteTransition(label = "progress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        ), label = "progress_anim"
    )
    
    // Add rotation animation for a more dynamic effect
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            )
        ), label = "rotation_anim"
    )
    
    CircularProgressIndicator(
        progress = progress,
        modifier = Modifier
            .size(48.dp)
            .rotate(rotation), // Add rotation
        strokeWidth = 4.dp,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun CountryPosterItem(poster: Poster, onClick: () -> Unit) {
    CompactPosterCard(
        title = poster.title, image = poster.image, imdb = poster.imdb,
        year = poster.year,
        typeLabel = androidx.compose.ui.res.stringResource(
            if (poster.isMovie()) com.dark.darknama.R.string.card_movie
            else com.dark.darknama.R.string.card_series),
        onClick = onClick, focusKey = "country-${poster.id}-${poster.type}"
    )
}

@Composable
fun CountryErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load content",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.tvFocusItem("country-retry").padding(top = 8.dp).tvFocusBorder(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(com.dark.darknama.R.string.retry))
        }
    }
}

@Composable
fun CountryErrorItem(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Failed to load more content",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Button(
                onClick = onRetry,
                modifier = Modifier.tvFocusItem("country-load-more-retry").tvFocusBorder(CircleShape),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onErrorContainer,
                    contentColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(com.dark.darknama.R.string.retry))
            }
        }
    }
}