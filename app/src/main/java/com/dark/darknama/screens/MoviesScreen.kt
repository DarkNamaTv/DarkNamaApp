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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.res.stringResource
import com.dark.darknama.R
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.ui.settings.UpdateViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.components.GenreFilterSection
import com.dark.darknama.components.CompactPosterCard
import com.dark.darknama.components.CompactPosterPlaceholder
import com.dark.darknama.components.copyTitleOnClick
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.TvRefreshButton
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Movie
import com.dark.darknama.ui.movies.MoviesViewModel
import com.dark.darknama.ui.home.HomeBannerViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

@Composable
fun MoviesScreen(
    viewModel: MoviesViewModel = viewModel(),
    navController: NavController? = null
) {
    val bannerViewModel: HomeBannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val movies = viewModel.movies
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val canLoadMore = viewModel.canLoadMore
    val needsManualLoadMore = viewModel.needsManualLoadMore
    val errorMessage = viewModel.errorMessage
    val genres = viewModel.genres
    val selectedGenreId = viewModel.selectedGenreId
    val selectedFilterType = viewModel.selectedFilterType
    val updates: UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = LocalContext.current as ComponentActivity)
    val updateState by updates.state.collectAsState()
    
    LaunchedEffect(Unit) {
        if (movies.isEmpty()) {
            viewModel.loadMovies()
        }
    }
    
    val context = LocalContext.current
    // TV: the section bar is pinned; banner, filters and cards share the grid.
    if (DeviceUtils.isTv(LocalContext.current)) {
        val sortFocus = remember { FocusRequester() }
        val headerFocus = remember { FocusRequester() }
        val displayMovies = movies.distinctBy { it.id }
        TvPosterGrid(
            screenKey = "movies", posters = displayMovies, key = { "movie:${it.id}" },
            headerDownFocus = sortFocus,
            pinnedHeader = {
                MainSectionHeader(logoFocusRequester = headerFocus,
                    title = stringResource(R.string.app_name),
                    navController = navController, actions = {
                        TvRefreshButton(onClick = { viewModel.refresh(); bannerViewModel.load() })
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        IconButton(onClick = { updates.checkForUpdates() },
                            interactionSource = interaction,
                            modifier = Modifier.tvFocusItem("movies-update")
                                .size(48.dp).settingsFocusBorder(focused, CircleShape)) {
                            if (updateState.checking) CircularProgressIndicator(Modifier.size(20.dp))
                            else Icon(Icons.Default.Download,
                                contentDescription = stringResource(R.string.check_updates),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    })
            },
            header = {
                HomeBanner(navController = navController, viewModel = bannerViewModel)
                GenreFilterSection(
                    genres = genres, selectedGenreId = selectedGenreId,
                    selectedFilterType = selectedFilterType,
                    onGenreSelected = viewModel::selectGenre,
                    onFilterTypeSelected = viewModel::selectFilterType,
                    sortFocusRequester = sortFocus,
                    headerFocusRequester = if (TV_PIN_HEADER) headerFocus else null)
            },
            card = { movie -> MovieItem(movie, onClick = {
                StorageUtils.saveMovieToFile(context, movie)
                navController?.navigate("single_movie/${movie.id}")
            }) },
            empty = {
                when {
                    isLoading -> ModernCircularProgressIndicator()
                    errorMessage != null -> ErrorScreen(errorMessage, onRetry = viewModel::retry)
                    else -> Text(stringResource(R.string.no_results_found),
                        color = MaterialTheme.colorScheme.onBackground)
                }
            },
            footer = {
                if (isLoadingMore) ModernCircularProgressIndicator()
                if (errorMessage != null && displayMovies.isNotEmpty())
                    ErrorItem(errorMessage, onRetry = viewModel::retry)
                if (errorMessage == null && !isLoading && !isLoadingMore &&
                    (displayMovies.isEmpty() || needsManualLoadMore) && canLoadMore) {
                    Button(onClick = viewModel::loadMoreMovies,
                        modifier = Modifier.tvFocusItem("movies-next-batch")
                            .tvFocusBorder(CircleShape)) {
                        Text(stringResource(R.string.load_more_results))
                    }
                }
            },
            shouldLoadMore = { index -> canLoadMore && !needsManualLoadMore &&
                !isLoading && !isLoadingMore && index == displayMovies.lastIndex },
            onLoadMore = viewModel::loadMoreMovies
        )
        return
    }

    val phoneHeader: @Composable () -> Unit = {
        Column {
        MainSectionHeader(title = stringResource(R.string.app_name), navController = navController,
            actions = {
                TvRefreshButton(onClick = { viewModel.refresh(); bannerViewModel.load() })
                val updateInteraction = remember { MutableInteractionSource() }
                val updateFocused by updateInteraction.collectIsFocusedAsState()
                IconButton(onClick = { updates.checkForUpdates() },
                    interactionSource = updateInteraction,
                    modifier = Modifier.tvFocusItem("movies-update")
                        .size(48.dp).settingsFocusBorder(updateFocused, androidx.compose.foundation.shape.CircleShape)) {
                    if (updateState.checking) CircularProgressIndicator(Modifier.size(20.dp))
                    else Icon(Icons.Default.Download,
                        contentDescription = stringResource(R.string.check_updates),
                        tint = MaterialTheme.colorScheme.primary)
                }
            })

        // First page of the catalogue and its companion series feed form the home slider.
        HomeBanner(navController = navController, viewModel = bannerViewModel)

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
            isLoading && movies.isEmpty() -> {
                // Show modern loading animation when initial movies are loading
                LoadingScreen(header = phoneHeader)
            }
            errorMessage != null && movies.isEmpty() -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    phoneHeader()
                ErrorScreen(
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() }
                )
                }
            }
            else -> {
                MovieGrid(
                    header = phoneHeader,
                    movies = movies,
                    isLoading = isLoading,
                    isLoadingMore = isLoadingMore,
                    canLoadMore = canLoadMore,
                    needsManualLoadMore = needsManualLoadMore,
                    errorMessage = errorMessage,
                    onRetry = { viewModel.retry() },
                    onRefresh = { viewModel.refresh(); bannerViewModel.load() },
                    onLoadMore = { viewModel.loadMoreMovies() },
                    navController = navController
                )
            }
        }
}

