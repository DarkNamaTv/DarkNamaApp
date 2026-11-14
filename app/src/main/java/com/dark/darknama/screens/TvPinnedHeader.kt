package com.dark.darknama.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.dark.darknama.components.LocalTvGridRowTracker
import com.dark.darknama.ui.theme.LocalVisualStyle
import com.dark.darknama.ui.theme.VisualStyle

/** Set false to make the header the first scrolling grid item instead. */
internal const val TV_PIN_HEADER = true

@Composable
internal fun TvPinnedHeader(content: @Composable () -> Unit) {
    val tracker = LocalTvGridRowTracker.current
    val colors = MaterialTheme.colorScheme
    val background = if (LocalVisualStyle.current == VisualStyle.DEFAULT) colors.background
        else colors.surface.copy(alpha = 1f)
    Column(Modifier.fillMaxWidth().background(background.copy(alpha = 1f))
        .onFocusChanged { if (it.hasFocus) tracker?.reset() }) {
        content()
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outlineVariant.copy(alpha = 1f)))
    }
}
