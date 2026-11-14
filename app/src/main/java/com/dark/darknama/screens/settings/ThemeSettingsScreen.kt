package com.dark.darknama.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.SettingsColorSwatch
import com.dark.darknama.components.SettingsOptionRow
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.ThemeMode
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.ui.theme.VisualStyle
import com.dark.darknama.ui.theme.defaultSecondaryColor
import com.dark.darknama.ui.theme.colorOptions
import com.dark.darknama.ui.theme.colorOptionNames
import com.dark.darknama.ui.theme.defaultPrimaryColor

/**
 * Full-page Theme settings (theme mode + primary colour).
 *
 * Replaces the previous inline drop-down card so that it can be
 * navigated comfortably with an Android TV remote.
 */
@Composable
fun ThemeSettingsScreen(
    navController: NavController?,
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    var themeSettings by remember { mutableStateOf(themeManager.loadThemeSettings()) }

    val colorRows = remember { colorOptions.withIndex().chunked(5) }

    fun update(newSettings: ThemeSettings) {
        themeSettings = newSettings
        themeManager.saveThemeSettings(newSettings)
        onThemeSettingsChanged(newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.theme_settings),
        navController = navController,
        icon = Icons.Default.FormatColorFill
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // ---------- Theme mode ----------
            item(key = "theme-mode-title") {
                SettingsSectionTitle(stringResource(R.string.theme_mode))
            }
            item(key = "theme-mode-light") {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_light),
                    isSelected = themeSettings.themeMode == ThemeMode.LIGHT,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.LIGHT)) }
                )
            }
            item(key = "theme-mode-dark") {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_dark),
                    isSelected = themeSettings.themeMode == ThemeMode.DARK,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.DARK)) }
                )
            }
            item(key = "theme-mode-system") {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_system),
                    isSelected = themeSettings.themeMode == ThemeMode.SYSTEM,
                    onSelect = { update(themeSettings.copy(themeMode = ThemeMode.SYSTEM)) }
                )
            }

            item(key = "theme-mode-space") { Spacer(modifier = Modifier.height(20.dp)) }
            item(key = "theme-style-title") { SettingsSectionTitle(stringResource(R.string.theme_style)) }
            items(VisualStyle.entries, key = { "theme-style-${it.name}" }) { style ->
                SettingsOptionRow(
                    label = stringResource(when (style) {
                        VisualStyle.DEFAULT -> R.string.style_default
                        VisualStyle.GLASS -> R.string.style_glass
                        VisualStyle.NEON_GLASS -> R.string.style_neon_glass
                    }),
                    description = stringResource(when (style) {
                        VisualStyle.DEFAULT -> R.string.style_default_hint
                        VisualStyle.GLASS -> R.string.style_glass_hint
                        VisualStyle.NEON_GLASS -> R.string.style_neon_glass_hint
                    }),
                    isSelected = themeSettings.style == style,
                    onSelect = { update(themeSettings.copy(style = style)) }
                )
            }
            item(key = "theme-style-space") { Spacer(modifier = Modifier.height(24.dp)) }

            // ---------- Primary colour ----------
            item(key = "theme-primary-title") {
                SettingsSectionTitle(stringResource(R.string.primary_color))
            }

            // Ten named swatches in two RTL-aware rows of five, without reordering saved choices.
            items(count = colorRows.size, key = { "theme-primary-row-$it" }) { rowIndex ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    colorRows[rowIndex].forEach { (index, color) ->
                        val name = stringResource(colorOptionNames[index])
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(
                                color = color,
                                isSelected = themeSettings.primaryColor == color,
                                onSelect = { update(themeSettings.copy(primaryColor = color)) },
                                focusKey = "theme-primary-${color.toArgb()}",
                                label = name,
                                accessibilityLabel = "${stringResource(R.string.primary_color)}: $name"
                            )
                        }
                    }
                }
            }

            item(key = "theme-primary-space") { Spacer(modifier = Modifier.height(24.dp)) }
            item(key = "theme-secondary-title") { SettingsSectionTitle(stringResource(R.string.secondary_color)) }
            items(count = colorRows.size, key = { "theme-secondary-row-$it" }) { rowIndex ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    colorRows[rowIndex].forEach { (index, color) ->
                        val name = stringResource(colorOptionNames[index])
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(color, themeSettings.secondaryColor == color,
                                onSelect = { update(themeSettings.copy(secondaryColor = color)) },
                                focusKey = "theme-secondary-${color.toArgb()}",
                                label = name,
                                accessibilityLabel = "${stringResource(R.string.secondary_color)}: $name")
                        }
                    }
                }
            }
            item(key = "theme-style-reset") {
                SettingsOptionRow(
                    label = stringResource(R.string.theme_reset_style),
                    isSelected = false,
                    onSelect = { update(themeSettings.copy(style = VisualStyle.DEFAULT,
                        primaryColor = defaultPrimaryColor, secondaryColor = defaultSecondaryColor)) }
                )
            }
            item(key = "theme-bottom-hint") {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.theme_color_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
