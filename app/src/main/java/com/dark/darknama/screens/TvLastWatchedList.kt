package com.dark.darknama.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.VideoPlayerActivity
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.data.model.PlaybackKey
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.navigation.LocalSidebarFocusRequester
import com.dark.darknama.navigation.tvFocusItem

private data class RemovedRow(val key: PlaybackKey, val index: Int)

private fun rowKey(entry: PlaybackRecord): String =
    "history:${entry.type}:${entry.contentId}:${entry.seasonId}:${entry.episodeId}"

/** The TV history tab has one keyed scroll state, including the title and tab switcher. */
@Composable
internal fun TvLastWatchedList(
    modifier: Modifier,
    records: List<PlaybackRecord>,
    state: LazyListState,
    header: @Composable (FocusRequester) -> Unit,
    tabFocus: FocusRequester,
    emptyAction: () -> Unit,
    focusEmptyAfterClear: Boolean,
    onEmptyFocused: () -> Unit,
    onClear: () -> Unit,
    onRemove: (PlaybackRecord) -> Unit
) {
    val context = LocalContext.current
    val unfinished = records.filterNot { it.isFinished }
    val finished = records.filter { it.isFinished }
    val order = unfinished + finished
    val sidebar = LocalSidebarFocusRequester.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val clearFocus = remember { FocusRequester() }
    val firstRowFocus = remember { FocusRequester() }
    val emptyFocus = remember { FocusRequester() }
    val targetFocus = if (records.isEmpty()) emptyFocus else clearFocus
    val rowFocus = remember { mutableMapOf<PlaybackKey, Pair<FocusRequester, FocusRequester>>() }
    var removed by remember { mutableStateOf<RemovedRow?>(null) }
    var lastFocused by remember { mutableStateOf<PlaybackKey?>(null) }

    LaunchedEffect(removed, records, focusEmptyAfterClear) {
        if (removed == null && !focusEmptyAfterClear) return@LaunchedEffect
        if (order.isEmpty()) {
            state.scrollToItem(2) // Header, Unfinished label, empty browse action.
            withFrameNanos { }
            try { emptyFocus.requestFocus() } catch (_: IllegalStateException) { }
            onEmptyFocused()
        } else {
            val removedIndex = removed?.index ?: 0
            val surviving = order.indexOfFirst { it.key == removed?.key }
            val nextIndex = if (surviving >= 0) surviving else removedIndex.coerceIn(0, order.lastIndex)
            val next = order[nextIndex]
            // Header(0), clear(1), unfinished label(2), then the section cards.
            val unfinishedStart = 3
            val finishedStart = unfinishedStart + maxOf(1, unfinished.size) + 1
            val listIndex = if (nextIndex < unfinished.size) unfinishedStart + nextIndex
                else finishedStart + nextIndex - unfinished.size
            if (state.layoutInfo.visibleItemsInfo.none { it.key == rowKey(next) }) {
                state.scrollToItem(listIndex)
            }
            repeat(4) {
                withFrameNanos { }
                val target = rowFocus[next.key]?.first
                if (target != null) {
                    try {
                        target.requestFocus()
                        removed = null
                        onEmptyFocused()
                        return@LaunchedEffect
                    } catch (_: IllegalStateException) { }
                }
            }
            // Never strand the D-pad if a row was detached during a second reload.
            try { clearFocus.requestFocus() } catch (_: IllegalStateException) { }
        }
        removed = null
    }

    // Refreshes after returning from the player should retain the focused row,
    // even if its updated watch progress moves it to another section.
    LaunchedEffect(records) {
        val key = lastFocused ?: return@LaunchedEffect
        if (removed != null) return@LaunchedEffect
        val index = order.indexOfFirst { it.key == key }
        if (index < 0) return@LaunchedEffect
        val next = order[index]
        if (state.layoutInfo.visibleItemsInfo.none { it.key == rowKey(next) }) {
            val unfinishedStart = 3
            val finishedStart = unfinishedStart + maxOf(1, unfinished.size) + 1
            state.scrollToItem(if (index < unfinished.size) unfinishedStart + index
                else finishedStart + index - unfinished.size)
        }
        withFrameNanos { }
        try { rowFocus[key]?.first?.requestFocus() } catch (_: IllegalStateException) { }
    }

    LazyColumn(
        state = state, modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "history-header") { header(targetFocus) }
        if (records.isNotEmpty()) {
            item(key = "history-clear") {
                TextButton(onClick = onClear,
                    modifier = Modifier.focusRequester(clearFocus)
                        .focusProperties {
                            up = tabFocus
                            if (sidebar != null) {
                                if (isRtl) right = sidebar else left = sidebar
                            }
                        }
                        .tvFocusItem("history-clear")
                        .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.history_clear_all))
                }
            }
        }
        item(key = "history-unfinished-header") {
            Text(stringResource(R.string.history_unfinished),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
        }
        if (unfinished.isEmpty()) {
            item(key = "history-unfinished-empty") {
                if (records.isEmpty()) {
                    TextButton(onClick = emptyAction,
                        modifier = Modifier.focusRequester(emptyFocus)
                            .focusProperties {
                                up = tabFocus
                                if (sidebar != null) {
                                    if (isRtl) right = sidebar else left = sidebar
                                }
                            }
                            .tvFocusItem("history-empty")
                            .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.browse_movies),
                            modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    Text(stringResource(R.string.history_none_unfinished),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        itemsIndexed(unfinished, key = { _, entry -> rowKey(entry) }) { index, entry ->
            val requester = remember(entry.key, index == 0) {
                if (index == 0) firstRowFocus else FocusRequester()
            }
            val removeRequester = remember(entry.key) { FocusRequester() }
            DisposableEffect(entry.key, requester, removeRequester) {
                rowFocus[entry.key] = requester to removeRequester
                onDispose { if (rowFocus[entry.key]?.first === requester) rowFocus.remove(entry.key) }
            }
            LastWatchedCard(entry,
                onPlay = { VideoPlayerActivity.startFromHistory(context, entry) },
                onRemove = {
                    removed = RemovedRow(entry.key, index)
                    onRemove(entry)
                },
                modifier = Modifier.onFocusChanged { if (it.hasFocus) lastFocused = entry.key }
                    .focusProperties {
                        if (index == 0) up = clearFocus
                        if (sidebar != null) {
                            if (isRtl) right = sidebar else left = sidebar
                        }
                    },
                playFocus = requester, removeFocus = removeRequester)
        }
        item(key = "history-finished-header") {
            Text(stringResource(R.string.history_finished),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        if (finished.isEmpty()) {
            item(key = "history-finished-empty") {
                Text(stringResource(R.string.history_none_finished),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        itemsIndexed(finished, key = { _, entry -> rowKey(entry) }) { index, entry ->
            val orderIndex = unfinished.size + index
            val requester = remember(entry.key, orderIndex == 0) {
                if (orderIndex == 0) firstRowFocus else FocusRequester()
            }
            val removeRequester = remember(entry.key) { FocusRequester() }
            DisposableEffect(entry.key, requester, removeRequester) {
                rowFocus[entry.key] = requester to removeRequester
                onDispose { if (rowFocus[entry.key]?.first === requester) rowFocus.remove(entry.key) }
            }
            LastWatchedCard(entry,
                onPlay = { VideoPlayerActivity.startFromHistory(context, entry) },
                onRemove = {
                    removed = RemovedRow(entry.key, orderIndex)
                    onRemove(entry)
                },
                modifier = Modifier.onFocusChanged { if (it.hasFocus) lastFocused = entry.key }
                    .focusProperties {
                        if (orderIndex == 0) up = clearFocus
                        if (sidebar != null) {
                            if (isRtl) right = sidebar else left = sidebar
                        }
                    },
                playFocus = requester, removeFocus = removeRequester)
        }
    }
}
