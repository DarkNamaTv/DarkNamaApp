package com.dark.darknama.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.clipToBounds
import com.dark.darknama.components.LocalTvGridRow
import com.dark.darknama.components.LocalTvGridRowTracker
import com.dark.darknama.components.TvGridRowTracker
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.VideoPlayerActivity
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.TvRefreshButton
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.data.model.TvBrowseMode
import com.dark.darknama.data.model.favoriteKey
import com.dark.darknama.data.repository.IptvRepository
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.ui.tv.LiveTvViewModel
import com.dark.darknama.utils.DeviceUtils

/** A single TV scrolling grid for mode/category controls, search and channels. */
@Composable
internal fun TvLiveTvContent(
    viewModel: LiveTvViewModel, navController: NavController?, context: Context,
    onHelp: () -> Unit, onCountryPicker: () -> Unit, onCategoryPicker: () -> Unit
) {
    val state = rememberLazyGridState()
    val scrollSpace = rememberTvFocusScrollSpace()
    val channels = viewModel.channels.distinctBy { it.favoriteKey }
    val columns = DeviceUtils.getGridColumns(context)
    val headerFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val tracker = remember { TvGridRowTracker(scope) }
    val pinnedHeader: @Composable () -> Unit = {
                MainSectionHeader(title = stringResource(R.string.live_tv),
                    logoFocusRequester = headerFocus,
                    navController = navController, titleIcon = Icons.Default.LiveTv,
                    actions = {
                        TvRefreshButton(onClick = viewModel::retry)
                        IconButton(onClick = onHelp,
                            modifier = Modifier.tvFocusItem("tv-help").size(48.dp)
                                .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = stringResource(R.string.tv_vpn_help_title))
                        }
                    })
    }
    CompositionLocalProvider(LocalTvGridRowTracker provides tracker) {
    Column(Modifier.fillMaxSize()) {
    if (TV_PIN_HEADER) TvPinnedHeader { pinnedHeader() }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.weight(1f).fillMaxSize().clipToBounds(), state = state,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = scrollSpace.bottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!TV_PIN_HEADER) item(key = "tv-pinned-header", span = { GridItemSpan(maxLineSpan) }) { pinnedHeader() }
        item(key = "tv-controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.tvResetGridRowOnFocus()) {
                LazyRow(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "mode-favorites") {
                        FilterChip(selected = viewModel.browseMode == TvBrowseMode.FAVORITES,
                            onClick = viewModel::selectFavorites,
                            modifier = Modifier.focusProperties { if (TV_PIN_HEADER) up = headerFocus }
                                .tvFocusItem("tv-favorites")
                                .tvBringIntoViewOnFocus()
                                .tvFocusBorder(RoundedCornerShape(16.dp)),
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            label = { Text(stringResource(R.string.tv_favorites) +
                                " (${viewModel.favoriteChannels.size})") })
                    }
                    item(key = "mode-persian") {
                        FilterChip(selected = viewModel.browseMode == TvBrowseMode.PERSIAN,
                            onClick = viewModel::selectPersianDefault,
                            modifier = Modifier.focusProperties { if (TV_PIN_HEADER) up = headerFocus }
                                .tvFocusItem("tv-persian")
                                .tvBringIntoViewOnFocus()
                                .tvFocusBorder(RoundedCornerShape(16.dp)),
                            label = { Text("🇮🇷 " + stringResource(R.string.tv_persian)) })
                    }
                    item(key = "mode-country") {
                        FilterChip(selected = viewModel.browseMode == TvBrowseMode.COUNTRY,
                            onClick = onCountryPicker,
                            modifier = Modifier.focusProperties { if (TV_PIN_HEADER) up = headerFocus }
                                .tvFocusItem("tv-country")
                                .tvBringIntoViewOnFocus()
                                .tvFocusBorder(RoundedCornerShape(16.dp)),
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null) },
                            label = {
                                val selected = viewModel.selectedGroup
                                    .takeIf { viewModel.browseMode == TvBrowseMode.COUNTRY }
                                Text(if (selected == null) stringResource(R.string.tv_country)
                                    else IptvRepository.flagFor(selected) + " " + selected)
                            })
                    }
                    item(key = "mode-category") {
                        FilterChip(selected = viewModel.browseMode == TvBrowseMode.CATEGORY,
                            onClick = onCategoryPicker,
                            modifier = Modifier.focusProperties { if (TV_PIN_HEADER) up = headerFocus }
                                .tvFocusItem("tv-category")
                                .tvBringIntoViewOnFocus()
                                .tvFocusBorder(RoundedCornerShape(16.dp)),
                            leadingIcon = { Icon(viewModel.selectedGroup?.let(::tvCategoryIcon)
                                ?: Icons.Default.Category, contentDescription = null) },
                            label = { Text(viewModel.selectedGroup
                                .takeIf { viewModel.browseMode == TvBrowseMode.CATEGORY }
                                ?: stringResource(R.string.tv_category)) })
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    TvSearchField(query = viewModel.searchQuery,
                        placeholder = stringResource(R.string.tv_search_hint),
                        modifier = Modifier.weight(1f),
                        onQueryChange = viewModel::updateSearchQuery, onSearch = {})
                    IconButton(onClick = { viewModel.updateSearchQuery("") },
                        modifier = Modifier.tvFocusItem("tv-search-clear")
                            .size(48.dp).tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                        Icon(Icons.Default.Clear,
                            contentDescription = stringResource(R.string.search_clear))
                    }
                }
                if (viewModel.browseMode == TvBrowseMode.PERSIAN &&
                    viewModel.persianCategories.isNotEmpty()) {
                    LazyRow(Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "category-all") {
                            FilterChip(selected = viewModel.selectedPersianCategory == null,
                                onClick = { viewModel.selectPersianCategory(null) },
                                modifier = Modifier.tvFocusItem("tv-all-categories")
                                    .tvBringIntoViewOnFocus()
                                    .tvFocusBorder(RoundedCornerShape(16.dp)),
                                label = { Text(stringResource(R.string.tv_all)) })
                        }
                        items(viewModel.persianCategories.distinct(), key = { it }) { category ->
                            FilterChip(selected = viewModel.selectedPersianCategory == category,
                                onClick = { viewModel.selectPersianCategory(category) },
                                modifier = Modifier.tvFocusItem("tv-category-$category")
                                    .tvBringIntoViewOnFocus()
                                    .tvFocusBorder(RoundedCornerShape(16.dp)),
                                leadingIcon = { Icon(tvCategoryIcon(category),
                                    contentDescription = null) },
                                label = { Text(category) })
                        }
                    }
                }
            }
        }
        when {
            viewModel.isLoading -> item(key = "tv-loading", span = { GridItemSpan(maxLineSpan) }) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            viewModel.errorMessage != null -> item(key = "tv-error",
                span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.tv_error),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground)
                    Text(viewModel.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = viewModel::retry,
                        modifier = Modifier.tvFocusItem("tv-retry")
                            .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(stringResource(R.string.tv_retry))
                    }
                }
            }
            channels.isEmpty() -> item(key = "tv-empty", span = { GridItemSpan(maxLineSpan) }) {
                TextButton(onClick = viewModel::selectPersianDefault,
                    modifier = Modifier.tvFocusItem("tv-empty-browse")
                        .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                    Text(stringResource(if (viewModel.browseMode == TvBrowseMode.FAVORITES)
                        R.string.tv_no_favorites else R.string.tv_no_channels))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.tv_persian))
                }
            }
            else -> itemsIndexed(channels, key = { _, channel -> channel.favoriteKey }) { index, channel ->
                CompositionLocalProvider(LocalTvGridRow provides index / columns) {
                Box(Modifier.tvPinFocusedRow(state, scrollSpace, index, columns,
                    headerItems = if (TV_PIN_HEADER) 1 else 2)) {
                    ChannelCard(channel = channel,
                        showCountryFlag = viewModel.browseMode == TvBrowseMode.COUNTRY,
                        isFavorite = viewModel.favoriteKeys.contains(channel.favoriteKey),
                        onToggleFavorite = { viewModel.toggleFavorite(channel) },
                        onClick = { VideoPlayerActivity.startLiveTv(context, channel.url,
                            channel.userAgent, channel.referer) })
                }
                }
            }
        }
    }
    }
    }
}
