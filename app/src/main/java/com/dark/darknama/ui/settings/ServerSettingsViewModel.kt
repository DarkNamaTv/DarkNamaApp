package com.dark.darknama.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dark.darknama.data.repository.ServerSettingsRepository

/** All server-setting writes pass through the repository (including the hub's global reset). */
class ServerSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ServerSettingsRepository.get(application)
    val settings = repository.settings

    fun setAutomatic(enabled: Boolean) = repository.setAutomatic(enabled)
    fun selectServer(number: Int) = repository.selectServer(number)
    fun reset() = repository.reset()
}
