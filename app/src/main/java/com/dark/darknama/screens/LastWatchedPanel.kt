package com.dark.darknama.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.dark.darknama.R
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.VideoPlayerActivity
import com.dark.darknama.components.goldFocus
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.formatTime
import com.dark.darknama.utils.StorageUtils
import java.util.Date

/** History deliberately does not share the Favorites playlist or its clear-all action. */
@Composable
fun LastWatchedPanel(
    modifier: Modifier = Modifier,
    tvListState: LazyListState? = null,
    tvHeader: (@Composable (FocusRequester) -> Unit)? = null,
    tvTabFocus: FocusRequester? = null,
    tvEmptyAction: (() -> Unit)? = null,
    phoneHeader: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var records by remember { mutableStateOf(StorageUtils.loadPlaybackHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }
    var focusEmptyAfterClear by remember { mutableStateOf(false) }

    // The player is a separate Activity: refresh when it closes, not just when this tab opens.
    DisposableEffect(owner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) records = StorageUtils.loadPlaybackHistory(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(modifier = Modifier.tvFocusBorder(CircleShape), onClick = {
                    StorageUtils.clearPlaybackHistory(context)
                    records = emptyList()
                    focusEmptyAfterClear = true
                    showClearDialog = false
                }) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val unfinished = records.filterNot { it.isFinished }
    val finished = records.filter { it.isFinished }
    if (tvHeader != null && tvListState != null && tvTabFocus != null && tvEmptyAction != null) {
        TvLastWatchedList(
            modifier = modifier, records = records, state = tvListState,
            header = tvHeader, tabFocus = tvTabFocus, emptyAction = tvEmptyAction,
            focusEmptyAfterClear = focusEmptyAfterClear,
            onEmptyFocused = { focusEmptyAfterClear = false },
            onClear = { showClearDialog = true },
            onRemove = { entry ->
                StorageUtils.removePlayback(context, entry.key)
                records = StorageUtils.loadPlaybackHistory(context)
            }
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") { phoneHeader() }
        if (records.isNotEmpty()) {
            item {
                TextButton(onClick = { showClearDialog = true },
                    modifier = Modifier.tvFocusItem("history-clear").tvFocusBorder(CircleShape)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.history_clear_all))
                }
            }
        }
        item { Text(stringResource(R.string.history_unfinished), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        if (unfinished.isEmpty()) {
            item { Text(stringResource(R.string.history_none_unfinished), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(unfinished, key = { "${it.type}-${it.contentId}-${it.seasonId}-${it.episodeId}" }) { entry ->
            LastWatchedCard(entry, onPlay = { VideoPlayerActivity.startFromHistory(context, entry) },
                onRemove = {
                    StorageUtils.removePlayback(context, entry.key)
                    records = StorageUtils.loadPlaybackHistory(context)
                })
        }
        item { Text(stringResource(R.string.history_finished), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)) }
        if (finished.isEmpty()) {
            item { Text(stringResource(R.string.history_none_finished), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(finished, key = { "${it.type}-${it.contentId}-${it.seasonId}-${it.episodeId}" }) { entry ->
            LastWatchedCard(entry, onPlay = { VideoPlayerActivity.startFromHistory(context, entry) },
                onRemove = {
                    StorageUtils.removePlayback(context, entry.key)
                    records = StorageUtils.loadPlaybackHistory(context)
                })
        }
    }
}

@Composable
internal fun LastWatchedCard(
    entry: PlaybackRecord, onPlay: () -> Unit, onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    playFocus: FocusRequester? = null,
    removeFocus: FocusRequester? = null
) {
    val context = LocalContext.current
    val playInteraction = remember { MutableInteractionSource() }
    val playFocused by playInteraction.collectIsFocusedAsState()
    val removeInteraction = remember { MutableInteractionSource() }
    val removeFocused by removeInteraction.collectIsFocusedAsState()
    Card(
        onClick = onPlay,
        interactionSource = playInteraction,
        modifier = (if (playFocus != null) modifier.focusRequester(playFocus) else modifier)
            .tvFocusItem("history-play-${entry.key}")
            .fillMaxWidth().tvBringIntoViewOnFocus()
            .goldFocus(playFocused, RoundedCornerShape(12.dp), thin = playFocus != null),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.image.isNotBlank()) {
                    AsyncImage(
                        model = entry.image, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (entry.type == "series") {
                        Text(listOfNotNull(entry.seasonTitle, entry.episodeTitle)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (entry.quality.isNotBlank()) {
                        Text(entry.quality, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(
                    onClick = onRemove,
                    interactionSource = removeInteraction,
                    modifier = (if (removeFocus != null) Modifier.focusRequester(removeFocus)
                        else Modifier).tvFocusItem("history-remove-${entry.key}")
                        .size(48.dp).tvBringIntoViewOnFocus()
                        .settingsFocusBorder(removeFocused, CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_remove))
                }
            }
            if (entry.durationMs > 0L) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { entry.progress }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.history_progress, (entry.progress * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.history_remaining, formatTime(entry.remainingMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (entry.positionMs > 0L) {
                Text(stringResource(R.string.history_position, formatTime(entry.positionMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(stringResource(if (entry.isFinished) R.string.history_replay else R.string.history_resume),
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                val date = android.text.format.DateFormat.getDateFormat(context).format(Date(entry.updatedAt))
                val time = android.text.format.DateFormat.getTimeFormat(context).format(Date(entry.updatedAt))
                Text("$date · $time", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
