package com.dark.darknama.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import androidx.compose.ui.res.stringResource
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex

@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    collapsedMaxLines: Int = 2
) {
    var expanded by remember { mutableStateOf(false) }
    var showReadMore by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    // Check if text is truncated (more lines than maxLines)
                    showReadMore = textLayoutResult.hasVisualOverflow
                }
            )
            
            // Copy icon
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText(
                        context.getString(R.string.description_clipboard_label), text)
                    clipboard.setPrimaryClip(clip)
                    
                    // Show a toast to indicate success
                    android.widget.Toast.makeText(context, R.string.description_copied,
                        android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.tvFocusItem("description-copy")
                    .padding(start = 8.dp).tvFocusBorder(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (showReadMore || expanded) {
            // Glass-like button for Show More/Show Less
            Card(
                modifier = Modifier.tvFocusItem("description-toggle")
                    .padding(top = 8.dp)
                    .tvFocusBorder(RoundedCornerShape(16.dp))
                    .clickable { expanded = !expanded }
                    .clip(RoundedCornerShape(16.dp))
                    .zIndex(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.description_show_less
                            else R.string.description_show_more),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}