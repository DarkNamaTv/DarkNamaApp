@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.dark.darknama.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dark.darknama.BuildConfig
import com.dark.darknama.R
import com.dark.darknama.components.SettingsNavigationCard
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.ui.theme.GlassCard as Card
import com.dark.darknama.data.model.AppFont
import com.dark.darknama.data.model.EnglishFontType
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.ui.settings.ServerSettingsViewModel
import com.dark.darknama.ui.settings.UpdateViewModel
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.ThemeMode
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.ui.theme.VisualStyle
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.LanguageUtils
import com.dark.darknama.utils.StorageUtils

/**
 * Settings hub.
 *
 * Every category (Theme / Player+Subtitles / Font) opens as its own
 * full page instead of an inline drop-down, which is far easier to use
 * with an Android TV remote (same pattern as the "About" page).
 */
@Composable
fun SettingsScreen(
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {},
    onFontSettingsChanged: (FontSettings) -> Unit = {},
    navController: NavController? = null
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val serverViewModel: ServerSettingsViewModel = viewModel()
    val serverSettings by serverViewModel.settings.collectAsState()
    val updates: UpdateViewModel = viewModel(viewModelStoreOwner = context as ComponentActivity)
    val updateState by updates.state.collectAsState()
    val automaticUpdates by updates.automaticChecks.collectAsState()

    // Re-read the persisted settings on every recomposition of this screen so
    // the summary lines stay correct after returning from a sub-page.
    var refreshKey by remember { mutableStateOf(0) }
    val themeSettings = remember(refreshKey) { themeManager.loadThemeSettings() }
    val subtitleSettings = remember(refreshKey) { StorageUtils.loadSubtitleSettings(context) }
    val videoPlayerSettings = remember(refreshKey) { StorageUtils.loadVideoPlayerSettings(context) }
    val fontSettings = remember(refreshKey) { StorageUtils.loadFontSettings(context) }

    var showResetDialog by remember { mutableStateOf(false) }
    var showClearWatchedEpisodesDialog by remember { mutableStateOf(false) }

    // Reset all settings to defaults
    fun resetToDefaults() {
        val defaultTheme = ThemeSettings()
        themeManager.saveThemeSettings(defaultTheme)
        onThemeSettingsChanged(defaultTheme)

        StorageUtils.saveSubtitleSettings(context, SubtitleSettings.getDefaultSettings(context))
        StorageUtils.saveVideoPlayerSettings(context, VideoPlayerSettings.DEFAULT)
        serverViewModel.reset()
        updates.setAutomaticChecks(true)

        val defaultFont = FontSettings.DEFAULT
        StorageUtils.saveFontSettings(context, defaultFont)
        onFontSettingsChanged(defaultFont)

        // Force the summaries to reload
        refreshKey++
    }

    fun clearWatchedEpisodes() {
        try {
            StorageUtils.clearAllWatchedEpisodes(context)
            Toast.makeText(context, context.getString(R.string.watched_cache_cleared), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Error clearing watched episodes", e)
            Toast.makeText(context, context.getString(R.string.error_clearing_cache), Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Summary lines shown under each category ----
    val themeModeLabel = when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    }
    // The summary describes the choice actually applied to the app's language.
    val fontLabel = stringResource(when (val active =
        fontSettings.forLanguage(LanguageUtils.getSavedLanguage(context))) {
        is AppFont.Persian -> when (active.type) {
            FontType.DEFAULT -> R.string.font_system_default
            FontType.VAZIRMATN -> R.string.font_vazirmatn
        }
        is AppFont.English -> when (active.type) {
            EnglishFontType.DEFAULT -> R.string.font_system_default
            EnglishFontType.INTER -> R.string.font_inter
            EnglishFontType.POPPINS -> R.string.font_poppins
            EnglishFontType.MONTSERRAT -> R.string.font_montserrat
            EnglishFontType.NUNITO -> R.string.font_nunito
            EnglishFontType.LATO -> R.string.font_lato
        }
    })
    val playerSummary = stringResource(
        R.string.player_settings_summary,
        videoPlayerSettings.seekTimeSeconds,
        subtitleSettings.textSize.toInt()
    )
    val serverLabel = stringResource(when (serverSettings.currentServer) {
        2 -> R.string.server_2
        3 -> R.string.server_3
        else -> R.string.server_1
    })
    val serverSummary = if (serverSettings.automatic)
        stringResource(R.string.server_auto_summary, serverLabel)
    else serverLabel

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentPadding = PaddingValues(bottom = if (DeviceUtils.isTv(context)) 32.dp else 0.dp)
    ) {
        // ---------- Header ----------
        item(key = "settings-header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // Language toggle (English <-> Farsi)
                val langInteraction = remember { MutableInteractionSource() }
                val isLangFocused by langInteraction.collectIsFocusedAsState()
                IconButton(
                    onClick = {
                        LanguageUtils.toggleLanguage(context)
                        (context as? android.app.Activity)?.recreate()
                    },
                    interactionSource = langInteraction,
                    modifier = Modifier.tvFocusItem("settings-language")
                        .size(48.dp).tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = stringResource(R.string.change_language),
                        tint = if (isLangFocused)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                navController?.let {
                    val favInteraction = remember { MutableInteractionSource() }
                    val isFavFocused by favInteraction.collectIsFocusedAsState()
                    IconButton(
                        onClick = { navController.navigate(AppScreens.Favorites.route) },
                        interactionSource = favInteraction,
                        modifier = Modifier.tvFocusItem("settings-favorites")
                            .size(48.dp).tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (isFavFocused)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ---------- Theme settings -> own page ----------
        item(key = "settings-theme") {
            SettingsNavigationCard(
                icon = Icons.Default.FormatColorFill,
                title = stringResource(R.string.theme_settings),
                summary = stringResource(R.string.theme_summary, themeModeLabel,
                    stringResource(when (themeSettings.style) {
                        VisualStyle.DEFAULT -> R.string.style_default
                        VisualStyle.GLASS -> R.string.style_glass
                        VisualStyle.NEON_GLASS -> R.string.style_neon_glass
                    })),
                onClick = { navController?.navigate(AppScreens.ThemeSettings.route) },
                modifier = Modifier.tvFocusItem("settings-theme", initial = true)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Player & subtitle settings -> own page ----------
        item(key = "settings-player") {
            SettingsNavigationCard(
                icon = Icons.Default.Subtitles,
                title = stringResource(R.string.video_player_settings),
                summary = playerSummary,
                onClick = { navController?.navigate(AppScreens.PlayerSettings.route) },
                modifier = Modifier.tvFocusItem("settings-player")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item(key = "settings-server") {
            SettingsNavigationCard(
                icon = Icons.Default.Dns,
                title = stringResource(R.string.server_settings),
                summary = serverSummary,
                onClick = { navController?.navigate(AppScreens.ServerSettings.route) },
                modifier = Modifier.tvFocusItem("settings-server")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Font settings -> own page ----------
        item(key = "settings-font") {
            SettingsNavigationCard(
                icon = Icons.Default.TextFields,
                title = stringResource(R.string.font_settings),
                summary = fontLabel,
                onClick = { navController?.navigate(AppScreens.FontSettings.route) },
                modifier = Modifier.tvFocusItem("settings-font")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Episode marks cache ----------
        item(key = "settings-episodes") {
            SettingsNavigationCard(
                icon = Icons.Default.Check,
                title = stringResource(R.string.episodes_cache),
                summary = stringResource(R.string.cache_clear_hint),
                onClick = { showClearWatchedEpisodesDialog = true },
                modifier = Modifier.tvFocusItem("settings-episodes-cache")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- About ----------
        item(key = "settings-about") {
            SettingsNavigationCard(
                icon = Icons.Default.Info,
                title = stringResource(R.string.about),
                summary = stringResource(R.string.about_description),
                onClick = { navController?.navigate(AppScreens.About.route) },
                modifier = Modifier.tvFocusItem("settings-about")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Check for updates ----------
        item(key = "settings-updates") {
            SettingsNavigationCard(
                icon = Icons.Default.Download,
                title = stringResource(R.string.check_updates),
                summary = if (updateState.checking)
                    stringResource(R.string.checking_updates)
                else
                    stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
                onClick = { updates.checkForUpdates() },
                modifier = Modifier.tvFocusItem("settings-check-updates")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Automatic launch checks (one attempt per 12 hours) ----------
        item(key = "settings-automatic-checks") {
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Card(onClick = { updates.setAutomaticChecks(!automaticUpdates) },
                interactionSource = interaction,
                modifier = Modifier.tvFocusItem("settings-auto-updates")
                    .fillMaxWidth().tvBringIntoViewOnFocus().settingsFocusBorder(focused),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.update_auto_checks),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.update_auto_checks_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = automaticUpdates, onCheckedChange = null)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ---------- Reset to defaults ----------
        item(key = "settings-reset") {
            SettingsNavigationCard(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.reset_defaults),
                summary = stringResource(R.string.reset_defaults_hint),
                onClick = { showResetDialog = true },
                modifier = Modifier.tvFocusItem("settings-reset")
            )
        }

        item(key = "settings-bottom-space") { Spacer(modifier = Modifier.height(24.dp)) }
    }

    // ---------- Dialogs ----------
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(R.string.reset_settings_title)) },
            text = { Text(stringResource(R.string.reset_settings_message)) },
            confirmButton = {
                TextButton(onClick = {
                    resetToDefaults()
                    showResetDialog = false
                }, modifier = Modifier.tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearWatchedEpisodesDialog) {
        AlertDialog(
            onDismissRequest = { showClearWatchedEpisodesDialog = false },
            title = { Text(text = stringResource(R.string.clear_watched_title)) },
            text = { Text(stringResource(R.string.clear_watched_message)) },
            confirmButton = {
                TextButton(onClick = {
                    clearWatchedEpisodes()
                    showClearWatchedEpisodesDialog = false
                }, modifier = Modifier.tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearWatchedEpisodesDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}
