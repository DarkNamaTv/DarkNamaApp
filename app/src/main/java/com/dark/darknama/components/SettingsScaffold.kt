package com.dark.darknama.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.dark.darknama.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.ui.theme.contrastingOn
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.utils.DeviceUtils

/**
 * Shared building blocks for the settings screens.
 *
 * Each settings category (Theme / Player / Font / …) is now a real
 * full-screen page instead of an inline drop-down, which is much easier
 * to operate with an Android TV remote (the same pattern used by the
 * "About" page).
 */

/** Kept for existing settings/control call sites; delegates to the one gold focus renderer. */
@Composable
fun Modifier.settingsFocusBorder(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this.goldFocus(isFocused, shape)

/**
 * Standard page frame for a settings sub-page: a back button + title header
 * and a scrollable content area.
 */
@Composable
fun SettingsSubPage(
    title: String,
    navController: NavController?,
    icon: ImageVector? = null,
    content: @Composable () -> Unit
) {
    val backInteractionSource = remember { MutableInteractionSource() }
    val isBackFocused by backInteractionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController?.popBackStack() },
                interactionSource = backInteractionSource,
                modifier = Modifier
                    .tvFocusItem("settings-back", initial = true)
                    .size(if (DeviceUtils.isTv(LocalContext.current)) 48.dp else 44.dp)
                    .settingsFocusBorder(isBackFocused, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * A tappable row in the settings list that navigates to a sub-page.
 * Shows the title, an optional current-value summary and a chevron.
 */
@Composable
fun SettingsNavigationCard(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth().tvBringIntoViewOnFocus()
            .settingsFocusBorder(isFocused),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A single-choice option row (radio style) used inside settings sub-pages.
 * The whole row is one focus target so a TV remote can select it with OK.
 */
@Composable
fun SettingsOptionRow(
    label: String,
    isSelected: Boolean,
    description: String? = null,
    onSelect: () -> Unit,
    previewText: String? = null,
    previewFontFamily: FontFamily? = null,
    focusKey: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier.tvFocusItem(focusKey ?: "settings-option-$label")
            .fillMaxWidth()
            .tvBringIntoViewOnFocus()
            .settingsFocusBorder(isFocused, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFocused)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onSelect() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // onClick = null keeps the RadioButton purely visual so the whole row
        // is a single focus target for the TV remote.
        RadioButton(selected = isSelected, onClick = null)
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected || isFocused)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (previewText != null) {
                // The current app theme can be a different font: override the
                // family for *this* row's sample, even for System Default.
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = previewFontFamily ?: FontFamily.Default,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/** Section title inside a settings sub-page. */
@Composable
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

/**
 * A numeric value row with focusable "−" / "+" buttons.
 *
 * A plain Slider is very hard to operate with a TV remote, so every numeric
 * setting is adjusted through discrete steps instead.
 */
@Composable
fun SettingsStepperRow(
    label: String,
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    canDecrease: Boolean = true,
    canIncrease: Boolean = true,
    focusKey: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperButton(
                symbol = "−",
                enabled = canDecrease,
                focusKey = focusKey?.let { "$it-decrease" },
                onClick = onDecrease
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            StepperButton(
                symbol = "+",
                enabled = canIncrease,
                focusKey = focusKey?.let { "$it-increase" },
                onClick = onIncrease
            )
        }
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    focusKey: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = (if (enabled && focusKey != null) Modifier.tvFocusItem(focusKey)
            else Modifier).size(48.dp)
            .settingsFocusBorder(isFocused, CircleShape)
            .clip(CircleShape)
            .background(
                when {
                    !enabled -> MaterialTheme.colorScheme.surfaceVariant
                    isFocused -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isFocused -> MaterialTheme.colorScheme.onPrimary
                // The idle button has only a 25% accent fill. Use surface text
                // rather than accent-on-accent so +/− remain readable in dark mode.
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/**
 * A focusable colour swatch used by the theme / subtitle colour pickers.
 */
@Composable
fun SettingsColorSwatch(
    color: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    label: String? = null,
    swatchSize: Int = 48,
    showEmptyIndicator: Boolean = false,
    focusKey: String? = null,
    accessibilityLabel: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val size = if (DeviceUtils.isTv(LocalContext.current)) swatchSize.coerceAtLeast(48)
        else swatchSize

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = (if (focusKey != null) Modifier.tvFocusItem(focusKey) else Modifier)
                .size(size.dp).tvBringIntoViewOnFocus()
                .settingsFocusBorder(isFocused, CircleShape)
                .clip(CircleShape)
                .background(
                    if (color == Color.Transparent) Color.Gray.copy(alpha = 0.3f) else color
                )
                .semantics {
                    contentDescription = accessibilityLabel ?: label ?: color.toString()
                    selected = isSelected
                    role = Role.RadioButton
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onSelect() },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contrastingOn(if (color == Color.Transparent)
                        MaterialTheme.colorScheme.surfaceVariant else color),
                    modifier = Modifier.size((size / 2).dp)
                )
            } else if (showEmptyIndicator && color == Color.Transparent) {
                Text(
                    text = "—",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isFocused || isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
