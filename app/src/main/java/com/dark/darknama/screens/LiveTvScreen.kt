@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dark.darknama.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.VideoPlayerActivity
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.TvRefreshButton
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.goldFocus
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.data.model.TvBrowseMode
import com.dark.darknama.data.model.TvChannel
import com.dark.darknama.data.model.favoriteKey
import com.dark.darknama.data.repository.IptvRepository
import com.dark.darknama.ui.tv.LiveTvViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem

/**
 * Live TV screen.
 *
 * - Default playlist: Persian channels (iptv-org languages/fas.m3u)
 * - Country picker (with flags) -> index.country.m3u
 * - Category picker -> index.m3u
 * - Fully usable with an Android TV remote (D-pad focus everywhere).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    viewModel: LiveTvViewModel = viewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val isTv = remember { DeviceUtils.isTv(context) }

    var showCountryPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showVpnHelpDialog by remember { mutableStateOf(false) }

    if (isTv) {
        TvLiveTvContent(viewModel, navController, context,
            onHelp = { showVpnHelpDialog = true },
            onCountryPicker = { viewModel.ensureCountryListLoaded(); showCountryPicker = true },
            onCategoryPicker = { viewModel.ensureCategoryListLoaded(); showCategoryPicker = true })
    } else {
        val phoneHeader: @Composable () -> Unit = {
        Column {
        MainSectionHeader(title = stringResource(R.string.live_tv),
            navController = navController, titleIcon = Icons.Default.LiveTv,
            actions = {
                TvRefreshButton(onClick = viewModel::retry)
                // Keep the VPN help action accessible with the remote.
                val helpInteractionSource = remember { MutableInteractionSource() }
                val isHelpFocused by helpInteractionSource.collectIsFocusedAsState()
                IconButton(
                    onClick = { showVpnHelpDialog = true },
                    interactionSource = helpInteractionSource,
                    modifier = Modifier.tvFocusItem("tv-help")
                        .size(if (isTv) 48.dp else 40.dp)
                        .settingsFocusBorder(isHelpFocused, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = stringResource(R.string.tv_vpn_help_title),
                        tint = if (isHelpFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp))
                }
            })

        // ---------- Browse mode selector ----------
        // Horizontally scrollable so the extra "Favorites" chip always fits,
        // including on Android TV where the sidebar takes horizontal space.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            // Favorites (starred channels)
            FilterChip(
                modifier = Modifier.tvFocusItem("tv-favorites").tvFocusBorder(RoundedCornerShape(16.dp)),
                selected = viewModel.browseMode == TvBrowseMode.FAVORITES,
                onClick = { viewModel.selectFavorites() },
                leadingIcon = {
                    Icon(
                        imageVector = if (viewModel.browseMode == TvBrowseMode.FAVORITES)
                            Icons.Default.Star
                        else
                            Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.tv_favorites) +
                            if (viewModel.favoriteChannels.isNotEmpty())
                                " (${viewModel.favoriteChannels.size})"
                            else "",
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
          }
          item {
            // Persian (default)
            FilterChip(
                modifier = Modifier.tvFocusItem("tv-persian").tvFocusBorder(RoundedCornerShape(16.dp)),
                selected = viewModel.browseMode == TvBrowseMode.PERSIAN,
                onClick = { viewModel.selectPersianDefault() },
                label = {
                    Text(
                        text = "\uD83C\uDDEE\uD83C\uDDF7 " + stringResource(R.string.tv_persian),
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
          }
          item {
            // Country picker
            FilterChip(
                modifier = Modifier.tvFocusItem("tv-country").tvFocusBorder(RoundedCornerShape(16.dp)),
                selected = viewModel.browseMode == TvBrowseMode.COUNTRY,
                onClick = {
                    viewModel.ensureCountryListLoaded()
                    showCountryPicker = true
                },
                leadingIcon = {
                    if (viewModel.browseMode != TvBrowseMode.COUNTRY) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                label = {
                    val label = if (viewModel.browseMode == TvBrowseMode.COUNTRY && viewModel.selectedGroup != null) {
                        IptvRepository.flagFor(viewModel.selectedGroup!!) + " " + viewModel.selectedGroup!!
                    } else {
                        stringResource(R.string.tv_country)
                    }
                    Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
          }
          item {
            // Category picker
            FilterChip(
                modifier = Modifier.tvFocusItem("tv-category").tvFocusBorder(RoundedCornerShape(16.dp)),
                selected = viewModel.browseMode == TvBrowseMode.CATEGORY,
                onClick = {
                    viewModel.ensureCategoryListLoaded()
                    showCategoryPicker = true
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (viewModel.browseMode == TvBrowseMode.CATEGORY)
                            viewModel.selectedGroup?.let(::tvCategoryIcon) ?: Icons.Default.Category
                        else Icons.Default.Category,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    val label = if (viewModel.browseMode == TvBrowseMode.CATEGORY && viewModel.selectedGroup != null) {
                        viewModel.selectedGroup!!
                    } else {
                        stringResource(R.string.tv_category)
                    }
                    Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Search ----------
        if (isTv) {
            // Use the native field so the Leanback keyboard retains D-pad input.
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TvSearchField(
                    query = viewModel.searchQuery,
                    placeholder = stringResource(R.string.tv_search_hint),
                    modifier = Modifier.weight(1f),
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = {} // Channel filtering happens as the user types.
                )
                // Stay in the focus tree after clearing; a disappearing focused
                // button would otherwise strand remote users on the search row.
                IconButton(onClick = { viewModel.updateSearchQuery("") },
                    modifier = Modifier.tvFocusItem("tv-search-clear")
                        .size(48.dp).tvFocusBorder(CircleShape)) {
                    Icon(Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (viewModel.searchQuery.isEmpty()) 0.4f else 1f))
                }
            }
        } else {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .tvFocusBorder(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent),
                placeholder = {
                    Text(stringResource(R.string.tv_search_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier.tvFocusBorder(CircleShape)) {
                            Icon(Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Persian category chips (default mode only) ----------
        if (viewModel.browseMode == TvBrowseMode.PERSIAN && viewModel.persianCategories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        modifier = Modifier.tvFocusItem("tv-all-categories").tvFocusBorder(RoundedCornerShape(16.dp)),
                        selected = viewModel.selectedPersianCategory == null,
                        onClick = { viewModel.selectPersianCategory(null) },
                        leadingIcon = {
                            Icon(tvCategoryIcon(stringResource(R.string.tv_all)),
                                contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        label = { Text(stringResource(R.string.tv_all)) }
                    )
                }
                items(viewModel.persianCategories.distinct(), key = { it }) { category ->
                    FilterChip(
                        modifier = Modifier.tvFocusItem("tv-category-$category")
                            .tvFocusBorder(RoundedCornerShape(16.dp)),
                        selected = viewModel.selectedPersianCategory == category,
                        onClick = { viewModel.selectPersianCategory(category) },
                        leadingIcon = {
                            Icon(tvCategoryIcon(category), contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                        label = { Text(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        }
        }
        val columns = DeviceUtils.getGridColumns(context).coerceAtLeast(3)
        LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { phoneHeader() }
        when {
            viewModel.isLoading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.tv_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            viewModel.errorMessage != null -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.tv_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry() },
                            modifier = Modifier.tvFocusItem("tv-retry").tvFocusBorder(CircleShape)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.tv_retry))
                        }
                    }
                }
            }
            viewModel.channels.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.browseMode == TvBrowseMode.FAVORITES) {
                        // Dedicated empty state explaining how to add favorites
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.tv_no_favorites),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.tv_no_favorites_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.tv_no_channels),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                    items(viewModel.channels.distinctBy { it.favoriteKey },
                        key = { it.favoriteKey }) { channel ->
                        ChannelCard(
                            channel = channel,
                            showCountryFlag = viewModel.browseMode == TvBrowseMode.COUNTRY,
                            isFavorite = viewModel.favoriteKeys.contains(channel.favoriteKey),
                            onToggleFavorite = { viewModel.toggleFavorite(channel) },
                            onClick = {
                                VideoPlayerActivity.startLiveTv(
                                    context = context,
                                    videoUrl = channel.url,
                                    userAgent = channel.userAgent,
                                    referer = channel.referer
                                )
                            }
                        )
                    }
            }
        }
        }
    }

    // ---------- VPN help dialog ----------
    if (showVpnHelpDialog) {
        AlertDialog(
            onDismissRequest = { showVpnHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.tv_vpn_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.tv_vpn_help_message),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(onClick = { showVpnHelpDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // ---------- Country picker dialog ----------
    if (showCountryPicker) {
        PickerDialog(
            title = stringResource(R.string.tv_select_country),
            items = viewModel.countryList,
            isLoading = viewModel.isCountryListLoading,
            selectedItem = if (viewModel.browseMode == TvBrowseMode.COUNTRY) viewModel.selectedGroup else null,
            itemLabel = { country -> IptvRepository.flagFor(country) + "  " + country },
            onItemSelected = { country ->
                showCountryPicker = false
                viewModel.selectCountry(country)
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    // ---------- Category picker dialog ----------
    if (showCategoryPicker) {
        PickerDialog(
            title = stringResource(R.string.tv_select_category),
            items = viewModel.categoryList,
            isLoading = viewModel.isCategoryListLoading,
            selectedItem = if (viewModel.browseMode == TvBrowseMode.CATEGORY) viewModel.selectedGroup else null,
            itemLabel = { it },
            itemIcon = ::tvCategoryIcon,
            onItemSelected = { category ->
                showCategoryPicker = false
                viewModel.selectCategory(category)
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

/**
 * A single channel card: logo + name (+ country flag / group).
 * Uses Material3 Card(onClick) so it is focusable and clickable with a TV remote.
 *
 * A star button is drawn on top of the logo:
 *  - hollow star  -> channel is NOT a favorite (tap to add)
 *  - filled star  -> channel IS a favorite (tap to remove)
 */
