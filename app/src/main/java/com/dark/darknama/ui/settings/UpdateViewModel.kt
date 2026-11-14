package com.dark.darknama.ui.settings

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.BuildConfig
import com.dark.darknama.data.model.ReleaseVersion
import com.dark.darknama.data.model.UpdateRelease
import com.dark.darknama.data.repository.UpdateRepository
import com.dark.darknama.utils.UpdateNotifier
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdateError { CHECK, NO_APK, DOWNLOAD, CANCELED, INSTALL, MISSING_FILE }

data class UpdateUiState(
    val checking: Boolean = false,
    val release: UpdateRelease? = null,
    val downloading: Boolean = false,
    val progress: Int? = null,
    val downloadedFile: File? = null,
    val error: UpdateError? = null,
    val showDialog: Boolean = false,
    val upToDateNotice: Boolean = false
)

/** One activity-scoped instance serves Settings, Movies, and the launch-time check. */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UpdateRepository()
    private val notifier = UpdateNotifier(application)
    private val preferences = application.getSharedPreferences("update_preferences", Context.MODE_PRIVATE)
    private val updateDirectory = File(application.cacheDir, "updates")
    private val _state = MutableStateFlow(restoreDownloaded())
    val state = _state.asStateFlow()
    private val _automaticChecks = MutableStateFlow(preferences.getBoolean(AUTO_ENABLED, true))
    val automaticChecks = _automaticChecks.asStateFlow()
    private var downloadJob: Job? = null

    init {
        // No partial APK can be installed after a process restart.
        File(updateDirectory, "DarkNama.apk.part").delete()
    }

    fun setAutomaticChecks(enabled: Boolean) {
        preferences.edit().putBoolean(AUTO_ENABLED, enabled).apply()
        _automaticChecks.value = enabled
    }

    fun checkAutomatically() {
        if (!_automaticChecks.value || _state.value.checking || _state.value.downloading) return
        val now = System.currentTimeMillis()
        val previous = preferences.getLong(LAST_AUTO_CHECK, 0L)
        if (previous > 0 && now >= previous && now - previous < TWELVE_HOURS) return
        // Record the attempt before the request: errors cannot cause a check loop on launch.
        preferences.edit().putLong(LAST_AUTO_CHECK, now).apply()
        if (_state.value.downloadedFile?.isFile == true) {
            // A completed download needs no network request, but can be offered again later.
            _state.update { it.copy(showDialog = true) }
        } else checkForUpdates(manual = false)
    }

    fun checkForUpdates(manual: Boolean = true) {
        if (_state.value.downloading) {
            if (manual) openUpdate()
            return
        }
        if (_state.value.checking) return
        _state.update { it.copy(checking = true, upToDateNotice = false, error = null) }
        viewModelScope.launch {
            try {
                val release = repository.latestRelease()
                if (ReleaseVersion.isNewer(BuildConfig.VERSION_NAME, release.version)) {
                    val saved = _state.value.downloadedFile?.takeIf {
                        _state.value.release?.version == release.version && it.isFile
                    }
                    _state.update { it.copy(checking = false, release = release,
                        downloadedFile = saved, error = if (release.asset == null && saved == null)
                            UpdateError.NO_APK else null, showDialog = true) }
                } else {
                    _state.update { it.copy(checking = false, release = null, downloadedFile = null,
                        showDialog = false, upToDateNotice = manual) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("UpdateViewModel", "Release check failed", e)
                _state.update { it.copy(checking = false, error = UpdateError.CHECK,
                    showDialog = manual) }
            }
        }
    }

    fun openUpdate() {
        if (_state.value.release != null || _state.value.error != null)
            _state.update { it.copy(showDialog = true) }
        else checkForUpdates()
    }

    fun dismissDialog() { _state.update { it.copy(showDialog = false) } }

    fun consumeUpToDateNotice() { _state.update { it.copy(upToDateNotice = false) } }

    fun startDownload() {
        val release = _state.value.release ?: return
        val asset = release.asset ?: run {
            _state.update { it.copy(error = UpdateError.NO_APK, showDialog = true) }
            return
        }
        if (_state.value.downloading) return
        _state.update { it.copy(downloading = true, progress = null, downloadedFile = null,
            error = null, showDialog = true) }
        notifier.progress(null)
        downloadJob = viewModelScope.launch {
            try {
                val file = repository.download(asset, updateDirectory) { percent ->
                    if (_state.value.downloading) {
                        _state.update { it.copy(progress = percent) }
                        notifier.progress(percent)
                    }
                }
                preferences.edit().putString(DOWNLOADED_VERSION, release.version)
                    .putString(DOWNLOADED_TITLE, release.title)
                    .putString(DOWNLOADED_NOTES, release.notes).apply()
                _state.update { it.copy(downloading = false, downloadedFile = file,
                    progress = 100, error = null, showDialog = true) }
                notifier.complete()
            } catch (e: CancellationException) {
                // cancelDownload already updated the UI and removed the notification.
                throw e
            } catch (e: Exception) {
                Log.e("UpdateViewModel", "APK download failed", e)
                _state.update { it.copy(downloading = false, downloadedFile = null,
                    error = UpdateError.DOWNLOAD, showDialog = true) }
                notifier.failed()
            }
        }
    }

    fun cancelDownload() {
        if (!_state.value.downloading) return
        downloadJob?.cancel()
        _state.update { it.copy(downloading = false, progress = null,
            error = UpdateError.CANCELED, showDialog = true) }
        notifier.cancel()
    }

    fun installFailed(missingFile: Boolean = false) {
        if (missingFile) preferences.edit().remove(DOWNLOADED_VERSION).apply()
        _state.update { it.copy(downloadedFile = if (missingFile) null else it.downloadedFile,
            error = if (missingFile) UpdateError.MISSING_FILE else UpdateError.INSTALL,
            showDialog = true) }
    }

    override fun onCleared() {
        if (_state.value.downloading) {
            downloadJob?.cancel()
            notifier.cancel()
        }
        super.onCleared()
    }

    private fun restoreDownloaded(): UpdateUiState {
        val version = preferences.getString(DOWNLOADED_VERSION, null) ?: return UpdateUiState()
        val file = File(updateDirectory, "DarkNama.apk")
        if (!ReleaseVersion.isNewer(BuildConfig.VERSION_NAME, version) || !file.isFile) {
            preferences.edit().remove(DOWNLOADED_VERSION).apply()
            return UpdateUiState()
        }
        return UpdateUiState(release = UpdateRelease(version,
            preferences.getString(DOWNLOADED_TITLE, version) ?: version,
            preferences.getString(DOWNLOADED_NOTES, "").orEmpty(), null), downloadedFile = file)
    }

    companion object {
        private const val AUTO_ENABLED = "automatic_checks_enabled"
        private const val LAST_AUTO_CHECK = "last_automatic_check"
        private const val DOWNLOADED_VERSION = "downloaded_version"
        private const val DOWNLOADED_TITLE = "downloaded_title"
        private const val DOWNLOADED_NOTES = "downloaded_notes"
        private const val TWELVE_HOURS = 12L * 60 * 60 * 1000
    }
}
