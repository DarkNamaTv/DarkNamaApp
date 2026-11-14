package com.dark.darknama.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.data.model.Source
import com.dark.darknama.utils.DownloadUtils
import com.dark.darknama.navigation.tvDialogInitialFocus

/** The same download/play actions work for movie links and episode links. */
@Composable
fun SourceDownloadOptions(
    source: Source, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DownloadOptionsDialog(
        onDismiss = onDismiss,
        onCopyLink = { DownloadUtils.copyToClipboard(context, source.url) },
        onDownloadWithBrowser = { DownloadUtils.openUrl(context, source.url) },
        onDownloadWithADM = { DownloadUtils.openWithADM(context, source.url) },
        onOpenInVLC = { DownloadUtils.openWithVLC(context, source.url) },
        onOpenInMXPlayer = { DownloadUtils.openWithMXPlayer(context, source.url) },
        onOpenInKMPlayer = { DownloadUtils.openWithKMPlayer(context, source.url) }
    )
}

@Composable
fun DownloadOptionsDialog(
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onDownloadWithBrowser: () -> Unit,
    onDownloadWithADM: () -> Unit,
    onOpenInVLC: () -> Unit,
    onOpenInMXPlayer: () -> Unit,
    onOpenInKMPlayer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.download_options_title),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text(stringResource(R.string.download_options_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.movie_download_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                DownloadAction(R.string.download_options_copy_link, onDismiss, onCopyLink,
                    modifier = Modifier.tvDialogInitialFocus())
                DownloadAction(R.string.download_options_browser, onDismiss, onDownloadWithBrowser)
                DownloadAction(R.string.download_options_adm, onDismiss, onDownloadWithADM)
                Text(stringResource(R.string.download_options_play),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                DownloadAction(R.string.download_options_vlc, onDismiss, onOpenInVLC, secondary = true)
                DownloadAction(R.string.download_options_mx, onDismiss, onOpenInMXPlayer, secondary = true)
                DownloadAction(R.string.download_options_km, onDismiss, onOpenInKMPlayer, secondary = true)
                TextButton(onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp), tonalElevation = 6.dp
    )
}

@Composable
private fun DownloadAction(label: Int, onDismiss: () -> Unit, onAction: () -> Unit,
                           secondary: Boolean = false, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Button(
        onClick = { onAction(); onDismiss() },
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().settingsFocusBorder(focused, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (secondary) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) { Text(stringResource(label)) }
}
