package com.dark.darknama

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import kotlin.math.roundToInt
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import com.dark.darknama.components.HoldSeekThrottle
import com.dark.darknama.components.holdSeekStep
import com.dark.darknama.components.steppedSeekPosition
import com.dark.darknama.components.PlayerSeekBar
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.ui.theme.DarkNamaTheme
import com.dark.darknama.ui.theme.ThemeManager
import androidx.compose.material3.Text
import com.dark.darknama.ui.theme.PlayerAlertDialog as AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.data.model.WatchedEpisode
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.LanguageUtils
import com.dark.darknama.utils.PlaybackSupport
import com.dark.darknama.utils.StorageUtils
import com.dark.darknama.ui.theme.FontManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Applies the user's subtitle settings (size + colours + font) to the PlayerView.
 *
 * IMPORTANT: media3's SubtitleView honours the *embedded* styles/sizes that come
 * with the subtitle track by default, which silently overrides any size we set.
 * `setApplyEmbeddedStyles(false)` / `setApplyEmbeddedFontSizes(false)` must be
 * disabled first, otherwise the "text size" setting appears to do nothing.
 */
fun PlayerView.applySubtitleSettings(settings: SubtitleSettings, typeface: Typeface? = null) {
    val sv = subtitleView ?: return

    // 1) Stop the track's own styling from overriding ours.
    sv.setApplyEmbeddedStyles(false)
    sv.setApplyEmbeddedFontSizes(false)

    // 2) Size - convert sp to px against the current display metrics.
    val pixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        settings.textSize,
        context.resources.displayMetrics
    )
    sv.setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)

    // 3) Colours + typeface.
    // borderColor doubles as the subtitle background/outline colour:
    //  - fully transparent -> no background, draw a black outline for legibility
    //  - otherwise         -> use it as the background box behind the text
    val isTransparentBackground = android.graphics.Color.alpha(settings.borderColor) == 0
    val style = CaptionStyleCompat(
        settings.textColor,
        if (isTransparentBackground) android.graphics.Color.TRANSPARENT else settings.borderColor,
        android.graphics.Color.TRANSPARENT, // window colour
        if (isTransparentBackground) CaptionStyleCompat.EDGE_TYPE_OUTLINE else CaptionStyleCompat.EDGE_TYPE_NONE,
        android.graphics.Color.BLACK,
        typeface
    )
    sv.setStyle(style)
}

// Kept for backwards compatibility with existing call sites.
fun PlayerView.setSubtitleTextSize(spSize: Float) {
    val pixels = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        spSize,
        context.resources.displayMetrics
    )
    subtitleView?.apply {
        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, pixels)
    }
}

class VideoPlayerActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.dark.darknama.utils.LanguageUtils.wrapContext(newBase))
    }

    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_SEASON_ID = "season_id"
        const val EXTRA_EPISODE_ID = "episode_id"
        const val EXTRA_USER_AGENT = "user_agent"
        const val EXTRA_REFERER = "referer"
        const val EXTRA_SOURCE_TYPE = "source_type"
        const val EXTRA_MOVIE_ID = "movie_id"
        const val EXTRA_TITLE = "content_title"
        const val EXTRA_IMAGE = "content_image"
        const val EXTRA_QUALITY = "source_quality"
        const val EXTRA_SEASON_TITLE = "season_title"
        const val EXTRA_EPISODE_TITLE = "episode_title"
        const val REQUEST_WRITE_SETTINGS = 1001

        fun start(
            context: Context, videoUrl: String, sourceType: String? = null,
            movieId: Int? = null, title: String? = null, image: String? = null,
            quality: String? = null
        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_SOURCE_TYPE, sourceType)
                movieId?.let { putExtra(EXTRA_MOVIE_ID, it) }
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_IMAGE, image)
                putExtra(EXTRA_QUALITY, quality)
            }
            context.startActivity(intent)
        }
        
        /**
         * Starts the player for a live TV stream, optionally with the custom
         * HTTP User-Agent / Referer headers required by some IPTV streams.
         */
        fun startLiveTv(context: Context, videoUrl: String, userAgent: String? = null, referer: String? = null) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                if (!userAgent.isNullOrBlank()) putExtra(EXTRA_USER_AGENT, userAgent)
                if (!referer.isNullOrBlank()) putExtra(EXTRA_REFERER, referer)
            }
            context.startActivity(intent)
        }
        
        fun startWithEpisodeInfo(
            context: Context, videoUrl: String, seriesId: Int, seasonId: Int, episodeId: Int,
            sourceType: String? = null, seriesTitle: String? = null, seasonTitle: String? = null,
            episodeTitle: String? = null, image: String? = null, quality: String? = null
        ) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_SOURCE_TYPE, sourceType)
                putExtra(EXTRA_SERIES_ID, seriesId)
                putExtra(EXTRA_SEASON_ID, seasonId)
                putExtra(EXTRA_EPISODE_ID, episodeId)
                putExtra(EXTRA_TITLE, seriesTitle)
                putExtra(EXTRA_SEASON_TITLE, seasonTitle)
                putExtra(EXTRA_EPISODE_TITLE, episodeTitle)
                putExtra(EXTRA_IMAGE, image)
                putExtra(EXTRA_QUALITY, quality)
            }
            context.startActivity(intent)
        }

        fun startFromHistory(context: Context, entry: PlaybackRecord) {
            if (entry.type == "movie") {
                start(context, entry.url, entry.sourceType, entry.contentId, entry.title,
                    entry.image, entry.quality)
            } else if (entry.type == "series" && entry.seasonId != null && entry.episodeId != null) {
                startWithEpisodeInfo(context, entry.url, entry.contentId, entry.seasonId,
                    entry.episodeId, entry.sourceType, entry.title, entry.seasonTitle,
                    entry.episodeTitle, entry.image, entry.quality)
            }
        }
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String? = null
    private var seriesId: Int? = null
    private var seasonId: Int? = null
    private var episodeId: Int? = null
    private var streamUserAgent: String? = null
    private var streamReferer: String? = null
    private var sourceType: String? = null
    private var playbackEntry: PlaybackRecord? = null
    private var hasPlayedMedia = false
    private var playerInitialized = false

    private fun persistPlayback(player: ExoPlayer?, ended: Boolean = false) {
        val entry = playbackEntry ?: return
        if (!hasPlayedMedia || player == null) return
        try {
            StorageUtils.savePlaybackProgress(this, entry, player.currentPosition, player.duration,
                ended || player.playbackState == Player.STATE_ENDED)
        } catch (_: Exception) {
            // History storage must never interrupt playback or closing the activity.
        }
    }
    
    // Compose-observable states shared with the composable so that
    // an Android TV remote can show controls / open the settings dialog.
    private val showControlsState = mutableStateOf(true)
    private val showSettingsDialogState = mutableStateOf(false)
    private val showSpeedDialogState = mutableStateOf(false)
    private val showPlaybackErrorState = mutableStateOf(false)
    // A monotonic timestamp, shared with Compose. +1 guarantees that two events
    // in the same clock tick still restart LaunchedEffect(lastInteraction).
    private val lastInteractionState = mutableLongStateOf(SystemClock.elapsedRealtimeNanos())
    private var swallowedRevealKey: Int? = null
    private var swallowedBackKey = false
    private var remoteSeekTimeSeconds = 10
    private val isTv by lazy { DeviceUtils.isTv(this) }
    private val holdSeekThrottle = HoldSeekThrottle()
    private var activeRemoteSeekKey: Int? = null

    private fun markPlayerInteraction() {
        lastInteractionState.longValue = maxOf(
            SystemClock.elapsedRealtimeNanos(), lastInteractionState.longValue + 1L
        )
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Also sees touches on child controls and the seek bar, not just video taps.
        markPlayerInteraction()
        return super.dispatchTouchEvent(ev)
    }

    private fun isRevealOnlyKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE -> true
        else -> false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Called BEFORE focused Compose controls can consume a key, unlike
        // onKeyDown. In particular the first OK from a hidden overlay may not
        // reach onKeyDown if an exiting button still has focus for one frame.
        markPlayerInteraction()
        if (isTv && event.action == KeyEvent.ACTION_UP) {
            holdSeekThrottle.reset(event.keyCode)
            if (activeRemoteSeekKey == event.keyCode) activeRemoteSeekKey = null
        }
        // A hidden-overlay seek reveals the controls, but the same held key must
        // continue seeking instead of being redirected to the newly focused UI.
        if (isTv && event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0 &&
            activeRemoteSeekKey == event.keyCode) return onKeyDown(event.keyCode, event)
        if (swallowedRevealKey == event.keyCode) {
            if (event.action == KeyEvent.ACTION_UP) swallowedRevealKey = null
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && isTv && !showControlsState.value &&
            !showSettingsDialogState.value && !showSpeedDialogState.value &&
            !showPlaybackErrorState.value && exoPlayer != null && isRevealOnlyKey(event.keyCode)
        ) {
            showControlsState.value = true
            swallowedRevealKey = event.keyCode
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set fullscreen landscape mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        // Enable immersive full-screen mode
        enableFullScreenMode()
        
        // Keep screen on while in video player
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Load the user-configured seek time so the TV remote uses the same value
        remoteSeekTimeSeconds = try {
            StorageUtils.loadVideoPlayerSettings(this).seekTimeSeconds
        } catch (e: Exception) {
            10
        }
        
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        seriesId = intent.getIntExtra(EXTRA_SERIES_ID, -1).takeIf { it != -1 }
        seasonId = intent.getIntExtra(EXTRA_SEASON_ID, -1).takeIf { it != -1 }
        episodeId = intent.getIntExtra(EXTRA_EPISODE_ID, -1).takeIf { it != -1 }
        streamUserAgent = intent.getStringExtra(EXTRA_USER_AGENT)
        streamReferer = intent.getStringExtra(EXTRA_REFERER)
        sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE)

        // A catalogue identity is required: live TV and bare URLs are not history items.
        val movieId = intent.getIntExtra(EXTRA_MOVIE_ID, -1).takeIf { it > 0 }
        playbackEntry = when {
            movieId != null -> PlaybackRecord(
                type = "movie", contentId = movieId,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                image = intent.getStringExtra(EXTRA_IMAGE).orEmpty(),
                url = videoUrl.orEmpty(), quality = intent.getStringExtra(EXTRA_QUALITY).orEmpty(),
                sourceType = sourceType
            )
            seriesId != null && seasonId != null && episodeId != null -> PlaybackRecord(
                type = "series", contentId = seriesId!!,
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                image = intent.getStringExtra(EXTRA_IMAGE).orEmpty(),
                url = videoUrl.orEmpty(), quality = intent.getStringExtra(EXTRA_QUALITY).orEmpty(),
                sourceType = sourceType, seasonId = seasonId,
                seasonTitle = intent.getStringExtra(EXTRA_SEASON_TITLE),
                episodeId = episodeId, episodeTitle = intent.getStringExtra(EXTRA_EPISODE_TITLE)
            )
            else -> null
        }

        if (videoUrl != null) {
            // The player runs in a separate Activity: pass the user's saved theme into
            // its Compose tree so dialogs/pickers inherit the same Material/glass/neon layer.
            val themeSettings = ThemeManager(this).loadThemeSettings()
            val fontSettings = StorageUtils.loadFontSettings(this)
            setContent {
                DarkNamaTheme(themeSettings, fontSettings, showStyleBackdrop = false) {
                    VideoPlayerScreen(
                        videoUrl = videoUrl!!,
                        seriesId = seriesId,
                        seasonId = seasonId,
                        episodeId = episodeId,
                        userAgent = streamUserAgent,
                        referer = streamReferer,
                        sourceType = sourceType,
                        playbackEntry = playbackEntry,
                        externalShowControls = showControlsState,
                        externalShowSettingsDialog = showSettingsDialogState,
                        externalShowSpeedDialog = showSpeedDialogState,
                        externalShowPlaybackError = showPlaybackErrorState,
                        externalLastInteraction = lastInteractionState,
                        onPlaybackStarted = { hasPlayedMedia = true },
                        onSavePlayback = { player, ended -> persistPlayback(player, ended) },
                        onBack = this::finish
                    ) { player ->
                        exoPlayer = player
                        playerInitialized = true
                    }
                }
            }
        } else {
            finish()
        }
    }
    
    // Handle remote keys even when the Compose controls have left the focus tree.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        markPlayerInteraction()
        if (swallowedRevealKey == keyCode) return true // A held reveal key must not click.
        try {
            if (showSettingsDialogState.value || showSpeedDialogState.value) {
                return super.onKeyDown(keyCode, event)
            }
            if (showPlaybackErrorState.value) {
                // Keep Retry / Back focusable; an error is never auto-hidden.
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_CENTER -> return super.onKeyDown(keyCode, event)
                    KeyEvent.KEYCODE_BACK -> {
                        swallowedBackKey = true
                        finish()
                        return true
                    }
                }
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                swallowedBackKey = true
                if (showControlsState.value && exoPlayer != null) showControlsState.value = false
                else finish()
                return true
            }
            exoPlayer?.let { player ->
                if (isTv) {
                    // The first key from a hidden overlay only reveals it. Left/right
                    // and the dedicated rewind/forward keys are deliberate seek gestures.
                    // Consume the corresponding key-up too (Compose clicks on key-up).
                    if (!showControlsState.value && isRevealOnlyKey(keyCode)) {
                        showControlsState.value = true
                        swallowedRevealKey = keyCode
                        return true
                    }
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> return super.onKeyDown(keyCode, event)
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT ->
                            if (showControlsState.value && activeRemoteSeekKey != keyCode)
                                return super.onKeyDown(keyCode, event)
                    }
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_DPAD_CENTER -> {
                        player.playWhenReady = !player.playWhenReady
                        showControlsState.value = true
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> {
                        player.playWhenReady = true
                        showControlsState.value = true
                        return true
                    }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        player.playWhenReady = false
                        showControlsState.value = true
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND,
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        if (!player.isCurrentMediaItemSeekable) {
                            showControlsState.value = true
                            return true
                        }
                        if (!isTv) {
                            // Keep the existing phone key behavior unchanged.
                            val seekMs = remoteSeekTimeSeconds * 1000L
                            val position = player.currentPosition.coerceAtLeast(0L)
                            val newPosition = if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                                (position - seekMs).coerceAtLeast(0L)
                            } else {
                                val end = player.duration.takeIf { it > 0L }
                                (position + seekMs).let { if (end != null) it.coerceAtMost(end) else it }
                            }
                            player.seekTo(newPosition)
                        } else {
                            activeRemoteSeekKey = keyCode
                            val forward = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                                keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                            val repeatCount = event?.repeatCount ?: 0
                            val duration = player.duration.takeIf { it > 0L }
                            if (duration != null && holdSeekThrottle.shouldSeek(keyCode, repeatCount,
                                    SystemClock.uptimeMillis())) {
                                val seekMs = holdSeekStep(remoteSeekTimeSeconds * 1000L, repeatCount)
                                player.seekTo(steppedSeekPosition(player.currentPosition, duration, seekMs, forward))
                            }
                        }
                        showControlsState.value = true
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_MENU -> {
                        showControlsState.value = true
                        showSettingsDialogState.value = true
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        showControlsState.value = !showControlsState.value
                        return true
                    }
                }
            }
        } catch (_: Exception) {
            // The player may have been released as the activity closes.
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        markPlayerInteraction()
        if (keyCode == KeyEvent.KEYCODE_BACK && swallowedBackKey) {
            swallowedBackKey = false
            return true
        }
        if (swallowedRevealKey == keyCode) {
            swallowedRevealKey = null
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
    
    private fun enableFullScreenMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // For Android 11 and above
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // For Android 4.4 to Android 10
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
            } else {
                // For even older versions
                @Suppress("DEPRECATION")
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
        } catch (e: Exception) {
            // Fallback to basic fullscreen if there's an issue
            try {
                @Suppress("DEPRECATION")
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } catch (e2: Exception) {
                // Ignore fullscreen errors
            }
        }
    }
    
    override fun onDestroy() {
        persistPlayback(exoPlayer)
        super.onDestroy()
        // VideoPlayerScreen's DisposableEffect owns and releases the player.
        exoPlayer = null
        playerInitialized = false
        
        // Remove keep screen on flag to conserve battery
        try {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (e: Exception) {
            // Ignore flag clear errors
        }
    }
    
    override fun onResume() {
        super.onResume()
        enableFullScreenMode()
        markPlayerInteraction() // Start a fresh countdown if playback resumes.
        // Do not pause on the initial resume: it races with the player's autoplay.
        // onPause() already pauses playback when returning from the background.
    }

    override fun onPause() {
        // Save the real player position before pausing (including when the app backgrounds).
        showControlsState.value = true
        swallowedRevealKey = null
        swallowedBackKey = false
        try {
            if (playerInitialized && exoPlayer != null) {
                persistPlayback(exoPlayer)
                exoPlayer?.playWhenReady = false
            }
        } catch (_: Exception) {
            // The player may already have been released.
        }
        super.onPause()
    }
}

