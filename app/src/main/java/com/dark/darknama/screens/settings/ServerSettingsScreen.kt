package com.dark.darknama.screens.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.SettingsSectionTitle
import com.dark.darknama.components.SettingsSubPage
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.ui.settings.ServerSettingsViewModel

/** Only server labels, never their hosts, are shown. Auto can still fail over from the chosen server. */
@Composable
fun ServerSettingsScreen(
    navController: NavController?,
    viewModel: ServerSettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val labels = listOf(R.string.server_1, R.string.server_2, R.string.server_3)

    SettingsSubPage(
        title = stringResource(R.string.server_settings),
        navController = navController,
        icon = Icons.Default.Dns
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                Card(
                    onClick = { viewModel.setAutomatic(!settings.automatic) },
                    interactionSource = interaction,
                    modifier = Modifier.tvFocusItem("server-auto")
                        .fillMaxWidth().settingsFocusBorder(focused)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.server_auto),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(R.string.server_auto_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // One focus target for the card; the switch itself is visual only.
                        Switch(checked = settings.automatic, onCheckedChange = null)
                    }
                }

                Spacer(Modifier.height(24.dp))
                SettingsSectionTitle(stringResource(R.string.server_choice))
                // Keep all three visible, including in Auto mode. Each card is a single
                // D-pad focus target and selecting one changes the starting server only.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { number ->
                        val serverInteraction = remember(number) { MutableInteractionSource() }
                        val serverFocused by serverInteraction.collectIsFocusedAsState()
                        val selected = settings.currentServer == number
                        Card(
                            onClick = { viewModel.selectServer(number) },
                            interactionSource = serverInteraction,
                            modifier = Modifier.tvFocusItem("server-choice-$number")
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .settingsFocusBorder(serverFocused),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(labels[number - 1]),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = viewModel::reset,
                    modifier = Modifier.tvFocusItem("server-reset").tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.server_reset_all))
                }
            }
        }
    }
}
