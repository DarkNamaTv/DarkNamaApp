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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dark.darknama.components.LocalTvGridRow
import com.dark.darknama.components.LocalTvGridRowTracker
import com.dark.darknama.components.TvGridRowTracker
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.SettingsIconButton
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.ui.search.SearchViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

/** TV search header/stories/results use one keyed grid, not a fixed header above it. */
@Composable
internal fun TvSearchResultsContent(viewModel: SearchViewModel,
    navController: NavController?, context: Context
) {
    val state = rememberLazyGridState()
    val scrollSpace = rememberTvFocusScrollSpace()
    val submitFocus = remember { FocusRequester() }
    val posters = viewModel.searchResults.distinctBy { "${it.type}:${it.id}" }
    val columns = DeviceUtils.getGridColumns(context)
    val scope = rememberCoroutineScope()
    val tracker = remember { TvGridRowTracker(scope) }
    CompositionLocalProvider(LocalTvGridRowTracker provides tracker) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = scrollSpace.bottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "search-controls", span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.tvResetGridRowOnFocus()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.search),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    SettingsIconButton(navController = navController,
                        modifier = Modifier.tvFocusItem("search-settings", initial = true))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TvSearchField(
                        query = viewModel.searchQuery,
                        placeholder = stringResource(R.string.search_placeholder),
                        modifier = Modifier.weight(1f),
                        onQueryChange = viewModel::updateSearchQuery,
                        onSearch = { if (viewModel.searchQuery.isNotBlank()) viewModel.triggerSearch() }
                    )
                    IconButton(onClick = {
                        if (viewModel.searchQuery.isNotBlank()) viewModel.triggerSearch()
                    }, modifier = Modifier.tvFocusItem("search-submit")
                        .focusRequester(submitFocus).size(48.dp)
                        .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            submitFocus.requestFocus()
                            viewModel.clearSearch()
                        }, modifier = Modifier.tvFocusItem("search-clear")
                            .size(48.dp).tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                            Icon(Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                }
                if (!viewModel.hasSearched) {
                    if (viewModel.isCountriesLoading) {
                        CircularProgressIndicator(Modifier.padding(vertical = 16.dp).size(24.dp))
                    } else if (viewModel.countries.isNotEmpty()) {
                        LazyRow(Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(viewModel.countries, key = { it.id }) { country ->
                                CountryStoryItem(country,
                                    onClick = { navController?.navigate("country/${country.id}") })
                            }
                        }
                    }
                }
            }
        }
        when {
            viewModel.isLoading -> item(key = "search-loading",
                span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(48.dp))
                }
            }
            viewModel.errorMessage != null -> item(key = "search-error",
                span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${viewModel.errorMessage}",
                        color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::triggerSearch,
                        modifier = Modifier.tvFocusItem("search-retry")
                            .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            posters.isNotEmpty() -> itemsIndexed(posters,
                key = { _, poster -> "${poster.type}:${poster.id}" }) { index, poster ->
                CompositionLocalProvider(LocalTvGridRow provides index / columns) {
                Box(Modifier.tvPinFocusedRow(state, scrollSpace, index, columns, headerItems = 1)) {
                    PosterItem(poster, onClick = {
                        if (poster.isMovie()) {
                            StorageUtils.saveMovieToFile(context, poster.toMovie())
                            navController?.navigate("single_movie/${poster.id}")
                        } else if (poster.isSeries()) {
                            StorageUtils.saveSeriesToFile(context, poster.toSeries())
                            navController?.navigate("single_series/${poster.id}")
                        }
                    })
                }
                }
            }
            else -> item(key = "search-empty", span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(if (viewModel.hasSearched && viewModel.searchQuery.isNotEmpty())
                    R.string.no_results_found else R.string.search_hint_text),
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    }
}
