package com.dark.darknama.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.ui.theme.chromeContainerColor
import com.dark.darknama.ui.theme.glassChrome

/** D-pad edge navigation enters this rail; pressing toward content returns to the last control. */
@Composable
fun SidebarNavigation(
    navController: NavController,
    selectedItemRequester: FocusRequester,
    onEnterContent: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedSection = AppScreens.sectionForRoute(currentRoute)
    val towardsContent = if (LocalLayoutDirection.current == LayoutDirection.Ltr)
        Key.DirectionRight else Key.DirectionLeft

    NavigationRail(
        modifier = Modifier.fillMaxHeight().width(112.dp)
            .padding(top = 24.dp, bottom = 24.dp)
            .glassChrome(RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)),
        containerColor = chromeContainerColor(),
        header = { Spacer(Modifier.height(8.dp)) }
    ) {
        Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppScreens.sidebarScreens.forEach { screen ->
                val selected = selectedSection == screen
                val scale by animateFloatAsState(if (selected) 1.06f else 1f,
                    animationSpec = tween(200), label = "tvRailScale")
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200), label = "tvRailTint")
                NavigationRailItem(
                    modifier = (if (selected) Modifier.focusRequester(selectedItemRequester) else Modifier)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == towardsContent) {
                                onEnterContent()
                                true
                            } else false
                        }
                        .tvFocusBorder(RoundedCornerShape(16.dp)),
                    selected = selected,
                    onClick = {
                        if (currentRoute == screen.route) {
                            onEnterContent()
                        } else {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
                                // Movies is the TV root; splash was removed from the stack.
                                popUpTo(AppScreens.Movies.route) { saveState = true }
                            }
                        }
                    },
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(100.dp).scale(scale)) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                if (selected) Surface(Modifier.size(44.dp), shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)) {}
                                // The selected icon sits on a translucent accent disc;
                                // primary-on-primary loses contrast for dark palette choices.
                                Icon(screen.icon ?: Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onSurface else tint,
                                    modifier = Modifier.size(28.dp))
                            }
                            Text(stringResource(screen.resourceId), color = tint,
                                fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1, textAlign = TextAlign.Center)
                        }
                    },
                    label = null,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent)
                )
            }
        }
    }
}
