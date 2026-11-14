package com.dark.darknama

import kotlinx.coroutines.delay

internal const val CONTROLS_AUTO_HIDE_DELAY_MS = 2_000L

/**
 * Runs inside a LaunchedEffect keyed to the last interaction and playback state.
 * Cancellation resets the countdown; the second check also covers events that
 * arrive before Compose has had a chance to restart the effect.
 */
internal suspend fun awaitControlsAutoHide(canHide: () -> Boolean, hide: () -> Unit) {
    if (!canHide()) return
    delay(CONTROLS_AUTO_HIDE_DELAY_MS)
    if (canHide()) hide()
}
