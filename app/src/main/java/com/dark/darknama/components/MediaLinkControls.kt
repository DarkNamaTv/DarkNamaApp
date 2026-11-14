package com.dark.darknama.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.dark.darknama.ui.theme.GlassDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.ui.theme.glassBorder

/** Reused for real movie qualities and series season/track + quality, with TV focus. */
@Composable
fun <T> MediaLinkSelector(
    options: List<T>, selected: T?, placeholder: String, description: String,
    label: @Composable (T) -> String, onSelect: (T) -> Unit,
    modifier: Modifier = Modifier, focusKey: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true }, enabled = options.isNotEmpty(),
            interactionSource = interaction,
            modifier = (if (focusKey != null && options.isNotEmpty())
                Modifier.tvFocusItem(focusKey) else Modifier)
                .fillMaxWidth().heightIn(min = 56.dp)
                .settingsFocusBorder(focused, shape).glassBorder(shape)
                .semantics { contentDescription = description },
            shape = shape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(selected?.let { label(it) } ?: placeholder,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Start,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            val initialIndex = options.indexOfFirst { it == selected }.takeIf { it >= 0 } ?: 0
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(label(option), maxLines = 2) },
                    onClick = { expanded = false; onSelect(option) },
                    modifier = (if (index == initialIndex) Modifier.tvDialogInitialFocus()
                        else Modifier).tvFocusBorder(),
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Default.Check, contentDescription = stringResource(R.string.selected)) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun MediaCopyAllButton(enabled: Boolean, onClick: () -> Unit,
                       modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val description = stringResource(R.string.copy_all)
    Button(
        onClick = onClick, enabled = enabled, interactionSource = interaction,
        modifier = modifier.size(56.dp).settingsFocusBorder(focused, shape).glassBorder(shape)
            .semantics { contentDescription = description },
        shape = shape, contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun MediaLinkBadge() {
    Box(Modifier.size(48.dp).clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.HighQuality, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(27.dp))
    }
}

@Composable
fun MediaLinkArrow() {
    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
        tint = MaterialTheme.colorScheme.primary)
}
