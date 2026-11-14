package com.dark.darknama.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.dark.darknama.utils.DeviceUtils

/** Shared by a grid's card focus and row-pinning logic. */
class TvGridRowTracker(val scope: CoroutineScope) {
    var lastRow: Int = -1
    var lastPinnedRow: Int = -1
    var pinJob: Job? = null
    var focusEpoch: Long = 0

    fun shouldBringIntoView(row: Int?): Boolean {
        if (row == null) { reset(); return true }
        if (lastRow == row) return false
        lastRow = row
        return true
    }

    fun shouldPin(row: Int): Boolean {
        if (lastPinnedRow == row) return false
        lastPinnedRow = row
        return true
    }

    fun reset() {
        focusEpoch++
        lastRow = -1
        lastPinnedRow = -1
        pinJob?.cancel()
        pinJob = null
    }
}

val LocalTvGridRowTracker = staticCompositionLocalOf<TvGridRowTracker?> { null }
val LocalTvGridRow = staticCompositionLocalOf<Int?> { null }

/**
 * TV-only: keep the *whole* focused item inside its scrolling ancestor, with
 * breathing room at the edges. Attach before the Button/Card focus target.
 * This is a no-op on phones (no visual, touch, or layout changes there).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tvBringIntoViewOnFocus(margin: Dp = 16.dp, horizontalMargin: Dp = margin): Modifier {
    if (!DeviceUtils.isTv(LocalContext.current)) return this
    val tracker = LocalTvGridRowTracker.current
    val row = LocalTvGridRow.current
    val requester = remember { BringIntoViewRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var itemSize by remember { mutableStateOf(IntSize.Zero) }
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    val horizontalMarginPx = with(LocalDensity.current) { horizontalMargin.toPx() }
    LaunchedEffect(hasFocus, itemSize, marginPx, horizontalMarginPx, row, tracker) {
        if (hasFocus && itemSize != IntSize.Zero) {
            if (tracker != null && !tracker.shouldBringIntoView(row)) return@LaunchedEffect
            try {
                requester.bringIntoView(Rect(
                    -horizontalMarginPx, -marginPx,
                    itemSize.width + horizontalMarginPx, itemSize.height + marginPx
                ))
            } catch (_: IllegalStateException) {
                // An item can detach while a lazy list is reloading.
            }
        }
    }
    return this.bringIntoViewRequester(requester)
        .onSizeChanged { itemSize = it }
        .onFocusChanged { hasFocus = it.hasFocus }
}
