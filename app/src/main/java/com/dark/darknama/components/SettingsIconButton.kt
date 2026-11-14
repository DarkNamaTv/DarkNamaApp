package com.dark.darknama.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.utils.DeviceUtils

/**
 * Settings gear button shown at the top of the main screens.
 * Tapping it (or pressing OK on a TV remote) opens the Settings screen.
 * Shows a visible focus ring for Android TV D-pad navigation.
 */
@Composable
fun SettingsIconButton(
    navController: NavController?,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    IconButton(
        onClick = {
            navController?.navigate(AppScreens.Settings.route) {
                launchSingleTop = true
            }
        },
        interactionSource = interactionSource,
        modifier = modifier
            .size(if (DeviceUtils.isTv(LocalContext.current)) 48.dp else 40.dp)
            .settingsFocusBorder(isFocused, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
