package com.dark.darknama.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.dark.darknama.utils.DeviceUtils
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.ui.settings.UpdateError
import com.dark.darknama.ui.settings.UpdateViewModel
import com.dark.darknama.ui.theme.GlassAlertDialog
import com.dark.darknama.utils.UpdateInstaller

/** Shared by the automatic Movies check and manual checks from Settings/Movies. */
@Composable
fun UpdateDialog(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var permissionRequired by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.startDownload() } // Denial never blocks the in-app progress dialog.
    val installPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val file = viewModel.state.value.downloadedFile
        if (file != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.packageManager.canRequestPackageInstalls())) {
            permissionRequired = false
            when (UpdateInstaller.install(context, file)) {
                UpdateInstaller.Result.STARTED -> viewModel.dismissDialog()
                UpdateInstaller.Result.MISSING_FILE -> viewModel.installFailed(missingFile = true)
                else -> viewModel.installFailed()
            }
        } else permissionRequired = true
    }

    fun beginDownload() {
        permissionRequired = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else viewModel.startDownload()
    }

    fun install() {
        val file = viewModel.state.value.downloadedFile ?: return
        when (UpdateInstaller.install(context, file)) {
            UpdateInstaller.Result.STARTED -> viewModel.dismissDialog()
            UpdateInstaller.Result.MISSING_FILE -> viewModel.installFailed(missingFile = true)
            UpdateInstaller.Result.FAILED -> viewModel.installFailed()
            UpdateInstaller.Result.NEEDS_PERMISSION -> {
                permissionRequired = true
                try { installPermission.launch(UpdateInstaller.permissionSettings(context)) }
                catch (_: Exception) { viewModel.installFailed() }
            }
        }
    }

    if (!state.showDialog) return
    val dismissFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (DeviceUtils.isTv(context)) {
            withFrameNanos { }
            try { dismissFocus.requestFocus() } catch (_: IllegalStateException) { }
        }
    }
    val errorLabel = when (state.error) {
        UpdateError.CHECK -> R.string.error_check_updates
        UpdateError.NO_APK -> R.string.update_no_apk
        UpdateError.DOWNLOAD -> R.string.update_download_failed
        UpdateError.CANCELED -> R.string.update_download_canceled
        UpdateError.INSTALL -> R.string.update_install_failed
        UpdateError.MISSING_FILE -> R.string.update_file_missing
        null -> null
    }
    GlassAlertDialog(
        onDismissRequest = viewModel::dismissDialog,
        title = { Text(stringResource(if (state.release == null && state.error == UpdateError.CHECK)
            R.string.error_check_updates else R.string.update_available_title)) },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.release?.let { release ->
                    Text(stringResource(R.string.update_available_message, release.version),
                        style = MaterialTheme.typography.bodyMedium)
                }
                if (state.checking || state.downloading) {
                    if (state.checking) Text(stringResource(R.string.checking_updates))
                    else Text(if (state.progress == null) stringResource(R.string.update_downloading)
                        else stringResource(R.string.update_download_progress, state.progress!!))
                    if (state.checking) CircularProgressIndicator()
                    else {
                        val progress = state.progress
                        if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                        else LinearProgressIndicator(progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth())
                    }
                }
                if (state.downloadedFile != null && !state.downloading)
                    Text(stringResource(R.string.update_ready), color = MaterialTheme.colorScheme.primary)
                errorLabel?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                if (permissionRequired) Text(stringResource(R.string.update_install_permission),
                    color = MaterialTheme.colorScheme.error)
                state.release?.let { release ->
                    Text(stringResource(R.string.update_release_notes),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(release.notes.ifBlank { stringResource(R.string.update_no_notes) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            when {
                state.downloading -> UpdateDialogAction(R.string.update_cancel_download, viewModel::cancelDownload)
                state.downloadedFile != null -> UpdateDialogAction(R.string.update_install, ::install)
                state.checking -> Unit
                state.error == UpdateError.CHECK || state.error == UpdateError.NO_APK ||
                    (state.error == UpdateError.MISSING_FILE && state.release?.asset == null) ->
                    UpdateDialogAction(R.string.retry, onClick = { viewModel.checkForUpdates() })
                state.release?.asset != null -> UpdateDialogAction(
                    if (state.error == null) R.string.download else R.string.retry, ::beginDownload)
                else -> UpdateDialogAction(R.string.retry, onClick = { viewModel.checkForUpdates() })
            }
        },
        dismissButton = {
            UpdateDialogAction(if (state.downloading) R.string.close else R.string.later,
                viewModel::dismissDialog, Modifier.focusRequester(dismissFocus))
        }
    )
}

@Composable
private fun UpdateDialogAction(label: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    TextButton(onClick = onClick, interactionSource = interaction,
        modifier = modifier.settingsFocusBorder(focused, CircleShape)) {
        Text(stringResource(label))
    }
}
