package com.dark.darknama.components

import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.utils.DeviceUtils

/** A 3dp track and small thumb inside a full-width, invisible 48dp touch target. */
@Composable
fun PlayerSeekBar(
    positionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    color: Color,
    onSeekStart: () -> Unit,
    onSeekPreview: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    seekStepMs: Long = 10_000L
) {
    val isTv = DeviceUtils.isTv(LocalContext.current)
    val holdSeekThrottle = remember { HoldSeekThrottle() }
    var dragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableLongStateOf(0L) }
    val onStart by rememberUpdatedState(onSeekStart)
    val onPreview by rememberUpdatedState(onSeekPreview)
    val onSeek by rememberUpdatedState(onSeekTo)
    val onFinish by rememberUpdatedState(onSeekFinished)
    val canSeek = enabled && durationMs > 0L
    val played = fraction(if (dragging) dragPosition else positionMs, durationMs)
    val buffered = fraction(bufferedPositionMs, durationMs)
    val description = stringResource(R.string.player_progress)

    // A stream can become unseekable (or the overlay can disappear) mid-drag.
    // Always release the player's scrubbing state in that case.
    LaunchedEffect(canSeek) {
        if (!canSeek && dragging) {
            dragging = false
            onFinish()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (dragging) {
                dragging = false
                onFinish()
            }
        }
    }

    val gestures = if (canSeek) Modifier
        .pointerInput(durationMs) {
            detectDragGestures(
                onDragStart = { offset ->
                    dragging = true
                    dragPosition = seekPosition(offset.x, size.width.toFloat(), durationMs)
                    onStart()
                    onPreview(dragPosition)
                },
                onDrag = { change, _ ->
                    change.consume()
                    dragPosition = seekPosition(change.position.x, size.width.toFloat(), durationMs)
                    onPreview(dragPosition)
                },
                onDragEnd = {
                    if (dragging) {
                        dragging = false
                        onSeek(dragPosition)
                        onFinish()
                    }
                },
                onDragCancel = {
                    if (dragging) {
                        dragging = false
                        onFinish()
                    }
                }
            )
        }
        .pointerInput(durationMs) {
            detectTapGestures(onTap = { offset ->
                onStart()
                onSeek(seekPosition(offset.x, size.width.toFloat(), durationMs))
                onFinish()
            })
        }
    else Modifier

    // TV: the track itself is a focus target. Left/right seek by the configured
    // step; up/down can still leave it for the other player controls. Phone
    // gestures and accessibility setProgress keep their original behavior.
    val dpad = if (isTv && canSeek) Modifier
        .tvFocusBorder(RoundedCornerShape(12.dp))
        .onKeyEvent { event ->
            val native = event.nativeKeyEvent
            val forward = when (native.keyCode) {
                AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> false
                AndroidKeyEvent.KEYCODE_DPAD_RIGHT, AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> true
                else -> null
            }
            when {
                forward == null -> false
                event.type == KeyEventType.KeyUp -> {
                    holdSeekThrottle.reset(native.keyCode)
                    true
                }
                event.type == KeyEventType.KeyDown -> {
                    if (holdSeekThrottle.shouldSeek(native.keyCode, native.repeatCount,
                            SystemClock.uptimeMillis())) {
                        onStart()
                        onSeek(steppedSeekPosition(positionMs, durationMs,
                            holdSeekStep(seekStepMs, native.repeatCount), forward))
                        onFinish()
                    }
                    true
                }
                else -> false
            }
        }
        .focusable()
    else Modifier

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(dpad)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(played, 0f..1f)
                if (canSeek) setProgress { value ->
                    onStart()
                    onSeek((value.coerceIn(0f, 1f).toDouble() * durationMs.toDouble()).toLong())
                    onFinish()
                    true
                }
            }
            .then(gestures)
    ) {
        val center = size.height / 2f
        val width = size.width
        val playedX = width * played
        val trackWidth = 3.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.30f),
            start = Offset(0f, center), end = Offset(width, center),
            strokeWidth = trackWidth, cap = StrokeCap.Round
        )
        if (buffered > 0f) drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(0f, center), end = Offset(width * buffered, center),
            strokeWidth = trackWidth, cap = StrokeCap.Round
        )
        if (playedX > 0f) drawLine(
            color = color, start = Offset(0f, center), end = Offset(playedX, center),
            strokeWidth = trackWidth, cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = (if (dragging) 6.dp else 4.dp).toPx(),
            center = Offset(playedX, center)
        )
    }
}

/** Step a seekable track without overflowing or moving outside its duration. */
internal fun steppedSeekPosition(positionMs: Long, durationMs: Long, stepMs: Long,
                                 forward: Boolean): Long {
    val end = durationMs.coerceAtLeast(0L)
    val position = positionMs.coerceIn(0L, end)
    val step = stepMs.coerceIn(0L, end)
    return if (forward) {
        if (end - position <= step) end else position + step
    } else (position - step).coerceAtLeast(0L)
}

private fun fraction(position: Long, duration: Long): Float =
    if (duration > 0L) (position.toDouble() / duration.toDouble()).coerceIn(0.0, 1.0).toFloat() else 0f

private fun seekPosition(x: Float, width: Float, duration: Long): Long =
    if (width > 0f && duration > 0L)
        (x.coerceIn(0f, width).toDouble() / width.toDouble() * duration.toDouble())
            .toLong().coerceIn(0L, duration)
    else 0L