private fun playbackHttpStatus(error: Throwable): Int? {
    var cause: Throwable? = error
    while (cause != null) {
        if (cause is HttpDataSource.InvalidResponseCodeException) return cause.responseCode
        cause = cause.cause
    }
    return null
}

private fun playbackErrorMessage(context: Context, error: PlaybackException): String {
    val status = playbackHttpStatus(error)
    return when {
        status != null -> context.getString(R.string.player_http_error, status)
        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
            error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT ->
            context.getString(R.string.player_network_error)
        error.errorCode == PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
            context.getString(R.string.player_cleartext_error)
        error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ->
            context.getString(R.string.player_invalid_content_error)
        error.errorCode in 3000..4999 -> context.getString(R.string.player_format_error)
        else -> context.getString(R.string.player_unknown_error, error.errorCode)
    }
}

private fun prepareMediaAgain(player: ExoPlayer, item: MediaItem) {
    // Do not seek into an unseekable live stream, or into a failed initial load.
    val resumeAt = player.currentPosition.takeIf {
        it > 0L && player.isCurrentMediaItemSeekable
    } ?: C.TIME_UNSET
    player.setMediaItem(item, resumeAt)
    player.prepare()
}

@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    seriesId: Int?,
    seasonId: Int?,
    episodeId: Int?,
    userAgent: String? = null,
    referer: String? = null,
    sourceType: String? = null,
    playbackEntry: PlaybackRecord? = null,
    externalShowControls: androidx.compose.runtime.MutableState<Boolean>? = null,
    externalShowSettingsDialog: androidx.compose.runtime.MutableState<Boolean>? = null,
    externalShowSpeedDialog: androidx.compose.runtime.MutableState<Boolean>? = null,
    externalShowPlaybackError: androidx.compose.runtime.MutableState<Boolean>? = null,
    externalLastInteraction: MutableLongState? = null,
    onPlaybackStarted: () -> Unit = {},
    onSavePlayback: (ExoPlayer, Boolean) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onPlayerReady: (ExoPlayer) -> Unit
) {
    val context = LocalContext.current
    val isTv = remember(context) { DeviceUtils.isTv(context) }
    val scope = rememberCoroutineScope()
    val playFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val speedFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    // Only unfinished items resume. Finished items replay from the beginning.
    var pendingResumeMs by remember(playbackEntry?.key, videoUrl) {
        mutableStateOf(playbackEntry?.let { StorageUtils.playbackFor(context, it.key) }
            ?.takeUnless { it.isFinished }?.positionMs?.takeIf { it > 0L })
    }
    var isPlaying by remember { mutableStateOf(true) } // Desired playWhenReady state
    var currentPosition by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    val showControlsHolder = externalShowControls ?: remember { mutableStateOf(true) }
    var showControls by showControlsHolder
    val interactionHolder = externalLastInteraction ?: remember {
        mutableLongStateOf(SystemClock.elapsedRealtimeNanos())
    }
    var lastInteraction by interactionHolder
    fun markInteraction() {
        lastInteraction = maxOf(SystemClock.elapsedRealtimeNanos(), lastInteraction + 1L)
    }
    var isSeeking by remember { mutableStateOf(false) }
    var actuallyPlaying by remember { mutableStateOf(false) } // Player.isPlaying, not playWhenReady
    var playerError by remember { mutableStateOf<PlaybackException?>(null) }
    var isRetrying by remember { mutableStateOf(false) }
    var autoRetryCount by remember { mutableStateOf(0) }
    val retryFocusRequester = remember { FocusRequester() }
    var showForwardIndicator by remember { mutableStateOf(false) }
    var showRewindIndicator by remember { mutableStateOf(false) }
    var wasPlayingBeforeSeek by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var speedBeforeHold by remember { mutableStateOf(1.0f) }
    var isHoldingForSpeed by remember { mutableStateOf(false) }
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var brightnessLevel by remember(context) {
        mutableStateOf(try {
            (context as? Activity)?.window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                ?: Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (_: Exception) { 0.5f })
    }
    var volumeLevel by remember(audioManager) {
        mutableStateOf(try {
            val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat() / max
        } catch (_: Exception) { 0.5f })
    }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var brightnessInteraction by remember { mutableLongStateOf(0L) }
    var volumeInteraction by remember { mutableLongStateOf(0L) }
    val showSpeedDropdownHolder = externalShowSpeedDialog ?: remember { mutableStateOf(false) }
    var showSpeedDropdown by showSpeedDropdownHolder
    var hasMarkedAsWatched by remember { mutableStateOf(false) }

    // Track selection state
    val showTrackSelectionDialogHolder = externalShowSettingsDialog ?: remember { mutableStateOf(false) }
    var showTrackSelectionDialog by showTrackSelectionDialogHolder
    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var trackSelector by remember { mutableStateOf<DefaultTrackSelector?>(null) }
    val controlsInteractive = showControls && (!isTv || playerError == null || isRetrying)

    // On phones and predictive-back devices too: Back hides first, then exits.
    // Dialogs handle their own Back; the error panel retains its Back action.
    BackHandler(enabled = showControls && !showTrackSelectionDialog &&
        !showSpeedDropdown && playerError == null) {
        markInteraction()
        showControls = false
    }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(isTv, controlsInteractive) {
        if (isTv && !controlsInteractive) focusManager.clearFocus(force = true)
    }

    // The player is a separate activity: enter the overlay on a real OK-action, and
    // restore the launching button when a popup is dismissed with Back or OK.
    LaunchedEffect(isTv, showControls, playerError) {
        if (isTv && showControls && playerError == null &&
            !showTrackSelectionDialog && !showSpeedDropdown) {
            withFrameNanos { }
            try { playFocusRequester.requestFocus() } catch (_: IllegalStateException) { }
        }
    }
    fun restorePlayerControl(requester: FocusRequester) {
        if (isTv) scope.launch {
            withFrameNanos { }
            try { requester.requestFocus() } catch (_: IllegalStateException) { }
        }
    }

    // Predefined playback speed options
    val speedOptions = remember {
        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 3.5f)
    }
    
    // Load font settings
    val fontSettings = remember(context) {
        try {
            StorageUtils.loadFontSettings(context)
        } catch (e: Exception) {
            com.dark.darknama.data.model.FontSettings.DEFAULT
        }
    }
    
    // Media3 subtitles and the Compose player labels use the same locale-aware
    // family as the app. A missing optional TTF safely uses system glyph fallback.
    val appLanguage = LanguageUtils.getSavedLanguage(context)
    val playerFontFamily = FontManager.loadFontFamily(context, fontSettings, appLanguage)
    val customTypeface = remember(context, fontSettings, appLanguage) {
        FontManager.loadSubtitleTypeface(context, fontSettings, appLanguage)
    }
    
    // Load video player settings (without affecting playback speed)
    val videoPlayerSettings = remember(context) {
        try {
            StorageUtils.loadVideoPlayerSettings(context)
        } catch (e: Exception) {
            com.dark.darknama.data.model.VideoPlayerSettings.DEFAULT
        }
    }
    
    // Load subtitle settings (mutable so they can be changed from the in-player settings dialog)
    var subtitleSettings by remember(context) {
        mutableStateOf(
            try {
                StorageUtils.loadSubtitleSettings(context)
            } catch (e: Exception) {
                SubtitleSettings.getDefaultSettings(context)
            }
        )
    }
    
    // Update and persist subtitle settings so the change is shared with the Settings screen
    fun updateSubtitleSettings(newSettings: SubtitleSettings) {
        markInteraction()
        subtitleSettings = newSettings
        try {
            StorageUtils.saveSubtitleSettings(context, newSettings)
        } catch (e: Exception) {
            // Ignore storage errors
        }
    }
    
    val uri = remember(videoUrl) { Uri.parse(videoUrl.trim()) }
    val validUrl = (uri.scheme.equals("https", ignoreCase = true) ||
        uri.scheme.equals("http", ignoreCase = true)) && !uri.host.isNullOrBlank()
    val mediaItem = remember(videoUrl, sourceType) {
        if (validUrl) MediaItem.Builder().setUri(uri).apply {
            PlaybackSupport.mimeType(sourceType)?.let { setMimeType(it) }
        }.build() else null
    }

    val exoPlayer = remember(context, videoUrl, sourceType, userAgent, referer) {
        if (mediaItem == null) null else try {
            val selector = DefaultTrackSelector(context)
            trackSelector = selector

            // Use the same headers for the stream, redirects and HLS segments.
            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(30000)
                .setReadTimeoutMs(30000)
            if (!userAgent.isNullOrBlank()) {
                httpDataSourceFactory.setUserAgent(userAgent)
            }
            if (!referer.isNullOrBlank()) {
                httpDataSourceFactory.setDefaultRequestProperties(mapOf("Referer" to referer))
            }
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

            ExoPlayer.Builder(context)
                .setTrackSelector(selector)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Unable to create player: ${e.javaClass.simpleName}")
            null
        }
    }

    val trackListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
            }
        }
    }

    // Keep the desired play/pause state in sync (including TV media keys).
    LaunchedEffect(isPlaying, exoPlayer) {
        try {
            exoPlayer?.playWhenReady = isPlaying
        } catch (_: Exception) {
            // The player may have been released while leaving the activity.
        }
    }
    
    // Update playback speed when it changes
    LaunchedEffect(playbackSpeed, exoPlayer) {
        try {
            exoPlayer?.setPlaybackSpeed(playbackSpeed)
        } catch (e: Exception) {
            // Ignore playback speed errors
        }
    }
    
    // Listen to player events and handle cleanup
    val playerListener = remember(exoPlayer) {
        object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                // playWhenReady alone stays true during buffering. The timer only
                // runs while Player.isPlaying is true, and restarts after a stall.
                actuallyPlaying = playing
                if (playing) {
                    markInteraction()
                    onPlaybackStarted()
                    exoPlayer?.let { onSavePlayback(it, false) }
                } else {
                    showControls = true
                }
                // Only mark an episode watched after media actually starts playing.
                if (playing && !hasMarkedAsWatched && seriesId != null && seasonId != null && episodeId != null) {
                    try {
                        StorageUtils.saveWatchedEpisode(context, WatchedEpisode(seriesId, seasonId, episodeId))
                        hasMarkedAsWatched = true
                    } catch (_: Exception) {
                        // A storage failure must not interrupt playback.
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // Also reflect TV/media-key changes made directly on the ExoPlayer.
                if (!isSeeking) isPlaying = playWhenReady
                if (!playWhenReady) {
                    showControls = true
                    exoPlayer?.let { onSavePlayback(it, false) }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    if (playbackState == Player.STATE_READY) {
                        duration = exoPlayer?.duration?.takeIf { it > 0L } ?: 0L
                        bufferedPosition = exoPlayer?.bufferedPosition?.coerceAtLeast(0L) ?: 0L
                        // Seek only once the media is ready and known to be seekable.
                        val resumeAt = pendingResumeMs
                        if (resumeAt != null) {
                            pendingResumeMs = null
                            exoPlayer?.let { player ->
                                if (player.isCurrentMediaItemSeekable) {
                                    val target = if (duration > 0L)
                                        resumeAt.coerceAtMost((duration - 1_000L).coerceAtLeast(0L))
                                    else resumeAt
                                    if (target > 0L) {
                                        player.seekTo(target)
                                        currentPosition = target
                                        Toast.makeText(context, context.getString(R.string.resuming_at,
                                            formatTime(target)), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        // Keep the error visible until Media3 actually loads a playable stream.
                        isRetrying = false
                        playerError = null
                    } else if (playbackState == Player.STATE_ENDED) {
                        exoPlayer?.let { onSavePlayback(it, true) }
                        isPlaying = false
                        showControls = true
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        showControls = true
                    }
                } catch (e: Exception) {
                    // Ignore duration errors
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                try {
                    if (!isSeeking) {
                        currentPosition = exoPlayer?.currentPosition ?: 0L
                    }
                } catch (e: Exception) {
                    // Ignore position errors
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                val status = playbackHttpStatus(error)
                Log.w("VideoPlayerActivity", "Playback failed: code=${error.errorCode}, HTTP=$status")
                playerError = error
                isRetrying = autoRetryCount < PlaybackSupport.MAX_AUTO_RETRIES &&
                    PlaybackSupport.shouldAutoRetry(error.errorCode, status)
                showControls = true
            }
        }
    }
    
    // Register listeners before prepare(), so even a fast media failure is reported.
    DisposableEffect(exoPlayer, mediaItem) {
        val player = exoPlayer
        val item = mediaItem
        if (player != null && item != null) {
            player.addListener(trackListener)
            player.addListener(playerListener)
            try {
                onPlayerReady(player)
                player.setMediaItem(item)
                player.playWhenReady = isPlaying
                player.setPlaybackSpeed(playbackSpeed)
                player.prepare()
            } catch (e: Exception) {
                Log.e("VideoPlayerActivity", "Unable to prepare player: ${e.javaClass.simpleName}")
                playerError = PlaybackException("Unable to prepare player", e, PlaybackException.ERROR_CODE_UNSPECIFIED)
                showControls = true
            }
        }
        onDispose {
            player?.let { onSavePlayback(it, false) }
            player?.removeListener(trackListener)
            player?.removeListener(playerListener)
            player?.release()
        }
    }

    // A failed load is retried at most twice. This effect is canceled on a new error,
    // successful load, source change or when the player screen is closed.
    LaunchedEffect(playerError, exoPlayer, mediaItem) {
        val error = playerError ?: return@LaunchedEffect
        val player = exoPlayer ?: return@LaunchedEffect
        val item = mediaItem ?: return@LaunchedEffect
        if (autoRetryCount >= PlaybackSupport.MAX_AUTO_RETRIES ||
            !PlaybackSupport.shouldAutoRetry(error.errorCode, playbackHttpStatus(error))
        ) {
            isRetrying = false
            return@LaunchedEffect
        }
        isRetrying = true
        delay(2000L * (autoRetryCount + 1))
        autoRetryCount++
        try {
            prepareMediaAgain(player, item)
            player.playWhenReady = isPlaying
            // Keep showing "Retrying" until STATE_READY or the next failure.
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Unable to retry player: ${e.javaClass.simpleName}")
            autoRetryCount = PlaybackSupport.MAX_AUTO_RETRIES
            isRetrying = false
        }
    }

    // The TV remote must focus a real button when automatic retries are exhausted.
    LaunchedEffect(playerError, isRetrying) {
        val showError = playerError != null && !isRetrying
        externalShowPlaybackError?.value = showError
        if (showError && exoPlayer != null) {
            withFrameNanos { }
            try { retryFocusRequester.requestFocus() } catch (_: IllegalStateException) { }
        }
    }

    // A server that accepts a connection but never supplies playable media must not
    // leave "Retrying" on screen indefinitely either.
    LaunchedEffect(playerError, isRetrying, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (playerError != null && isRetrying) {
            delay(45_000L)
            if (isRetrying && player.playbackState != Player.STATE_READY) {
                autoRetryCount = PlaybackSupport.MAX_AUTO_RETRIES
                isRetrying = false
                showControls = true
                player.stop()
            }
        }
    }

    // Persist the actual player position periodically; never persist a scrubbing preview.
    LaunchedEffect(exoPlayer, playbackEntry) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (playbackEntry == null) return@LaunchedEffect
        while (true) {
            delay(5_000L)
            if (player.isPlaying) onSavePlayback(player, false)
        }
    }

    // Keep position and buffered progress in sync, including when paused or buffering.
    // Poll less often when controls are hidden; scrubbing owns the preview position.
    LaunchedEffect(exoPlayer, showControls, isSeeking) {
        val player = exoPlayer ?: return@LaunchedEffect
        while (true) {
            delay(if (showControls) 250L else 1000L)
            if (!isSeeking) {
                try {
                    currentPosition = player.currentPosition.coerceAtLeast(0L)
                    bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
                    duration = player.duration.takeIf { it > 0L } ?: 0L
                } catch (_: IllegalStateException) {
                    // The player may have been released while leaving the activity.
                }
            }
        }
    }
    
    // A new touch, gesture, key, seek or dialog action cancels the old timer.
    // Unlike playWhenReady, Player.isPlaying is false while paused, buffering
    // or ended. Recheck everything after delay to close a recomposition race.
    val interactionAtLaunch = lastInteraction
    LaunchedEffect(interactionAtLaunch, showControls, actuallyPlaying, isPlaying,
        isSeeking, isHoldingForSpeed, showTrackSelectionDialog, showSpeedDropdown,
        playerError, isRetrying) {
        awaitControlsAutoHide(
            canHide = {
                showControls && lastInteraction == interactionAtLaunch && isPlaying &&
                    actuallyPlaying && exoPlayer?.isPlaying == true && !isSeeking &&
                    !isHoldingForSpeed && !showTrackSelectionDialog && !showSpeedDropdown &&
                    playerError == null && !isRetrying
            },
            hide = { showControls = false }
        )
    }
    
    // Hide forward/rewind indicators after a delay
    LaunchedEffect(showForwardIndicator) {
        try {
            if (showForwardIndicator) {
                delay(500) // Hide after 500ms
                showForwardIndicator = false
            }
        } catch (e: Exception) {
            // Ignore delay errors
        }
    }
    
    LaunchedEffect(showRewindIndicator) {
        try {
            if (showRewindIndicator) {
                delay(500) // Hide after 500ms
                showRewindIndicator = false
            }
        } catch (e: Exception) {
            // Ignore delay errors
        }
    }

    // Each gesture indicator has its own countdown; revealing or fading the
    // playback controls never cancels either brightness or volume feedback.
    LaunchedEffect(brightnessInteraction) {
        if (showBrightnessIndicator) {
            delay(1_200L)
            showBrightnessIndicator = false
        }
    }
    LaunchedEffect(volumeInteraction) {
        if (showVolumeIndicator) {
            delay(1_200L)
            showVolumeIndicator = false
        }
    }

    fun retryPlayback() {
        markInteraction()
        val player = exoPlayer ?: return
        val item = mediaItem ?: return
        autoRetryCount = 0 // A deliberate tap starts a fresh, bounded attempt.
        isRetrying = true
        isPlaying = true
        try {
            prepareMediaAgain(player, item)
            player.playWhenReady = true
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Unable to retry player: ${e.javaClass.simpleName}")
            isRetrying = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                try {
                    detectTapGestures(
                        onPress = {
                            markInteraction()
                            try {
                                tryAwaitRelease()
                            } finally {
                                if (isHoldingForSpeed) {
                                    playbackSpeed = speedBeforeHold
                                    isHoldingForSpeed = false
                                    markInteraction()
                                }
                            }
                        },
                        onLongPress = {
                            markInteraction()
                            // Holding the video temporarily plays at 2x; release
                            // restores the user's previous speed, even on cancel.
                            if (exoPlayer?.isPlaying == true && !isSeeking &&
                                !showTrackSelectionDialog && !showSpeedDropdown && !isHoldingForSpeed) {
                                speedBeforeHold = playbackSpeed
                                isHoldingForSpeed = true
                                playbackSpeed = 2.0f
                            }
                        },
                        onDoubleTap = { offset ->
                            markInteraction()
                            // Calculate if the tap is on the left or right side
                            val screenWidth = size.width
                            val tapX = offset.x
                            
                            // Store the playing state before seeking
                            wasPlayingBeforeSeek = isPlaying
                            isSeeking = true
                            
                            if (tapX < screenWidth / 2) {
                                // Left side - rewind specified seconds
                                try {
                                    exoPlayer?.let { player ->
                                        val seekTimeMs = videoPlayerSettings.seekTimeSeconds * 1000L
                                        val newPosition = (player.currentPosition - seekTimeMs).coerceAtLeast(0L)
                                        if (player.isCurrentMediaItemSeekable) {
                                            player.seekTo(newPosition)
                                            currentPosition = newPosition
                                            showRewindIndicator = true
                                            // Keep playing if playback was active before seeking.
                                            if (wasPlayingBeforeSeek) player.playWhenReady = true
                                        } else {
                                            showControls = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore seek errors
                                }
                            } else {
                                // Right side - forward specified seconds
                                try {
                                    exoPlayer?.let { player ->
                                        val seekTimeMs = videoPlayerSettings.seekTimeSeconds * 1000L
                                        val end = player.duration.takeIf { it > 0L }
                                        val newPosition = (player.currentPosition.coerceAtLeast(0L) + seekTimeMs)
                                            .let { if (end != null) it.coerceAtMost(end) else it }
                                        if (player.isCurrentMediaItemSeekable) {
                                            player.seekTo(newPosition)
                                            currentPosition = newPosition
                                            showForwardIndicator = true
                                            if (wasPlayingBeforeSeek) player.playWhenReady = true
                                        } else {
                                            showControls = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Ignore seek errors
                                }
                            }
                            
                            // Reset seeking state after a short delay using a coroutine scope
                            scope.launch {
                                try {
                                    delay(500) // Reset after 500ms
                                    isSeeking = false
                                    markInteraction()
                                    // Restore the playing state after seeking is finished
                                    try {
                                        exoPlayer?.playWhenReady = wasPlayingBeforeSeek
                                        // Update isPlaying state to match the player's actual state
                                        isPlaying = wasPlayingBeforeSeek
                                    } catch (e: Exception) {
                                        // Ignore errors
                                    }
                                } catch (e: Exception) {
                                    // Ignore delay errors
                                }
                            }
                        },
                        onTap = {
                            markInteraction()
                            showControls = !showControls
                        }
                    )
                } catch (e: Exception) {
                    // Ignore gesture detection errors
                }
            }
            .pointerInput(showControls) {
                // Vertical swipes on the unobscured video adjust brightness
                // (left) or music volume (right). Keeping these off while the
                // controls are up avoids stealing seek-bar drags or button taps.
                if (!showControls) {
                    var adjustingBrightness = true
                    fun finishAdjustment() {
                        markInteraction()
                        if (adjustingBrightness) brightnessInteraction = lastInteraction
                        else volumeInteraction = lastInteraction
                    }
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            adjustingBrightness = offset.x < size.width / 2f
                            markInteraction()
                            if (adjustingBrightness) {
                                showBrightnessIndicator = true
                                brightnessInteraction = lastInteraction
                            } else {
                                try {
                                    val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        ?.coerceAtLeast(1) ?: 1
                                    volumeLevel = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        ?: 0).toFloat() / max
                                } catch (_: Exception) { }
                                showVolumeIndicator = true
                                volumeInteraction = lastInteraction
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            markInteraction()
                            val step = dragAmount / size.height.coerceAtLeast(1)
                            if (adjustingBrightness) {
                                brightnessLevel = (brightnessLevel - step).coerceIn(0f, 1f)
                                try {
                                    (context as? Activity)?.window?.let { window ->
                                        val attributes = window.attributes
                                        attributes.screenBrightness = brightnessLevel.coerceAtLeast(0.01f)
                                        window.attributes = attributes
                                    }
                                } catch (_: Exception) { }
                                brightnessInteraction = lastInteraction
                            } else {
                                volumeLevel = (volumeLevel - step).coerceIn(0f, 1f)
                                try {
                                    audioManager?.let { manager ->
                                        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        manager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                            (volumeLevel * max).roundToInt(), 0)
                                    }
                                } catch (_: Exception) { }
                                volumeInteraction = lastInteraction
                            }
                        },
                        onDragEnd = { finishAdjustment() },
                        onDragCancel = { finishAdjustment() }
                    )
                }
            }
    ) {
        if (exoPlayer == null) {
            LaunchedEffect(isTv) {
                if (isTv) {
                    withFrameNanos { }
                    try { backFocusRequester.requestFocus() } catch (_: IllegalStateException) { }
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(androidx.compose.ui.res.stringResource(R.string.player_playback_failed), color = Color.White)
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        if (validUrl) R.string.player_init_error else R.string.player_invalid_url
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
                Button(onClick = onBack, modifier = Modifier.focusRequester(backFocusRequester)
                    .tvFocusBorder(CircleShape)) {
                    Text(androidx.compose.ui.res.stringResource(R.string.back))
                }
            }
            return@Box
        }
        
        // Video player
        AndroidView(
            factory = { ctx ->
                try {
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // We're using our own controls
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Make the player view fill the entire screen
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        
                        // Apply subtitle settings (size + colours + font) to the player view
                        applySubtitleSettings(subtitleSettings, customTypeface)
                    }
                } catch (e: Exception) {
                    // Return a simple view if PlayerView fails to initialize
                    View(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                try {
                    // Re-apply subtitle styling whenever the settings change so
                    // changes made in the in-player dialog are visible instantly.
                    if (playerView is PlayerView) {
                        playerView.applySubtitleSettings(subtitleSettings, customTypeface)
                    }
                } catch (e: Exception) {
                    // Ignore update errors
                }
            }
        )
        
        // Rewind indicator
        if (showRewindIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = androidx.compose.ui.res.stringResource(
                            R.string.player_rewind_seconds, videoPlayerSettings.seekTimeSeconds),
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "${videoPlayerSettings.seekTimeSeconds}s",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        // Forward indicator
        if (showForwardIndicator) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward,
                        contentDescription = androidx.compose.ui.res.stringResource(
                            R.string.player_forward_seconds, videoPlayerSettings.seekTimeSeconds),
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "${videoPlayerSettings.seekTimeSeconds}s",
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        
        if (showBrightnessIndicator) {
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White)
                Text("${(brightnessLevel * 100).roundToInt()}%", color = Color.White)
            }
        }
        if (showVolumeIndicator) {
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                Text("${(volumeLevel * 100).roundToInt()}%", color = Color.White)
            }
        }
        if (isHoldingForSpeed) {
            Box(
                Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("2×", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        // A passive, thin silver progress line remains when the controls fade.
        // The full 48dp seek target lives only in the interactive overlay.
        if (!showControls && duration > 0L) {
            Canvas(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp)) {
                val played = (currentPosition.toDouble() / duration.toDouble())
                    .coerceIn(0.0, 1.0).toFloat()
                drawRect(Color(0xFFB0B0B0).copy(alpha = 0.45f))
                drawRect(Color(0xFFD0D0D0),
                    size = androidx.compose.ui.geometry.Size(size.width * played, size.height))
            }
        }

        // Fade on both phone and TV. During the 350ms exit the controls stay
        // drawn but their focus/click handlers are disabled; after exit the
        // entire subtree is removed (no invisible touch or focus targets).
        AnimatedVisibility(
            visible = controlsInteractive,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 350)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top bar with back button and settings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = { markInteraction(); onBack() },
                        enabled = controlsInteractive,
                        modifier = Modifier
                            .size(48.dp)
                            .tvFocusBorder(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    
                    // Settings button in top right corner
                    IconButton(
                        onClick = { markInteraction(); showTrackSelectionDialog = true },
                        enabled = controlsInteractive,
                        modifier = Modifier
                            .focusRequester(settingsFocusRequester)
                            .size(48.dp)
                            .tvFocusBorder(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.settings),
                            tint = Color.White
                        )
                    }
                }
                
                // Middle play/pause button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { markInteraction(); isPlaying = !isPlaying },
                        enabled = controlsInteractive,
                        modifier = Modifier
                            .focusRequester(playFocusRequester)
                            .size(64.dp)
                            .tvFocusBorder(CircleShape)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = androidx.compose.ui.res.stringResource(
                                if (isPlaying) R.string.player_pause else R.string.player_play),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Bottom controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp)
                ) {
                    // Canvas draws a slim track in a full-height (48dp) touch target.
                    if (isRetrying) {
                        androidx.compose.material3.LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    } else {
                        PlayerSeekBar(
                            positionMs = currentPosition,
                            bufferedPositionMs = bufferedPosition,
                            durationMs = duration,
                            color = Color(videoPlayerSettings.progressColor),
                            enabled = controlsInteractive && duration > 0L &&
                                exoPlayer?.isCurrentMediaItemSeekable == true,
                            seekStepMs = videoPlayerSettings.seekTimeSeconds * 1000L,
                            onSeekStart = {
                                markInteraction()
                                wasPlayingBeforeSeek = isPlaying
                                isSeeking = true
                            },
                            onSeekPreview = { markInteraction(); currentPosition = it },
                            onSeekTo = { position ->
                                markInteraction()
                                try {
                                    exoPlayer?.seekTo(position)
                                    currentPosition = position
                                } catch (_: Exception) {
                                    // A live stream can become unseekable while dragging.
                                }
                            },
                            onSeekFinished = {
                                markInteraction()
                                isSeeking = false
                                try {
                                    exoPlayer?.playWhenReady = wasPlayingBeforeSeek
                                    isPlaying = wasPlayingBeforeSeek
                                } catch (_: Exception) {
                                    // The player may be closing.
                                }
                            }
                        )
                    }
                    
                    // Time and controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = playerFontFamily
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        if (isRetrying) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.retrying),
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        
                        // Video speed controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Tapping the speed chip opens a small popup dialog
                            // listing every speed option (a DropdownMenu did not
                            // show reliably above the full-screen player, and was
                            // unreachable with a TV remote).
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .focusRequester(speedFocusRequester)
                                    .heightIn(min = if (isTv) 48.dp else 0.dp)
                                    .tvFocusBorder(RoundedCornerShape(8.dp))
                                    .clickable(enabled = controlsInteractive) {
                                        markInteraction()
                                        showSpeedDropdown = true
                                    }
                                    .padding(if (isTv) 8.dp else 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.playback_speed),
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text(
                                    text = String.format("%.2fx", playbackSpeed),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontFamily = playerFontFamily
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            // Separate, full-height focus target for returning to normal speed.
                            Box(
                                modifier = Modifier.heightIn(min = if (isTv) 48.dp else 0.dp)
                                    .widthIn(min = if (isTv) 48.dp else 0.dp)
                                    .tvFocusBorder(RoundedCornerShape(8.dp))
                                    .clickable(enabled = controlsInteractive) {
                                        markInteraction()
                                        playbackSpeed = 1.0f
                                    }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.normal),
                                    color = if (playbackSpeed == 1.0f) MaterialTheme.colorScheme.primary else Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (playbackSpeed == 1.0f) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = playerFontFamily
                                )
                            }
                        }
                        
                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = playerFontFamily
                        )
                    }
                }
            }
        }
        
        // Persistent, focusable error panel (also reachable with an Android TV remote).
        playerError?.takeUnless { isRetrying }?.let { error ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.92f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.player_playback_failed),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = playbackErrorMessage(context, error),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { retryPlayback() },
                        modifier = Modifier.focusRequester(retryFocusRequester).tvFocusBorder(CircleShape)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.retry))
                    }
                    TextButton(onClick = onBack, modifier = Modifier.tvFocusBorder(CircleShape)) {
                        Text(androidx.compose.ui.res.stringResource(R.string.back))
                    }
                }
            }
        }

        // Quick playback-speed popup opened from the bottom control bar
        if (showSpeedDropdown) {
            OptionPickerDialog(
                title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
                options = speedOptions.map { speed ->
                    PickerOption(
                        label = String.format("%.2fx", speed),
                        isSelected = speed == playbackSpeed,
                        onSelect = { markInteraction(); playbackSpeed = speed }
                    )
                },
                onInteraction = ::markInteraction,
                onDismiss = {
                    markInteraction()
                    showSpeedDropdown = false
                    restorePlayerControl(speedFocusRequester)
                }
            )
        }

        // Player settings dialog (tracks + subtitle style + speed)
        if (showTrackSelectionDialog) {
            TrackSelectionDialog(
                tracks = currentTracks,
                trackSelector = trackSelector,
                subtitleSettings = subtitleSettings,
                onSubtitleSettingsChanged = { updateSubtitleSettings(it) },
                playbackSpeed = playbackSpeed,
                speedOptions = speedOptions,
                onPlaybackSpeedChanged = { markInteraction(); playbackSpeed = it },
                onInteraction = ::markInteraction,
                onDismiss = {
                    markInteraction()
                    showTrackSelectionDialog = false
                    restorePlayerControl(settingsFocusRequester)
                }
            )
        }
    }
}