@Composable
internal fun ChannelCard(
    channel: TvChannel,
    showCountryFlag: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val starInteractionSource = remember { MutableInteractionSource() }
    val isStarFocused by starInteractionSource.collectIsFocusedAsState()
    val isTv = DeviceUtils.isTv(LocalContext.current)

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .tvFocusItem("tv-channel-${channel.favoriteKey}")
            .fillMaxWidth().tvBringIntoViewOnFocus(margin = 4.dp, horizontalMargin = 0.dp)
            .goldFocus(isFocused, RoundedCornerShape(12.dp), thin = isTv),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Channel logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logo.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = channel.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit,
                        error = androidx.compose.ui.res.painterResource(id = R.drawable.splash_logo)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = channel.name,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // ---- Favorite (star) toggle, drawn above the logo ----
                IconButton(
                    onClick = onToggleFavorite,
                    interactionSource = starInteractionSource,
                    modifier = Modifier
                        .tvFocusItem("tv-star-${channel.favoriteKey}")
                        .align(Alignment.TopEnd)
                        .size(if (isTv) 48.dp else 30.dp)
                        .settingsFocusBorder(isStarFocused, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.tv_remove_favorite else R.string.tv_add_favorite
                        ),
                        tint = if (isFavorite)
                            Color(0xFFFFC107) // amber for a starred channel
                        else
                            Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Channel name
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            // Group / flag line
            if (channel.group.isNotEmpty()) {
                val groupText = if (showCountryFlag) {
                    IptvRepository.flagFor(channel.group) + " " + channel.group
                } else {
                    channel.group.split(';').firstOrNull()?.trim() ?: channel.group
                }
                Text(
                    text = groupText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reusable single-choice picker dialog with a scrollable list.
 * Rows are focusable for TV remote navigation.
 */
@Composable
private fun PickerDialog(
    title: String,
    items: List<String>,
    isLoading: Boolean,
    selectedItem: String?,
    itemLabel: (String) -> String,
    itemIcon: ((String) -> ImageVector)? = null,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    var focusFirstOption by remember { mutableStateOf(true) }
    val isTv = DeviceUtils.isTv(LocalContext.current)
    val filteredItems = remember(items, filter) {
        // A feed may repeat a category/country; the lazy keys must stay unique.
        val options = items.distinct()
        if (filter.isBlank()) options
        else options.filter { it.contains(filter.trim(), ignoreCase = true) }
    }
    val selectedIndex = filteredItems.indexOf(selectedItem).takeIf { it >= 0 } ?: 0
    val listState = rememberLazyListState()
    LaunchedEffect(filteredItems, selectedIndex, isLoading, isTv) {
        if (isTv && !isLoading && filteredItems.isNotEmpty()) {
            listState.scrollToItem(if (filter.isBlank()) selectedIndex else 0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTv) {
                    TvSearchField(
                        query = filter,
                        placeholder = stringResource(R.string.tv_filter_hint),
                        onQueryChange = { filter = it; focusFirstOption = false },
                        onSearch = {} // List already filters as the user types.
                    )
                } else {
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        modifier = Modifier.fillMaxWidth()
                            .tvFocusBorder(RoundedCornerShape(10.dp)),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent),
                        placeholder = { Text(stringResource(R.string.tv_filter_hint)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    filteredItems.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.tv_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                        ) {
                            itemsIndexed(filteredItems, key = { _, item -> item }) { index, item ->
                                PickerRow(
                                    label = itemLabel(item),
                                    icon = itemIcon?.invoke(item),
                                    isSelected = item == selectedItem,
                                    modifier = if (focusFirstOption && index == selectedIndex)
                                        Modifier.tvDialogInitialFocus() else Modifier,
                                    onClick = { onItemSelected(item) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss,
                modifier = (if (filter.isBlank() && (isLoading || filteredItems.isEmpty()))
                    Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(CircleShape)) {
                Text(stringResource(R.string.tv_cancel))
            }
        }
    )
}

@Composable
private fun PickerRow(
    label: String,
    icon: ImageVector?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .settingsFocusBorder(isFocused, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                isFocused -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else if (isFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                // Accent-tinted selection is still mostly the underlying surface.
                // Use its light/dark onSurface text, with the accent dot as the cue.
                color = if (isFocused && !isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
