package com.dark.darknama.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.data.repository.PlayerSettingsRepository

class PlayerSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlayerSettingsRepository(application)

    var settings by mutableStateOf(repository.load())
        private set

    fun update(newSettings: VideoPlayerSettings) {
        repository.save(newSettings)
        settings = newSettings
    }

    fun resetProgressBar() {
        update(settings.copy(
            progressColor = VideoPlayerSettings.DEFAULT_PROGRESS_COLOR
        ))
    }
}