/** A single row inside [OptionPickerDialog]. */
data class PickerOption(
    val label: String,
    val isSelected: Boolean,
    val onSelect: () -> Unit
)

/**
 * A small popup dialog listing selectable options.
 *
 * Replaces the `DropdownMenu`s that were used for playback speed /
 * audio track / subtitle track: those did not render reliably on top of the
 * full-screen player and could not be focused with an Android TV remote.
 */
@Composable
fun OptionPickerDialog(
    title: String,
    options: List<PickerOption>,
    onDismiss: () -> Unit,
    onInteraction: () -> Unit = {}
) {
    val initialFocus = remember { FocusRequester() }
    val isTv = DeviceUtils.isTv(LocalContext.current)
    LaunchedEffect(isTv) {
        if (isTv) {
            withFrameNanos { }
            try { initialFocus.requestFocus() } catch (_: IllegalStateException) { }
        }
    }
    val initialIndex = options.indexOfFirst { it.isSelected }.takeIf { it >= 0 } ?: 0
    val pickerMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.60f).dp.coerceAtMost(
        if (isTv) 520.dp else 380.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        onInteraction = onInteraction,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = pickerMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEachIndexed { index, option ->
                    OptionPickerRow(
                        label = option.label,
                        isSelected = option.isSelected,
                        onClick = {
                            onInteraction()
                            option.onSelect()
                            onDismiss()
                        },
                        modifier = if (index == initialIndex) Modifier.focusRequester(initialFocus)
                            else Modifier
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss,
                modifier = (if (options.isEmpty()) Modifier.focusRequester(initialFocus) else Modifier)
                    .tvFocusBorder(CircleShape)) {
                Text(androidx.compose.ui.res.stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun OptionPickerRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .settingsFocusBorder(isFocused, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    isFocused -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .semantics { selected = isSelected; role = Role.RadioButton }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            // On the translucent selected fill, onSurface remains legible on both
            // light and dark themes; the checkmark still carries the accent.
            color = if (isFocused && !isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Player settings dialog: audio track, subtitle track, playback speed and
 * subtitle styling.
 *
 * Each selectable list opens as its own small popup ([OptionPickerDialog])
 * instead of an inline DropdownMenu, so the options actually appear over the
 * full-screen player and can be reached with an Android TV remote.
 */
@Composable
fun TrackSelectionDialog(
    tracks: Tracks,
    trackSelector: DefaultTrackSelector?,
    subtitleSettings: SubtitleSettings = SubtitleSettings.DEFAULT,
    onSubtitleSettingsChanged: (SubtitleSettings) -> Unit = {},
    playbackSpeed: Float = 1.0f,
    speedOptions: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f),
    onPlaybackSpeedChanged: (Float) -> Unit = {},
    onDismiss: () -> Unit,
    onInteraction: () -> Unit = {}
) {
    val noneLabel = androidx.compose.ui.res.stringResource(R.string.none)

    val audioTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    }

    val textTrackGroups = remember(tracks) {
        tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
    }

    // Which sub-popup is currently open
    var showAudioPicker by remember { mutableStateOf(false) }
    var showSubtitlePicker by remember { mutableStateOf(false) }
    var showSpeedPicker by remember { mutableStateOf(false) }
    val isTv = DeviceUtils.isTv(LocalContext.current)
    val settingsMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.68f).dp
    val audioFocus = remember { FocusRequester() }
    val subtitleFocus = remember { FocusRequester() }
    val speedFocus = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(isTv) {
        if (isTv) {
            withFrameNanos { }
            val first = when {
                audioTrackGroups.isNotEmpty() -> audioFocus
                textTrackGroups.isNotEmpty() -> subtitleFocus
                else -> speedFocus
            }
            try { first.requestFocus() } catch (_: IllegalStateException) { }
        }
    }
    fun returnTo(requester: FocusRequester) {
        if (isTv) scope.launch {
            withFrameNanos { }
            try { requester.requestFocus() } catch (_: IllegalStateException) { }
        }
    }

    /** Human readable name for a track format. */
    fun trackName(format: androidx.media3.common.Format, fallback: String): String {
        val label = format.label
        if (!label.isNullOrBlank()) return label
        val language = format.language
        if (!language.isNullOrBlank()) {
            return try {
                val locale = java.util.Locale(language)
                locale.displayLanguage.ifBlank { language }
            } catch (e: Exception) {
                language
            }
        }
        return fallback
    }

    val currentAudioSelection = remember(audioTrackGroups) {
        audioTrackGroups.firstOrNull { it.isSelected }?.let { group ->
            (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { index ->
                trackName(group.getTrackFormat(index), "Track ${index + 1}")
            }
        } ?: noneLabel
    }

    val currentSubtitleSelection = remember(textTrackGroups) {
        textTrackGroups.firstOrNull { it.isSelected }?.let { group ->
            (0 until group.length).firstOrNull { group.isTrackSelected(it) }?.let { index ->
                trackName(group.getTrackFormat(index), "Subtitle ${index + 1}")
            }
        } ?: noneLabel
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        onInteraction = onInteraction,
        title = {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.player_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = settingsMaxHeight)
                    .verticalScroll(rememberScrollState())
            ) {
                // ---------- Audio track ----------
                if (audioTrackGroups.isNotEmpty()) {
                    PlayerSettingRow(
                        title = androidx.compose.ui.res.stringResource(R.string.audio_tracks),
                        value = currentAudioSelection,
                        onClick = { showAudioPicker = true },
                        modifier = Modifier.focusRequester(audioFocus)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ---------- Subtitle track ----------
                if (textTrackGroups.isNotEmpty()) {
                    PlayerSettingRow(
                        title = androidx.compose.ui.res.stringResource(R.string.subtitles),
                        value = currentSubtitleSelection,
                        onClick = { showSubtitlePicker = true },
                        modifier = Modifier.focusRequester(subtitleFocus)
                    )
                } else {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.no_subtitles_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---------- Playback speed ----------
                PlayerSettingRow(
                    title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
                    value = String.format("%.2fx", playbackSpeed),
                    onClick = { showSpeedPicker = true },
                    modifier = Modifier.focusRequester(speedFocus)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ---------- Subtitle styling ----------
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Subtitle text size, adjusted with +/- so a TV remote works
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.subtitle_text_size,
                        subtitleSettings.textSize.toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SubtitleSizeButton(
                        symbol = "−", label = R.string.player_subtitle_smaller,
                        enabled = subtitleSettings.textSize > 10f,
                        onClick = {
                            onSubtitleSettingsChanged(
                                subtitleSettings.copy(
                                    textSize = (subtitleSettings.textSize - 1f).coerceIn(10f, 60f)
                                )
                            )
                        }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${subtitleSettings.textSize.toInt()} sp",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    SubtitleSizeButton(
                        symbol = "+", label = R.string.player_subtitle_larger,
                        enabled = subtitleSettings.textSize < 60f,
                        onClick = {
                            onSubtitleSettingsChanged(
                                subtitleSettings.copy(
                                    textSize = (subtitleSettings.textSize + 1f).coerceIn(10f, 60f)
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle text colour
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_text_color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Color.Yellow to R.string.color_yellow, Color.White to R.string.color_white,
                        Color.Black to R.string.color_black, Color.Red to R.string.color_red,
                        Color.Blue to R.string.color_blue, Color.Green to R.string.color_green,
                        Color.Cyan to R.string.color_cyan
                    ).forEach { (color, nameRes) ->
                        val argb = color.toArgb()
                        SubtitleColorSwatch(
                            color = color,
                            label = androidx.compose.ui.res.stringResource(nameRes),
                            isSelected = subtitleSettings.textColor == argb,
                            onClick = {
                                onSubtitleSettingsChanged(subtitleSettings.copy(textColor = argb))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Subtitle background / outline colour
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.subtitle_background_color),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val glass = Color.Black.copy(alpha = 0.5f)
                    listOf(
                        Color.Transparent to R.string.color_empty, glass to R.string.color_glass,
                        Color.Black to R.string.color_black, Color.White to R.string.color_white,
                        Color.Red to R.string.color_red, Color.Blue to R.string.color_blue
                    ).forEach { (color, nameRes) ->
                        val argb = color.toArgb()
                        SubtitleColorSwatch(
                            color = color,
                            label = androidx.compose.ui.res.stringResource(nameRes),
                            isSelected = subtitleSettings.borderColor == argb,
                            onClick = {
                                onSubtitleSettingsChanged(subtitleSettings.copy(borderColor = argb))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live preview so the chosen size/colours are visible instantly
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF101010))
                        .padding(vertical = 18.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.subtitle_preview_text),
                        color = Color(subtitleSettings.textColor),
                        fontSize = subtitleSettings.textSize.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(subtitleSettings.borderColor))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.tvFocusBorder(CircleShape)) {
                Text(
                    androidx.compose.ui.res.stringResource(R.string.close),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )

    // ---------- Sub-popups ----------
    if (showAudioPicker) {
        val options = mutableListOf<PickerOption>()
        options += PickerOption(
            label = noneLabel,
            isSelected = audioTrackGroups.none { it.isSelected },
            onSelect = {
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                )
            }
        )
        audioTrackGroups.forEachIndexed { groupIndex, trackGroup ->
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(i)
                options += PickerOption(
                    label = trackName(format, "Track ${groupIndex + 1}.${i + 1}"),
                    isSelected = trackGroup.isTrackSelected(i),
                    onSelect = {
                        trackSelector?.setParameters(
                            trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                        )
                    }
                )
            }
        }
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.audio_tracks),
            options = options,
            onInteraction = onInteraction,
            onDismiss = {
                onInteraction()
                showAudioPicker = false
                returnTo(audioFocus)
            }
        )
    }

    if (showSubtitlePicker) {
        val options = mutableListOf<PickerOption>()
        options += PickerOption(
            label = noneLabel,
            isSelected = textTrackGroups.none { it.isSelected },
            onSelect = {
                trackSelector?.setParameters(
                    trackSelector.buildUponParameters()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                )
            }
        )
        textTrackGroups.forEachIndexed { groupIndex, trackGroup ->
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getTrackFormat(i)
                options += PickerOption(
                    label = trackName(format, "Subtitle ${groupIndex + 1}.${i + 1}"),
                    isSelected = trackGroup.isTrackSelected(i),
                    onSelect = {
                        trackSelector?.setParameters(
                            trackSelector.buildUponParameters()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(
                                    TrackSelectionOverride(trackGroup.mediaTrackGroup, i)
                                )
                        )
                    }
                )
            }
        }
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.subtitles),
            options = options,
            onInteraction = onInteraction,
            onDismiss = {
                onInteraction()
                showSubtitlePicker = false
                returnTo(subtitleFocus)
            }
        )
    }

    if (showSpeedPicker) {
        OptionPickerDialog(
            title = androidx.compose.ui.res.stringResource(R.string.playback_speed),
            options = speedOptions.map { speed ->
                PickerOption(
                    label = String.format("%.2fx", speed),
                    isSelected = speed == playbackSpeed,
                    onSelect = { onPlaybackSpeedChanged(speed) }
                )
            },
            onInteraction = onInteraction,
            onDismiss = {
                onInteraction()
                showSpeedPicker = false
                returnTo(speedFocus)
            }
        )
    }
}

/**
 * A "title / current value / chevron" row inside the player settings dialog.
 * Tapping it (or pressing OK on a remote) opens the matching option popup.
 */
@Composable
private fun PlayerSettingRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .settingsFocusBorder(isFocused, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isFocused)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Focusable +/- button for the in-player subtitle size stepper. */
@Composable
private fun SubtitleSizeButton(
    symbol: String,
    label: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val actionDescription = androidx.compose.ui.res.stringResource(label)

    Box(
        modifier = Modifier
            .size(if (DeviceUtils.isTv(LocalContext.current)) 48.dp else 44.dp)
            .settingsFocusBorder(isFocused, CircleShape)
            .clip(CircleShape)
            .background(
                if (enabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = if (isFocused) 0.9f else 0.25f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .semantics { contentDescription = actionDescription }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                isFocused -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

/** Focusable colour swatch used by the in-player subtitle style section. */
@Composable
private fun SubtitleColorSwatch(
    color: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val isTv = DeviceUtils.isTv(LocalContext.current)
    Box(
        modifier = Modifier
            .size(if (isTv) 48.dp else 34.dp)
            .settingsFocusBorder(isFocused, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(if (color == Color.Transparent) Color.Gray.copy(alpha = 0.35f) else color)
            .semantics {
                contentDescription = label
                selected = isSelected
                role = Role.RadioButton
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color == Color.White || color == Color.Yellow || color == Color.Transparent)
                    Color.Black
                else
                    Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else if (color == Color.Transparent) {
            Text(text = "—", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, remainingMinutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", remainingMinutes, remainingSeconds)
    }
}