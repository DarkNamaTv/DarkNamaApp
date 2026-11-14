package com.dark.darknama.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dark.darknama.data.model.ServerSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Allows the failover interceptor to be tested without Android preferences. */
interface ServerSelection {
    fun snapshot(): ServerSettings
    fun promoteIfAuto(expectedServer: Int, workingServer: Int): Boolean
}

/** The UI observes server changes without a network layer dependency on Android widgets. */
object ServerSwitchEvents {
    // Buffer the first switch even if it happens before MainActivity starts collecting.
    private val queue = Channel<Int>(Channel.BUFFERED)
    val switches = queue.receiveAsFlow()

    fun announce(server: Int) { queue.trySend(server) }
}

/** SharedPreferences is also used by ThemeManager; synchronous reads work in OkHttp threads. */
class ServerSettingsRepository private constructor(context: Context) : ServerSelection {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(snapshot())
    val settings = _settings.asStateFlow()

    // A repository request can switch servers while the settings page is open.
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = snapshot()
    }

    init { preferences.registerOnSharedPreferenceChangeListener(listener) }

    override fun snapshot(): ServerSettings = ServerSettings(
        automatic = preferences.getBoolean(AUTO, true),
        currentServer = preferences.getInt(CURRENT, 1).coerceIn(1, 3),
        server2Url = ServerSettings.normalizeEditableUrl(
            preferences.getString(SERVER_2, ServerSettings.DEFAULT_SERVER_2).orEmpty()
        ) ?: ServerSettings.DEFAULT_SERVER_2,
        server3Url = ServerSettings.normalizeEditableUrl(
            preferences.getString(SERVER_3, ServerSettings.DEFAULT_SERVER_3).orEmpty()
        ) ?: ServerSettings.DEFAULT_SERVER_3
    )

    fun setAutomatic(enabled: Boolean) = synchronized(preferences) {
        preferences.edit().putBoolean(AUTO, enabled).apply()
        _settings.value = snapshot()
    }

    fun selectServer(number: Int) = synchronized(preferences) {
        require(number in 1..3)
        preferences.edit().putInt(CURRENT, number).apply()
        _settings.value = snapshot()
    }

    fun reset() {
        synchronized(preferences) {
            preferences.edit().clear().commit()
            _settings.value = snapshot()
        }
    }

    /** Never overwrite a newer manual choice or a switch made by another request. */
    override fun promoteIfAuto(expectedServer: Int, workingServer: Int): Boolean =
        synchronized(preferences) {
            val current = snapshot()
            if (!current.automatic || current.currentServer != expectedServer ||
                workingServer !in 1..3 || expectedServer == workingServer
            ) return@synchronized false
            val saved = preferences.edit().putInt(CURRENT, workingServer).commit()
            if (saved) _settings.value = snapshot()
            saved
        }

    companion object {
        private const val PREFS = "server_prefs"
        private const val AUTO = "auto"
        private const val CURRENT = "current_server"
        private const val SERVER_2 = "server_2_url"
        private const val SERVER_3 = "server_3_url"

        @Volatile private var instance: ServerSettingsRepository? = null

        fun get(context: Context): ServerSettingsRepository = instance ?: synchronized(this) {
            instance ?: ServerSettingsRepository(context.applicationContext).also { instance = it }
        }
    }
}
