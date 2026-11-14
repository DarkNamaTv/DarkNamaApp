package com.dark.darknama.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.utils.DeviceUtils

/** Pull-to-refresh has no remote equivalent; expose the same ViewModel action on TV. */
@Composable
fun TvRefreshButton(onClick: () -> Unit) {
    if (!DeviceUtils.isTv(LocalContext.current)) return
    IconButton(onClick = onClick,
        modifier = Modifier.tvFocusItem("screen-refresh").size(48.dp).tvFocusBorder(CircleShape)) {
        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.tv_refresh_content),
            tint = MaterialTheme.colorScheme.primary)
    }
}
