package com.dark.darknama.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit

class ThemeManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    fun saveThemeSettings(settings: ThemeSettings) {
        prefs.edit {
            putString("theme_mode", settings.themeMode.name)
            putInt("primary_color", supportedThemeColor(settings.primaryColor).toArgb())
            putInt("secondary_color", supportedThemeColor(settings.secondaryColor).toArgb())
            putString("visual_style", settings.style.name)
        }
    }
    
    fun loadThemeSettings(): ThemeSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
        
        // getInt throws for older string-based selections. Read the raw values
        // so both an old ARGB and an old color name can be normalized in place.
        val stored = prefs.all
        val (primary, secondary) = normalizeStoredThemeColors(
            stored["primary_color"], stored["secondary_color"]
        ) { primaryArgb, secondaryArgb ->
            prefs.edit {
                putInt("primary_color", primaryArgb)
                putInt("secondary_color", secondaryArgb)
            }
        }
        return ThemeSettings(
            themeMode = themeMode,
            primaryColor = primary,
            secondaryColor = secondary,
            style = VisualStyle.fromStored(prefs.getString("visual_style", null))
        )
    }
}

@Composable
fun rememberThemeManager(): ThemeManager {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember { ThemeManager(context) }
}

@Composable
fun rememberAppThemeSettings(): MutableState<ThemeSettings> {
    val themeManager = rememberThemeManager()
    val settings = remember { mutableStateOf(themeManager.loadThemeSettings()) }
    return settings
}