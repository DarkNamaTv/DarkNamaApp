package com.dark.darknama.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dark.darknama.utils.DeviceUtils

/** Over moving video the glass must stay translucent *and* sufficiently opaque for
 * theme onSurface text. Default retains its original Material surface exactly. */
fun playerDialogSurface(style: VisualStyle, surface: Color): Color = when (style) {
    VisualStyle.DEFAULT -> surface
    VisualStyle.GLASS -> surface.copy(alpha = 0.94f)
    VisualStyle.NEON_GLASS -> surface.copy(alpha = 0.96f)
}

/** Shared visual-style wrapper for the main player settings and audio/subtitle/speed pickers. */
@Composable
fun PlayerAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    onInteraction: () -> Unit = {}
) {
    val isTv = DeviceUtils.isTv(LocalContext.current)
    val style = LocalVisualStyle.current
    val interaction by rememberUpdatedState(onInteraction)
    GlassAlertDialog(
        onDismissRequest = { interaction(); onDismissRequest() },
        title = title, text = text, confirmButton = confirmButton,
        modifier = (if (isTv) Modifier.widthIn(max = 720.dp).fillMaxWidth(0.88f) else Modifier)
            .onPreviewKeyEvent { interaction(); false }
            // Dialogs have their own window: Activity.dispatchTouchEvent cannot see
            // their swipes, button presses or D-pad focus navigation.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interaction()
                    }
                }
            },
        containerColor = playerDialogSurface(style, MaterialTheme.colorScheme.surface),
        tonalElevation = if (style == VisualStyle.DEFAULT) 6.dp else 0.dp,
        shape = RoundedCornerShape(20.dp),
        properties = DialogProperties(usePlatformDefaultWidth = !isTv)
    )
}
