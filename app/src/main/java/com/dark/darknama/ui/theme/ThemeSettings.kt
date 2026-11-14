package com.dark.darknama.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val primaryColor: Color = defaultPrimaryColor,
    val secondaryColor: Color = defaultSecondaryColor,
    val style: VisualStyle = VisualStyle.DEFAULT
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class VisualStyle {
    DEFAULT, GLASS, NEON_GLASS;

    companion object {
        fun fromStored(value: String?): VisualStyle =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

// Keep the ten positions stable; only the former sixth swatch changes hue.
val colorOptions = listOf(
    Color(0xFF838FDA), Color(0xFFFFD700), Color(0xFF536878), Color(0xFF26AF9C),
    Color(0xFF00D0FF), Color(0xFFFF0097), Color(0xFFFF0303), Color(0xFFB7FF03),
    Color(0xFF4B4F56), Color(0xFF1E3A8A)
)

/** Localized, spoken and visible names in the same order as [colorOptions]. */
val colorOptionNames = listOf(
    com.dark.darknama.R.string.color_periwinkle, com.dark.darknama.R.string.color_gold,
    com.dark.darknama.R.string.color_slate_gray, com.dark.darknama.R.string.color_teal,
    com.dark.darknama.R.string.color_cyan, com.dark.darknama.R.string.color_magenta,
    com.dark.darknama.R.string.color_red, com.dark.darknama.R.string.color_lime,
    com.dark.darknama.R.string.color_graphite, com.dark.darknama.R.string.color_navy
)

val defaultPrimaryColor = colorOptions.first()
val defaultSecondaryColor = colorOptions.first()

// Only used for migration; never offered as a palette choice again.
private val previousSixthColor = Color(0xFFFF0084)

/** Preserve the replacement swatch when upgrading; unsupported colors use the first swatch. */
fun supportedThemeColor(color: Color): Color = when {
    color == previousSixthColor -> colorOptions[5]
    color in colorOptions -> color
    else -> defaultPrimaryColor
}

/** Old versions may have saved the sixth swatch by name instead of ARGB. */
private fun storedThemeColor(value: Any?, fallback: Color): Color = when (value) {
    is Int -> supportedThemeColor(Color(value))
    is String -> if (value.trim().equals("pink", ignoreCase = true)) colorOptions[5] else fallback
    else -> fallback
}

/** Called by the preference loader; write normalized ARGBs whenever either saved value changes. */
internal fun normalizeStoredThemeColors(
    storedPrimary: Any?, storedSecondary: Any?, persist: (Int, Int) -> Unit
): Pair<Color, Color> {
    val primary = storedThemeColor(storedPrimary, defaultPrimaryColor)
    val secondary = storedThemeColor(storedSecondary, defaultSecondaryColor)
    if ((storedPrimary != null && storedPrimary != primary.toArgb()) ||
        (storedSecondary != null && storedSecondary != secondary.toArgb())) {
        persist(primary.toArgb(), secondary.toArgb())
    }
    return primary to secondary
}

@Composable
fun rememberThemeSettings(): ThemeSettings {
    return remember { ThemeSettings() }
}