package com.dark.darknama.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.navigation.tvFocusItem

/** Shared main-page header: the logo is centered in the *whole* width, not a weighted row. */
@Composable
fun MainSectionHeader(
    title: String,
    navController: NavController?,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    logoFocusRequester: FocusRequester? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp)) {
        Row(Modifier.align(Alignment.CenterStart).widthIn(max = maxWidth * 0.38f),
            verticalAlignment = Alignment.CenterVertically) {
            if (titleIcon != null) {
                Icon(titleIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        // Center is independent of title length, trailing actions, RTL/LTR and the TV sidebar.
        Box(Modifier.align(Alignment.Center).size(48.dp), contentAlignment = Alignment.Center) {
            NetworkLogoButton(modifier = (if (logoFocusRequester != null)
                Modifier.focusRequester(logoFocusRequester) else Modifier)
                .tvFocusItem("main-logo", initial = true))
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically) {
            actions()
            SettingsIconButton(navController = navController,
                modifier = Modifier.tvFocusItem("header-settings"))
        }
    }
}
