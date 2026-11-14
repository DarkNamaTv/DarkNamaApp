package com.dark.darknama.screens

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dark.darknama.components.LocalTvGridRowTracker
import kotlinx.coroutines.launch

internal class TvFocusScrollSpace {
    var bottomPaddingPx by mutableIntStateOf(0)
}

@Composable
internal fun rememberTvFocusScrollSpace(): TvFocusScrollSpace = remember { TvFocusScrollSpace() }

@Composable
internal fun TvFocusScrollSpace.bottomPadding(): Dp =
    maxOf(56.dp, with(LocalDensity.current) { bottomPaddingPx.toDp() })


/** Leaving the card area invalidates both row and pin state. */
@Composable
internal fun Modifier.tvResetGridRowOnFocus(): Modifier {
    val tracker = LocalTvGridRowTracker.current
    return onFocusChanged { if (it.hasFocus) tracker?.reset() }
}

private class TvCardFocusState {
    var hadFocus = false
}

/** Pin the focused card's row; returning to the first row reveals the controls. */
@Composable
internal fun Modifier.tvPinFocusedRow(
    state: LazyGridState, space: TvFocusScrollSpace, cardIndex: Int, columns: Int, headerItems: Int
): Modifier {
    val tracker = LocalTvGridRowTracker.current ?: return this
    val focusState = remember { TvCardFocusState() }
    val row = cardIndex / columns
    val target = if (row == 0) 0 else headerItems + row * columns
    return onFocusChanged { focus ->
        if (!focus.hasFocus && focusState.hadFocus) {
            focusState.hadFocus = false
            val epoch = tracker.focusEpoch
            tracker.scope.launch {
                withFrameNanos { }
                if (tracker.focusEpoch == epoch) tracker.reset()
            }
        } else if (focus.hasFocus && !focusState.hadFocus) {
            focusState.hadFocus = true
            tracker.focusEpoch++
            if (tracker.shouldPin(row)) {
                tracker.pinJob?.cancel()
                tracker.pinJob = tracker.scope.launch {
                    // Reserve enough trailing space for the last row to reach the top too.
                    if (row > 0) {
                        val layout = state.layoutInfo
                        val item = layout.visibleItemsInfo.firstOrNull { it.index == headerItems + cardIndex }
                        if (item != null) {
                            space.bottomPaddingPx = maxOf(0, layout.viewportSize.height - item.size.height)
                        }
                    }
                    // Allow padding and the child's bring-into-view request to settle.
                    withFrameNanos { }
                    if (target < state.layoutInfo.totalItemsCount) {
                        state.animateScrollToItem(target,
                            if (row == 0) 0 else state.layoutInfo.beforeContentPadding)
                    }
                }
            }
        }
    }
}
