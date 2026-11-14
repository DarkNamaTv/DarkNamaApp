package com.dark.darknama.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dark.darknama.R

/** Copy only the title text; the surrounding card still opens the details screen. */
@Composable
fun Modifier.copyTitleOnClick(title: String): Modifier {
    val context = LocalContext.current
    val interaction = remember(title) { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    return this
        .settingsFocusBorder(focused, RoundedCornerShape(4.dp))
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            onClickLabel = stringResource(R.string.copy_title)
        ) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(
                context.getString(R.string.title_clipboard_label), title
            ))
            Toast.makeText(context, R.string.title_copied, Toast.LENGTH_SHORT).show()
        }
}
