package com.dark.darknama.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dark.darknama.ui.theme.GlassCard
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.utils.DeviceUtils
import java.util.Locale

/** Same compact, 2:3 poster in movies, series, search, favorites (and country results). */
@Composable
fun CompactPosterCard(
    title: String, image: String, imdb: Double, year: Int, typeLabel: String,
    onClick: () -> Unit, modifier: Modifier = Modifier, focusKey: String? = null,
    thinTvFocus: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    GlassCard(
        onClick = onClick, interactionSource = interaction,
        modifier = (if (focusKey != null) modifier.tvFocusItem(focusKey) else modifier)
            .fillMaxWidth().tvBringIntoViewOnFocus(margin = 4.dp, horizontalMargin = 0.dp)
            .goldFocus(focused, shape, scaleOnFocus = true,
                thin = thinTvFocus && DeviceUtils.isTv(LocalContext.current)),
        shape = shape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)) {
                AsyncImage(model = image, contentDescription = title, contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize())
                if (imdb > 0.0) {
                    Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.78f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null,
                            tint = Color(0xFFFFCA28), modifier = Modifier.size(11.dp))
                        Text(String.format(Locale.getDefault(), "%.1f", imdb),
                            color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
                Text(typeLabel, modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.76f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Color.White, fontSize = 10.sp, maxLines = 1)
                overlay()
            }
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = (if (focusKey != null) Modifier.tvFocusItem("$focusKey-title")
                    else Modifier).copyTitleOnClick(title))
            if (year > 0) {
                Text(year.toString(), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

/** Placeholder shares the same poster ratio, padding and text dimensions as the real card. */
@Composable
fun CompactPosterPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "posterShimmer")
    val offset by transition.animateFloat(-280f, 280f,
        infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "posterGradient")
    val colors = MaterialTheme.colorScheme
    val brush = Brush.linearGradient(listOf(colors.surfaceVariant,
        colors.onSurface.copy(alpha = 0.16f), colors.surfaceVariant),
        start = Offset(offset, 0f), end = Offset(offset + 220f, 240f))
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp)).background(brush))
            Spacer(Modifier.fillMaxWidth(0.85f).height(14.dp).background(brush))
            Spacer(Modifier.fillMaxWidth(0.45f).height(12.dp).background(brush))
        }
    }
}
