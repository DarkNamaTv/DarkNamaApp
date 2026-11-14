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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.TvRefreshButton
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Series
import com.dark.darknama.ui.series.SeriesViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel = viewModel(),
    navController: NavController? = null
) {
    val series = viewModel.series
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val errorMessage = viewModel.errorMessage
    val genres = viewModel.genres
    val selectedGenreId = viewModel.selectedGenreId
    val selectedFilterType = viewModel.selectedFilterType
    
    LaunchedEffect(Unit) {
        if (series.isEmpty()) {
            viewModel.loadSeries()
        }
    }
    
    // TV: pin the section bar while the filters and cards scroll together.
    if (DeviceUtils.isTv(LocalContext.current)) {
        val context = LocalContext.current
        val sortFocus = remember { FocusRequester() }
        val headerFocus = remember { FocusRequester() }
        val displaySeries = series.distinctBy { it.id }
        TvPosterGrid(
            screenKey = "series", posters = displaySeries, key = { "series:${it.id}" },
            headerDownFocus = sortFocus,
            pinnedHeader = {
                MainSectionHeader(logoFocusRequester = headerFocus,
                    title = stringResource(com.dark.darknama.R.string.app_name),
                    navController = navController,
                    actions = { TvRefreshButton(onClick = viewModel::refresh) })
            },
            header = {
                GenreFilterSection(
                    genres = genres, selectedGenreId = selectedGenreId,
                    selectedFilterType = selectedFilterType,
                    onGenreSelected = viewModel::selectGenre,
                    onFilterTypeSelected = viewModel::selectFilterType,
                    sortFocusRequester = sortFocus,
                    headerFocusRequester = if (TV_PIN_HEADER) headerFocus else null)
            },
            card = { entry -> SeriesItem(entry, onClick = {
                StorageUtils.saveSeriesToFile(context, entry)
                navController?.navigate("single_series/${entry.id}")
            }) },
            empty = {
                when {
                    isLoading -> ModernCircularProgressIndicatorSeries()
                    errorMessage != null -> ErrorScreenSeries(errorMessage, onRetry = viewModel::retry)
                    else -> Text(stringResource(R.string.no_results_found),
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            footer = {
                if (isLoadingMore) ModernCircularProgressIndicatorSeries()
                if (errorMessage != null && displaySeries.isNotEmpty())
                    ErrorItemSeries(errorMessage, onRetry = viewModel::retry)
            },
            shouldLoadMore = { index -> !isLoading && !isLoadingMore &&
                index == displaySeries.lastIndex },
            onLoadMore = viewModel::loadMoreSeries
        )
        return
    }

    val phoneHeader: @Composable () -> Unit = {
        Column {
        MainSectionHeader(title = stringResource(com.dark.darknama.R.string.app_name),
            navController = navController, actions = {
                TvRefreshButton(onClick = viewModel::refresh)
            })

        // Genre filter section
        GenreFilterSection(
            genres = genres,
            selectedGenreId = selectedGenreId,
            selectedFilterType = selectedFilterType,
            onGenreSelected = { genreId -> viewModel.selectGenre(genreId) },
            onFilterTypeSelected = { filterType -> viewModel.selectFilterType(filterType) }
        )
        
        }
    }
    when {
            isLoading && series.isEmpty() -> {
                // Show modern loading animation when initial series are loading
                LoadingScreenSeries(header = phoneHeader)
            }
            errorMessage != null && series.isEmpty() -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    phoneHeader()
                ErrorScreenSeries(
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() }
                )
                }
            }
            else -> {
                SeriesGrid(
                    header = phoneHeader,
                    series = series,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() },
                    onRefresh = { viewModel.refresh() },
                    onLoadMore = { viewModel.loadMoreSeries() },
                    navController = navController
                )
            }
        }
}

@Composable
fun LoadingScreenSeries(header: @Composable () -> Unit = {}) {
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        item(key = "loading-title", span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.loading_series), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold)
        }
        items(6) { ShimmerSeriesItem() }
    }
}

@Composable
fun ShimmerSeriesItem() { CompactPosterPlaceholder() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesGrid(
    series: List<Series>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    navController: NavController? = null,
    header: @Composable () -> Unit = {}
) {
    val seriesList = series.distinctBy { it.id }
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
        itemsIndexed(seriesList, key = { _, seriesItem -> seriesItem.id }) { index, seriesItem ->
            SeriesItem(
                series = seriesItem,
                onClick = {
                    // Save series to storage
                    StorageUtils.saveSeriesToFile(context, seriesItem)
                    // Navigate to single series screen
                    navController?.navigate("single_series/${seriesItem.id}")
                }
            )
            
            // Load more when we're near the end of the list
            if (index >= seriesList.size - 3) {
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
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
                    ModernCircularProgressIndicatorSeries()
                }
            }
        }
        
        if (errorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorItemSeries(
                    errorMessage = errorMessage,
                    onRetry = onRetry
                )
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
fun ModernCircularProgressIndicatorSeries() {
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
fun SeriesItem(series: Series, onClick: () -> Unit) {
    CompactPosterCard(
        title = series.title, image = series.image, imdb = series.imdb,
        year = series.year, typeLabel = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.card_series),
        onClick = onClick, focusKey = "series-${series.id}"
    )
}

@Composable
fun ErrorScreenSeries(
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
            text = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.failed_load_series),
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
            modifier = Modifier.tvFocusItem("series-retry").padding(top = 8.dp).tvFocusBorder(CircleShape)
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
fun ErrorItemSeries(
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
                text = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.failed_load_more_series),
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
                modifier = Modifier.tvFocusItem("series-load-more-retry").tvFocusBorder(CircleShape),
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