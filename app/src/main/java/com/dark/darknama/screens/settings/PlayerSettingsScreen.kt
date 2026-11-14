package com.dark.darknama.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dark.darknama.R
import com.dark.darknama.components.PlayerSeekBar
import com.dark.darknama.components.SettingsColorSwatch
import com.dark.darknama.components.SettingsOptionRow
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsStepperRow
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.ui.settings.PlayerSettingsViewModel
import com.dark.darknama.ui.theme.colorOptions
import com.dark.darknama.ui.theme.colorOptionNames
import com.dark.darknama.utils.StorageUtils

/** Subtitle text size bounds shared with the in-player settings dialog. */
const val SUBTITLE_TEXT_SIZE_MIN = 10f
const val SUBTITLE_TEXT_SIZE_MAX = 60f
const val SUBTITLE_TEXT_SIZE_STEP = 1f

/** Seek-time bounds for the double-tap / D-pad seek. */
private const val SEEK_MIN = 5
private const val SEEK_MAX = 60
private const val SEEK_STEP = 5

/**
 * Full-page Video player & subtitle settings.
 *
 * Uses +/- steppers instead of sliders so it can be adjusted precisely
 * with an Android TV remote, and shows a live subtitle preview so the
 * chosen size/colour is immediately visible.
 */
