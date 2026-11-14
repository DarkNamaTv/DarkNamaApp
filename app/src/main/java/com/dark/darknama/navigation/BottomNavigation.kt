package com.dark.darknama.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.ui.theme.LocalVisualStyle
import com.dark.darknama.ui.theme.VisualStyle
import com.dark.darknama.ui.theme.chromeContainerColor
import com.dark.darknama.ui.theme.contrastingOver
import com.dark.darknama.ui.theme.glassChrome

/** A 58dp content bar plus the device's gesture/navigation inset; TV keeps its rail. */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val colors = MaterialTheme.colorScheme
    val style = LocalVisualStyle.current
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

    Surface(modifier = Modifier.fillMaxWidth().glassChrome(shape), shape = shape,
        color = chromeContainerColor(), tonalElevation = 3.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(58.dp)
            .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AppScreens.screens.filter { it.showBottomBar }.forEach { screen ->
                val selected = currentRoute == screen.route
                val interaction = remember(screen.route) { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val scale by animateFloatAsState(if (selected) 1.06f else 1f,
                    animationSpec = tween(200), label = "nav-scale")
                val selectedPill = colors.primary.copy(alpha = when (style) {
                    VisualStyle.DEFAULT -> 1f
                    VisualStyle.GLASS -> 0.88f
                    VisualStyle.NEON_GLASS -> 0.78f
                })
                val pill by animateColorAsState(if (selected) selectedPill else Color.Transparent,
                    animationSpec = tween(200), label = "nav-pill")
                // onPrimary is derived from an opaque accent; glass pills are translucent.
                // Use the *visible* composited fill to keep icons and labels readable.
                val foreground by animateColorAsState(
                    if (selected) contrastingOver(selectedPill, colors.surface, colors.background)
                    else colors.onSurfaceVariant,
                    animationSpec = tween(200), label = "nav-content")
                val pillShape = RoundedCornerShape(18.dp)
                Box(Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp)
                    .settingsFocusBorder(focused, pillShape).clip(pillShape)
                    .background(pill, pillShape)
                    .selectable(selected = selected, interactionSource = interaction,
                        indication = null, role = Role.Tab) {
                        if (!selected) navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }, contentAlignment = Alignment.Center) {
                    Column(Modifier.fillMaxSize().scale(scale),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(screen.icon ?: Icons.Default.Movie, contentDescription = null,
                            tint = foreground, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(screen.resourceId), modifier = Modifier.fillMaxWidth(),
                            color = foreground, fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
