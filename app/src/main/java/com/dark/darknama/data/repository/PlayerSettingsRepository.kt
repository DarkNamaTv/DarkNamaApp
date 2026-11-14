package com.dark.darknama.data.repository

import android.content.Context
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.utils.StorageUtils

/** Keeps the existing JSON file (and old seek-time preference) as the source of truth. */
class PlayerSettingsRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(): VideoPlayerSettings = StorageUtils.loadVideoPlayerSettings(appContext)
    fun save(settings: VideoPlayerSettings) = StorageUtils.saveVideoPlayerSettings(appContext, settings)
}
