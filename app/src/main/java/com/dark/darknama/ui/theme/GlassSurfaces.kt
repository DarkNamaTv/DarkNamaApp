package com.dark.darknama.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.Dp
import com.dark.darknama.utils.DeviceUtils
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource

/** One styling layer shared by cards, menus, dialogs and navigation chrome. */
val LocalVisualStyle = compositionLocalOf { VisualStyle.DEFAULT }

@Composable
fun Modifier.glassBorder(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val style = LocalVisualStyle.current
    if (style == VisualStyle.DEFAULT) return this
    val colors = MaterialTheme.colorScheme
    val borderBrush = if (style == VisualStyle.NEON_GLASS) {
        Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.95f),
            colors.secondary.copy(alpha = 0.65f), colors.primary.copy(alpha = 0.9f)))
    } else {
        Brush.linearGradient(listOf(colors.onSurface.copy(alpha = 0.32f),
            Color.White.copy(alpha = 0.12f)))
    }
    // TV/low-power devices retain the glass/neon outline but skip per-card shadows.
    val surface = if (DeviceUtils.isTv(LocalContext.current)) this else this.shadow(
        if (style == VisualStyle.NEON_GLASS) 7.dp else 3.dp, shape,
        ambientColor = colors.primary.copy(alpha = 0.32f),
        spotColor = colors.secondary.copy(alpha = 0.30f))
    return surface.border(BorderStroke(1.dp, borderBrush), shape)
}

@Composable
fun Modifier.glassChrome(shape: Shape = RoundedCornerShape(0.dp)): Modifier =
    if (LocalVisualStyle.current == VisualStyle.DEFAULT) this
    else this.glassBorder(shape)

@Composable
fun chromeContainerColor(): Color = MaterialTheme.colorScheme.surface

/** Drop-in replacement for Material Card; explicit focus/click behavior is preserved. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    colors: CardColors? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val actualColors = colors ?: if (LocalVisualStyle.current == VisualStyle.DEFAULT)
        CardDefaults.cardColors()
    else CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val styledModifier = modifier.glassBorder(shape)
    if (onClick == null) {
        Card(modifier = styledModifier, shape = shape, colors = actualColors,
            elevation = elevation, content = content)
    } else {
        Card(onClick = onClick, modifier = styledModifier, enabled = enabled,
            interactionSource = interactionSource, shape = shape, colors = actualColors,
            elevation = elevation, content = content)
    }
}

@Composable
fun GlassDropdownMenu(
    expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest,
        modifier = modifier.glassBorder(RoundedCornerShape(12.dp)), content = content)
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    tonalElevation: Dp = 6.dp,
    properties: DialogProperties = DialogProperties()
) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = confirmButton,
        modifier = modifier.glassBorder(shape), dismissButton = dismissButton,
        icon = icon, title = title, text = text, shape = shape,
        containerColor = containerColor, tonalElevation = tonalElevation,
        properties = properties)
}
