package com.dark.darknama.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import kotlinx.coroutines.launch
import com.dark.darknama.components.LocalTvGridRow
import com.dark.darknama.components.LocalTvGridRowTracker
import com.dark.darknama.components.TvGridRowTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dark.darknama.utils.DeviceUtils

/** TV viewport: the optional section bar is separate from scrolling controls and cards. */
@Composable
internal fun <T : Any> TvPosterGrid(
    screenKey: String,
    posters: List<T>,
    key: (T) -> String,
    header: @Composable () -> Unit,
    pinnedHeader: @Composable () -> Unit = {},
    hasPinnedHeader: Boolean = true,
    headerDownFocus: FocusRequester? = null,
    card: @Composable (T) -> Unit,
    empty: @Composable () -> Unit,
    footer: @Composable () -> Unit = {},
    shouldLoadMore: (Int) -> Boolean = { false },
    onLoadMore: () -> Unit = {}
) {
    val state = rememberLazyGridState()
    val scrollSpace = rememberTvFocusScrollSpace()
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    val scope = rememberCoroutineScope()
    val tracker = remember { TvGridRowTracker(scope) }
    val headerItems = 1 + if (hasPinnedHeader && !TV_PIN_HEADER) 1 else 0
    // The filter may have been disposed while the grid was deep-scrolled.
    // Compose it before requesting focus instead of holding a dangling requester.
    val headerKeys = if (headerDownFocus == null) Modifier else Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
            scope.launch {
                state.scrollToItem(if (TV_PIN_HEADER) 0 else 1)
                withFrameNanos { }
                try { headerDownFocus.requestFocus() } catch (_: IllegalStateException) { }
            }
            true
        } else false
    }
    CompositionLocalProvider(LocalTvGridRowTracker provides tracker) {
    Column(Modifier.fillMaxSize()) {
    if (hasPinnedHeader && TV_PIN_HEADER) Box(headerKeys) { TvPinnedHeader { pinnedHeader() } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.weight(1f).fillMaxSize().clipToBounds(), state = state,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = scrollSpace.bottomPadding()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasPinnedHeader && !TV_PIN_HEADER) item(key = "$screenKey-pinned-header", span = { GridItemSpan(maxLineSpan) }) { Box(headerKeys) { pinnedHeader() } }
        item(key = "$screenKey-header", span = { GridItemSpan(maxLineSpan) }) { Column(Modifier.tvResetGridRowOnFocus()) { header() } }
        if (posters.isEmpty()) item(key = "$screenKey-empty", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.tvResetGridRowOnFocus()) { empty() }
        }
        itemsIndexed(posters, key = { _, poster -> key(poster) }) { index, poster ->
            CompositionLocalProvider(LocalTvGridRow provides index / columns) {
                Box(Modifier.tvPinFocusedRow(state, scrollSpace, index, columns, headerItems)) {
                    card(poster)
                }
            }
            if (shouldLoadMore(index)) LaunchedEffect(key(poster)) { onLoadMore() }
        }
        item(key = "$screenKey-footer", span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.tvResetGridRowOnFocus()) { footer() } }
    }
    }
    }
}
