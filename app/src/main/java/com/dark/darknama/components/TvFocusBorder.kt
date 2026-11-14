package com.dark.darknama.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Legacy name for focus targets without an interaction source. Also works with
 * a physical keyboard on a phone; a touch-focused control never draws gold. */
@Composable
fun Modifier.tvFocusBorder(shape: Shape = RoundedCornerShape(12.dp)): Modifier =
    this.goldFocus(shape)
