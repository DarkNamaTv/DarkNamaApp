package com.dark.darknama.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.dark.darknama.components.copyTitleOnClick
import com.dark.darknama.components.ExpandableText
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.data.model.FavoriteItem
import com.dark.darknama.data.model.MediaSourceSelection
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.Source
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.DownloadUtils
import com.dark.darknama.utils.StorageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleMovieScreen(
    movieId: Int,
    navController: NavController
) {
    var movie by remember(movieId) { mutableStateOf<Movie?>(null) }
    var isLoading by remember(movieId) { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(movieId) {
        movie = StorageUtils.loadMovieFromFile(context, movieId)
        isLoading = false
    }
    // The loading back button is replaced with a new one when the file arrives.
    // Re-attach the remembered focus after that replacement on TV.
    val focusRegistry = LocalTvFocusRegistry.current
    val focusEntry = LocalTvFocusEntry.current
    LaunchedEffect(movie?.id, focusEntry) {
        if (movie != null && DeviceUtils.isTv(context)) {
            withFrameNanos { }
            focusRegistry?.restore(focusEntry)
        }
    }

    // Directly render content without Scaffold since it's already in MainScreen's Scaffold
    if (movie != null) {
        MovieDetailsContent(
            movie = movie!!,
            onBackClick = { navController.popBackStack() },
            onPlayClick = { source ->
                // Launch video player activity
                VideoPlayerActivity.start(
                    context, source.url, source.type,
                    movieId = movie!!.id, title = movie!!.title, image = movie!!.image,
                    quality = source.quality
                )
            },
            // Remove any padding from parent Scaffold to use full screen
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
                    modifier = Modifier.tvFocusItem("movie-back", initial = true)
                        .tvFocusBorder(CircleShape)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Text(
                    text = stringResource(if (isLoading) R.string.loading_movies else R.string.movie_not_found),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
fun MovieDetailsContent(
    movie: Movie,
    onBackClick: () -> Unit,
    onPlayClick: (Source) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSource by remember(movie.id) { mutableStateOf<Source?>(null) }
    var showSourceDialog by remember(movie.id) { mutableStateOf(false) }
    var downloadSource by remember(movie.id) { mutableStateOf<Source?>(null) }
    var downloadSources by remember(movie.id) { mutableStateOf<List<Source>>(emptyList()) }

    if (showSourceDialog && selectedSource != null) {
        SourceOptionsDialog(
            source = selectedSource!!,
            onDismiss = { showSourceDialog = false },
            onPlay = { source ->
                showSourceDialog = false
                onPlayClick(source)
            }
        )
    }
    downloadSource?.let { source ->
        SourceDownloadOptions(source) {
            downloadSource = null
        }
    }
    if (downloadSources.isNotEmpty()) {
        DownloadMenu(sources = downloadSources,
            onDismiss = { downloadSources = emptyList() })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Movie header with background cover and foreground image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Background cover image (blurred)
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(movie.cover)
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
            
            // Row to contain poster and movie details side by side
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Foreground movie poster
                var showImageDialog by remember { mutableStateOf(false) }
                
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current)
                            .data(movie.image)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = movie.title,
                    modifier = Modifier
                        .height(200.dp)
                        .tvFocusItem("movie-poster")
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
                                    DownloadUtils.copyToClipboard(context, movie.image)
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
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Movie details to the right of the poster
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    // Movie title
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.tvFocusItem("movie-copy-title")
                            .copyTitleOnClick(movie.title)
                    )
                    
                    // Country and year
                    val countryText = if (movie.country.isNotEmpty()) {
                        "${movie.country.joinToString(", ") { it.title }} (${movie.year})"
                    } else {
                        "(${movie.year})"
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
                            text = String.format("%.1f", movie.imdb),
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
                    .tvFocusItem("movie-back", initial = true)
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
            val movieId = movie.id
            
            // Check if movie is already favorite
            LaunchedEffect(movieId) {
                isFavorite = StorageUtils.isFavorite(context, movieId, "movie")
            }
            
            var showRemoveFavoriteDialog by remember { mutableStateOf(false) }
            
            // Confirmation dialog for removing from favorites
            if (showRemoveFavoriteDialog) {
                AlertDialog(
                    onDismissRequest = { showRemoveFavoriteDialog = false },
                    title = { Text("Remove from Favorites") },
                    text = { Text("Are you sure you want to remove this movie from your favorites?") },
                    confirmButton = {
                        TextButton(
                            modifier = Modifier.tvFocusBorder(CircleShape),
                            onClick = {
                                StorageUtils.removeFavorite(context, movieId, "movie")
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
                            Text("Cancel")
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
                        // Convert movie to favorite item with sources
                        val favoriteItem = FavoriteItem(
                            id = movie.id,
                            type = "movie",
                            title = movie.title,
                            description = movie.description,
                            year = movie.year,
                            imdb = movie.imdb,
                            rating = movie.rating,
                            duration = movie.duration,
                            image = movie.image,
                            cover = movie.cover,
                            genres = movie.genres,
                            country = movie.country,
                            sources = movie.sources // Include sources in favorites
                        )
                        StorageUtils.saveFavorite(context, favoriteItem)
                        isFavorite = true
                        // Show toast
                        android.widget.Toast.makeText(context, "Added to favorites", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.tvFocusItem("movie-favorite")
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
        
        // Genres
        if (movie.genres.isNotEmpty()) {
            Text(
                text = "Genres",
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
                items(movie.genres) { genre ->
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
        
        // Description
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Set layout direction to RTL for the description text
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ExpandableText(
                text = movie.description,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            )
        }
        MovieLinksSection(
            movie = movie,
            onSourceClick = { source ->
                selectedSource = source
                showSourceDialog = true
            },
            onDownloadSources = { sources ->
                if (sources.size == 1) downloadSource = sources.first()
                else downloadSources = sources
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SourceOptionsDialog(
    source: Source,
    onDismiss: () -> Unit,
    onPlay: (Source) -> Unit
) {
    var showDownloadOptions by remember(source.url) { mutableStateOf(false) }
    val context = LocalContext.current
    if (showDownloadOptions) {
        SourceDownloadOptions(source, onDismiss)
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = source.quality.trim().takeIf { MediaSourceSelection.hasNamedQuality(it) }
                    ?: stringResource(R.string.movie_direct_link_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(stringResource(R.string.movie_quality_action_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showDownloadOptions = true },
                    modifier = Modifier.tvDialogInitialFocus().fillMaxWidth().tvFocusBorder(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = androidx.compose.material3.ButtonDefaults.elevatedButtonElevation()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = stringResource(R.string.download),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.movie_download_options))
                }
                
                Button(
                    onClick = { onPlay(source) },
                    modifier = Modifier.fillMaxWidth().tvFocusBorder(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = androidx.compose.material3.ButtonDefaults.elevatedButtonElevation()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.movie_play_in_app),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.movie_play_in_app))
                }

                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                        context.getString(R.string.movie_links_clipboard), source.url.trim()))
                    Toast.makeText(context, context.resources.getQuantityString(
                        R.plurals.movie_links_copied, 1, 1), Toast.LENGTH_SHORT).show()
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth().tvFocusBorder(CircleShape)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.download_options_copy_link))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().tvFocusBorder(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp
    )
}


fun openUrl(context: Context, url: String) {
    DownloadUtils.openUrl(context, url)
}