@Composable
fun LoadingScreen(header: @Composable () -> Unit = {}) {
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        item(key = "loading-title", span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.loading_movies), style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold)
        }
        items(6) { ShimmerMovieItem() }
    }
}

@Composable
fun ShimmerMovieItem() { CompactPosterPlaceholder() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieGrid(
    movies: List<Movie>,
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
    val moviesList = movies.distinctBy { it.id }
    val context = LocalContext.current
    
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    PullToRefreshBox(isRefreshing = isLoading, onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        itemsIndexed(moviesList, key = { _, movie -> movie.id }) { index, movie ->
            MovieItem(
                movie = movie,
                onClick = {
                    // Save movie to storage
                    StorageUtils.saveMovieToFile(context, movie)
                    // Navigate to single movie screen
                    navController?.navigate("single_movie/${movie.id}")
                }
            )
            
            // Load more when we're near the end of the list
            if (canLoadMore && !needsManualLoadMore && index >= moviesList.size - 3) {
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
                    ModernCircularProgressIndicator()
                }
            }
        }
        
        if (errorMessage != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorItem(errorMessage = errorMessage, onRetry = onRetry)
            }
        }
        // Five raw pages may all be promotions. Let the user fetch the next
        // bounded batch; never mistake that for the end of the catalogue.
        if (errorMessage == null && !isLoading && !isLoadingMore &&
            (moviesList.isEmpty() || needsManualLoadMore)) {
            item(key = "movies-next-batch", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    if (moviesList.isEmpty()) Text(stringResource(R.string.no_results_found),
                        color = MaterialTheme.colorScheme.onSurface)
                    if (canLoadMore) Button(onClick = onLoadMore,
                        modifier = Modifier.tvFocusItem("movies-next-batch").tvFocusBorder(CircleShape)) {
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
fun ModernCircularProgressIndicator() {
    val transition = rememberInfiniteTransition(label = "progress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
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
                easing = androidx.compose.animation.core.LinearEasing
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
fun MovieItem(movie: Movie, onClick: () -> Unit) {
    CompactPosterCard(
        title = movie.title, image = movie.image, imdb = movie.imdb,
        year = movie.year, typeLabel = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.card_movie),
        onClick = onClick, focusKey = "movie-${movie.id}"
    )
}

@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.failed_load_movies),
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
            modifier = Modifier.tvFocusItem("movies-retry").padding(top = 8.dp).tvFocusBorder(CircleShape)
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
fun ErrorItem(
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
                text = androidx.compose.ui.res.stringResource(com.dark.darknama.R.string.failed_load_more_movies),
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
                modifier = Modifier.tvFocusItem("movies-load-more-retry").tvFocusBorder(CircleShape),
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