@Composable
fun PlayerSettingsScreen(navController: NavController?) {
    val context = LocalContext.current
    var subtitleSettings by remember { mutableStateOf(StorageUtils.loadSubtitleSettings(context)) }
    val playerViewModel: PlayerSettingsViewModel = viewModel()
    val playerSettings = playerViewModel.settings
    val colorRows = remember { colorOptions.withIndex().chunked(5) }

    fun updateSubtitle(newSettings: SubtitleSettings) {
        subtitleSettings = newSettings
        StorageUtils.saveSubtitleSettings(context, newSettings)
    }

    fun updatePlayer(newSettings: VideoPlayerSettings) {
        playerViewModel.update(newSettings)
    }

    SettingsSubPage(
        title = stringResource(R.string.video_player_settings),
        navController = navController,
        icon = Icons.Default.Subtitles
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---------- Seek time ----------
            SettingsSectionTitle(stringResource(R.string.player_seek_section))
            SettingsStepperRow(
                label = stringResource(R.string.seek_time, playerSettings.seekTimeSeconds),
                valueText = "${playerSettings.seekTimeSeconds}s",
                focusKey = "player-seek-step",
                canDecrease = playerSettings.seekTimeSeconds > SEEK_MIN,
                canIncrease = playerSettings.seekTimeSeconds < SEEK_MAX,
                onDecrease = {
                    updatePlayer(
                        playerSettings.copy(
                            seekTimeSeconds = (playerSettings.seekTimeSeconds - SEEK_STEP)
                                .coerceIn(SEEK_MIN, SEEK_MAX)
                        )
                    )
                },
                onIncrease = {
                    updatePlayer(
                        playerSettings.copy(
                            seekTimeSeconds = (playerSettings.seekTimeSeconds + SEEK_STEP)
                                .coerceIn(SEEK_MIN, SEEK_MAX)
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ---------- Normal progress bar color ----------
            SettingsSectionTitle(stringResource(R.string.progress_color))
            SettingsOptionRow(
                label = stringResource(R.string.theme_use_default_color),
                isSelected = playerSettings.progressColor == VideoPlayerSettings.DEFAULT_PROGRESS_COLOR,
                onSelect = { updatePlayer(playerSettings.copy(
                    progressColor = VideoPlayerSettings.DEFAULT_PROGRESS_COLOR
                )) }
            )
            colorRows.forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowColors.forEach { (index, color) ->
                        val name = stringResource(colorOptionNames[index])
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(
                                color = color,
                                isSelected = playerSettings.progressColor == color.toArgb(),
                                onSelect = { updatePlayer(playerSettings.copy(progressColor = color.toArgb())) },
                                focusKey = "player-progress-color-${color.toArgb()}",
                                swatchSize = 40, label = name,
                                accessibilityLabel = "${stringResource(R.string.progress_color)}: $name"
                            )
                        }
                    }
                }
            }
            TextButton(onClick = playerViewModel::resetProgressBar,
                modifier = Modifier.tvFocusItem("player-progress-reset").tvFocusBorder(CircleShape)) {
                Text(stringResource(R.string.reset_progress_settings))
            }
            SettingsSectionTitle(stringResource(R.string.progress_preview))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101010))
                    .padding(horizontal = 12.dp)
            ) {
                PlayerSeekBar(
                    positionMs = 35_000L,
                    bufferedPositionMs = 68_000L,
                    durationMs = 100_000L,
                    color = Color(playerSettings.progressColor),
                    onSeekStart = {},
                    onSeekPreview = {},
                    onSeekTo = {},
                    onSeekFinished = {},
                    enabled = false
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---------- Subtitle size ----------
            SettingsSectionTitle(stringResource(R.string.subtitle_settings))
            SettingsStepperRow(
                label = stringResource(R.string.subtitle_text_size, subtitleSettings.textSize.toInt()),
                valueText = "${subtitleSettings.textSize.toInt()} sp",
                focusKey = "subtitle-size-step",
                canDecrease = subtitleSettings.textSize > SUBTITLE_TEXT_SIZE_MIN,
                canIncrease = subtitleSettings.textSize < SUBTITLE_TEXT_SIZE_MAX,
                onDecrease = {
                    updateSubtitle(
                        subtitleSettings.copy(
                            textSize = (subtitleSettings.textSize - SUBTITLE_TEXT_SIZE_STEP)
                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN, SUBTITLE_TEXT_SIZE_MAX)
                        )
                    )
                },
                onIncrease = {
                    updateSubtitle(
                        subtitleSettings.copy(
                            textSize = (subtitleSettings.textSize + SUBTITLE_TEXT_SIZE_STEP)
                                .coerceIn(SUBTITLE_TEXT_SIZE_MIN, SUBTITLE_TEXT_SIZE_MAX)
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Subtitle text colour ----------
            Text(
                text = stringResource(R.string.subtitle_text_color),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            listOf(
                Color.Yellow, Color.White, Color.Black,
                Color.Red, Color.Blue, Color.Green, Color.Cyan
            ).chunked(4).forEach { rowColors ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowColors.forEach { color ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(
                                color = color,
                                isSelected = subtitleSettings.textColor == color.toArgb(),
                                onSelect = { updateSubtitle(subtitleSettings.copy(textColor = color.toArgb())) },
                                focusKey = "subtitle-text-color-${color.toArgb()}",
                                swatchSize = 40
                            )
                        }
                    }
                    repeat(4 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Subtitle outline / background colour ----------
            Text(
                text = stringResource(R.string.subtitle_background_color),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            val glass = Color.Black.copy(alpha = 0.5f)
            listOf(
                Triple(Color.Transparent, stringResource(R.string.color_empty), true),
                Triple(glass, stringResource(R.string.color_glass), false),
                Triple(Color.Black, null, false), Triple(Color.White, null, false),
                Triple(Color.Red, null, false), Triple(Color.Blue, null, false)
            ).chunked(4).forEach { rowColors ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    rowColors.forEach { (color, label, empty) ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            SettingsColorSwatch(
                                color = color,
                                isSelected = subtitleSettings.borderColor == color.toArgb(),
                                onSelect = { updateSubtitle(subtitleSettings.copy(borderColor = color.toArgb())) },
                                label = label,
                                swatchSize = 40,
                                showEmptyIndicator = empty,
                                focusKey = "subtitle-background-color-${color.toArgb()}"
                            )
                        }
                    }
                    repeat(4 - rowColors.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---------- Live preview ----------
            SettingsSectionTitle(stringResource(R.string.subtitle_preview))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF101010))
                    .padding(vertical = 24.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.subtitle_preview_text),
                    color = Color(subtitleSettings.textColor),
                    fontSize = subtitleSettings.textSize.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(subtitleSettings.borderColor))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.subtitle_size_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
