package com.dark.darknama.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import com.dark.darknama.utils.DeviceUtils
import androidx.compose.ui.unit.dp

val goldFocusColor = Color(0xFFFFD700)
private val highlightGold = Color(0xFFFFF2AD)
private val contrastEdge = Color(0xFF08090D)

/** Focus is visible for D-pad and keyboard navigation, never merely because touch gave a control focus. */
fun isGoldFocusVisible(isFocused: Boolean, inputMode: InputMode): Boolean =
    isFocused && inputMode == InputMode.Keyboard

/** The default entry point: attach *before* the focusable/clickable target.
 * Draws last so the gold outline remains visible above Material, glass, and neon borders.
 * No per-item blur or infinite animation; the optional scale is for large cards only. */
@Composable
fun Modifier.goldFocus(
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Boolean = false,
    thin: Boolean = false
): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this.onFocusChanged { focused = it.isFocused }
        .goldFocus(focused, shape, scaleOnFocus, thin)
}

/** For controls already exposing a focused interaction state. Do not also use [goldFocus]
 * without an explicit state on the same focus target (that would draw two rings). */
@Composable
fun Modifier.goldFocus(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Boolean = false,
    thin: Boolean = false
): Modifier {
    val isTv = DeviceUtils.isTv(LocalContext.current)
    val visible = isGoldFocusVisible(isFocused, LocalInputModeManager.current.inputMode)
    val amount by animateFloatAsState(if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 160), label = "gold-focus")
    val scale by animateFloatAsState(if (visible && scaleOnFocus) 1.035f else 1f,
        animationSpec = tween(durationMillis = 160), label = "gold-focus-scale")
    val scaled = if (scaleOnFocus) this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        clip = false
    } else this
    return scaled.drawWithCache {
        val path = when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        val brush = Brush.linearGradient(listOf(highlightGold, goldFocusColor, highlightGold))
        onDrawWithContent {
            drawContent()
            if (amount > 0f) {
                if (isTv) {
                    drawPath(path, goldFocusColor.copy(alpha = 0.55f * amount),
                        style = Stroke(width = 1.dp.toPx()))
                } else if (thin) {
                    // Compact TV library cards use only a 1dp gold frame.
                    drawPath(path, goldFocusColor.copy(alpha = amount),
                        style = Stroke(width = 1.dp.toPx()))
                } else {
                    // Other controls retain the existing dark edge and lightweight glow.
                    drawPath(path, contrastEdge.copy(alpha = 0.9f * amount),
                        style = Stroke(width = 9.dp.toPx()))
                    drawPath(path, goldFocusColor.copy(alpha = 0.35f * amount),
                        style = Stroke(width = 6.dp.toPx()))
                    drawPath(path, brush = brush, alpha = amount,
                        style = Stroke(width = 2.5.dp.toPx()))
                }
            }
        }
    }
}
