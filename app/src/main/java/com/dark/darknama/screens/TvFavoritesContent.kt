package com.dark.darknama.screens

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.dark.darknama.components.LocalTvGridRow
import com.dark.darknama.components.LocalTvGridRowTracker
import com.dark.darknama.components.TvGridRowTracker
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dark.darknama.R
import com.dark.darknama.components.LibraryTabButton
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.data.model.FavoriteGroup
import com.dark.darknama.data.model.FavoriteItem
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.navigation.LocalSidebarFocusRequester
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import com.dark.darknama.ui.theme.GlassCard as Card
import com.dark.darknama.ui.theme.GlassDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

/** TV-only library: header, switcher, groups and cards share ONE scrolling grid. */
@Composable
internal fun TvFavoritesContent(
    navController: NavController,
    favorites: List<FavoriteItem>,
    groups: List<FavoriteGroup>,
    selectedGroup: FavoriteGroup?,
    showHistory: Boolean,
    moveDialogOpen: Boolean,
    gridState: LazyGridState,
    historyState: LazyListState,
    restoreEmptyFocus: Boolean,
    restoreGroupFocus: Boolean,
    onEmptyRestored: () -> Unit,
    onGroupRestored: () -> Unit,
    onTabSelected: (Boolean) -> Unit,
    onGroupSelected: (FavoriteGroup) -> Unit,
    onCreateGroup: () -> Unit,
    onDeleteAll: () -> Unit,
    onRenameGroup: (FavoriteGroup) -> Unit,
    onDeleteGroup: (FavoriteGroup) -> Unit,
    onMoveToGroup: (FavoriteItem) -> Unit,
    onFavoritesChanged: (List<FavoriteItem>) -> Unit
) {
    val context = LocalContext.current
    val columns = DeviceUtils.getGridColumns(context)
    val scrollSpace = rememberTvFocusScrollSpace()
    val scope = rememberCoroutineScope()
    val tracker = remember { TvGridRowTracker(scope) }
    val sidebar = LocalSidebarFocusRequester.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val headerFocus = remember { FocusRequester() }
    val favoritesTab = remember { FocusRequester() }
    val historyTab = remember { FocusRequester() }
    val groupFocus = remember { FocusRequester() }
    val firstCardFocus = remember { FocusRequester() }
    val cardRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var pendingCardKey by remember { mutableStateOf<String?>(null) }
    var pendingIndex by remember { mutableIntStateOf(0) }
    var removeCandidate by remember { mutableStateOf<FavoriteItem?>(null) }
    var previousTab by remember { mutableStateOf(showHistory) }

    // A switched-away lazy tab can be parked deep in its content, so its tab
    // button is not composed yet. On an actual tab change, reveal that real
    // action before requesting focus. A screen reload does not reset scroll.
    LaunchedEffect(showHistory) {
        if (previousTab != showHistory) {
            previousTab = showHistory
            if (showHistory) historyState.scrollToItem(0) else gridState.scrollToItem(1)
        }
        withFrameNanos { }
        try {
            (if (showHistory) historyTab else favoritesTab).requestFocus()
        } catch (_: IllegalStateException) { }
    }

    LaunchedEffect(restoreGroupFocus, restoreEmptyFocus, groups, favorites, selectedGroup, showHistory) {
        if (showHistory) return@LaunchedEffect
        if (restoreGroupFocus && groups.isNotEmpty()) {
            gridState.scrollToItem(2)
            withFrameNanos { }
            try { groupFocus.requestFocus() } catch (_: IllegalStateException) {
                try { favoritesTab.requestFocus() } catch (_: IllegalStateException) { }
            }
            onGroupRestored()
        }
        if (restoreEmptyFocus && favorites.isEmpty()) {
            gridState.scrollToItem(3)
            withFrameNanos { }
            try { firstCardFocus.requestFocus() } catch (_: IllegalStateException) {
                try { favoritesTab.requestFocus() } catch (_: IllegalStateException) { }
            }
            onEmptyRestored()
        }
    }

    // Keep the same card after a menu/dialog closes; if that card was removed or
    // left the selected group, focus its nearest remaining neighbour. Preserve
    // the current scroll position unless that neighbour is genuinely offscreen.
    LaunchedEffect(pendingCardKey, favorites, moveDialogOpen, removeCandidate, showHistory) {
        val wanted = pendingCardKey ?: return@LaunchedEffect
        if (showHistory || moveDialogOpen || removeCandidate != null) return@LaunchedEffect
        if (favorites.isEmpty()) {
            gridState.scrollToItem(3) // Full-width empty action after header/tabs/groups.
            withFrameNanos { }
            try { firstCardFocus.requestFocus() } catch (_: IllegalStateException) { }
            pendingCardKey = null
            return@LaunchedEffect
        }
        val index = favorites.indexOfFirst { favoriteKey(it) == wanted }
            .takeIf { it >= 0 } ?: pendingIndex.coerceIn(0, favorites.lastIndex)
        val target = favoriteKey(favorites[index])
        if (gridState.layoutInfo.visibleItemsInfo.none { it.key == target }) {
            gridState.scrollToItem(index + 3)
        }
        repeat(4) {
            withFrameNanos { }
            val requester = cardRequesters[target]
            if (requester != null) {
                try {
                    requester.requestFocus()
                    pendingCardKey = null
                    return@LaunchedEffect
                } catch (_: IllegalStateException) { }
            }
        }
    }

    if (removeCandidate != null) {
        val item = removeCandidate!!
        AlertDialog(
            onDismissRequest = { removeCandidate = null },
            title = { Text("Remove from Favorites") },
            text = { Text("Are you sure you want to remove \"${item.title}\" from your favorites?") },
            confirmButton = {
                TextButton(modifier = Modifier.tvFocusBorder(CircleShape), onClick = {
                    StorageUtils.removeFavorite(context, item.id, item.type)
                    val updated = if (selectedGroup != null && !selectedGroup.isDefault)
                        StorageUtils.getFavoritesInGroup(context, selectedGroup.id)
                    else StorageUtils.loadAllFavorites(context)
                    onFavoritesChanged(updated)
                    removeCandidate = null
                    android.widget.Toast.makeText(context, "Removed from favorites",
                        android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { removeCandidate = null },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)) {
                    Text("Cancel")
                }
            }
        )
    }

    CompositionLocalProvider(LocalTvGridRowTracker provides tracker) {
    Column(Modifier.fillMaxSize()) {
    if (TV_PIN_HEADER) TvPinnedHeader {
        TvFavoritesMainHeader(navController, selectedGroup, showHistory, headerFocus)
    }
    if (showHistory) {
        LastWatchedPanel(
            modifier = Modifier.weight(1f).fillMaxSize().clipToBounds(),
            tvListState = historyState,
            tvHeader = { downTarget ->
                TvFavoritesHeaderAndTabs(
                    navController, selectedGroup, favorites.isNotEmpty(), showHistory,
                    favoritesTab, historyTab, downTarget,
                    onTabSelected, onCreateGroup, onDeleteAll, if (TV_PIN_HEADER) headerFocus else null
                )
            },
            tvTabFocus = historyTab,
            tvEmptyAction = { navController.navigate(AppScreens.Movies.route) }
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            modifier = Modifier.weight(1f).fillMaxSize().clipToBounds(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = scrollSpace.bottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.tvResetGridRowOnFocus()) {
                    TvFavoritesHeader(navController, selectedGroup, favorites.isNotEmpty(),
                        showHistory = false, onCreateGroup, onDeleteAll, if (TV_PIN_HEADER) headerFocus else null)
                }
            }
            item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.tvResetGridRowOnFocus()) {
                    TvFavoritesTabs(showHistory = false, favoritesTab, historyTab,
                        if (groups.isNotEmpty()) groupFocus else firstCardFocus, onTabSelected)
                }
            }
            item(key = "groups", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.tvResetGridRowOnFocus()) {
                    TvFavoriteGroups(groups, selectedGroup, favoritesTab, groupFocus, firstCardFocus,
                        onGroupSelected, onRenameGroup, onDeleteGroup)
                }
            }
            if (favorites.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        TextButton(onClick = { navController.navigate(AppScreens.Movies.route) },
                            modifier = Modifier.focusRequester(firstCardFocus)
                                .focusProperties {
                                    up = if (groups.isNotEmpty()) groupFocus else favoritesTab
                                    if (sidebar != null) {
                                        if (isRtl) right = sidebar else left = sidebar
                                    }
                                }
                                .tvFocusItem("favorites-empty")
                                .tvBringIntoViewOnFocus().tvFocusBorder(CircleShape)) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Text(stringResource(R.string.browse_movies),
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            } else {
                itemsIndexed(favorites, key = { _, item -> favoriteKey(item) }) { index, favorite ->
                    val key = favoriteKey(favorite)
                    val requester = remember(key, index == 0) {
                        if (index == 0) firstCardFocus else FocusRequester()
                    }
                    DisposableEffect(key, requester) {
                        cardRequesters[key] = requester
                        onDispose { if (cardRequesters[key] === requester) cardRequesters.remove(key) }
                    }
                    CompositionLocalProvider(LocalTvGridRow provides index / columns) {
                    FavoriteItemCard(
                        favorite = favorite,
                        modifier = Modifier.tvPinFocusedRow(gridState, scrollSpace, index, columns, headerItems = 3)
                            .focusProperties {
                            if (index < columns) {
                                up = if (groups.isNotEmpty()) groupFocus else favoritesTab
                            }
                            if (index % columns == 0 && sidebar != null) {
                                if (isRtl) right = sidebar else left = sidebar
                            }
                        },
                        cardFocusRequester = requester,
                        thinTvFocus = true,
                        onClick = {
                            StorageUtils.saveFavoriteToDatabase(context, favorite)
                            when (favorite.type) {
                                "movie" -> navController.navigate(AppScreens.SingleMovie.route.replace("{movieId}", favorite.id.toString()))
                                "series" -> navController.navigate(AppScreens.SingleSeries.route.replace("{seriesId}", favorite.id.toString()))
                            }
                        },
                        onDelete = {
                            pendingCardKey = key
                            pendingIndex = index
                            removeCandidate = favorite
                        },
                        onMoveToGroup = { item ->
                            pendingCardKey = key
                            pendingIndex = index
                            onMoveToGroup(item)
                        }
                    )
                    }
                }
            }
        }
    }
    }
    }
}

internal fun favoriteKey(item: FavoriteItem): String = "${item.type}:${item.id}"

@Composable
internal fun TvFavoritesHeaderAndTabs(
    navController: NavController, selectedGroup: FavoriteGroup?, hasFavorites: Boolean,
    showHistory: Boolean, favoritesTab: FocusRequester, historyTab: FocusRequester,
    downTarget: FocusRequester?, onTabSelected: (Boolean) -> Unit,
    onCreateGroup: () -> Unit, onDeleteAll: () -> Unit,
    headerFocus: FocusRequester? = null
) {
    TvFavoritesHeader(navController, selectedGroup, hasFavorites, showHistory,
        onCreateGroup, onDeleteAll, headerFocus)
    TvFavoritesTabs(showHistory, favoritesTab, historyTab, downTarget, onTabSelected, headerFocus)
}

@Composable
private fun TvFavoritesMainHeader(navController: NavController, selectedGroup: FavoriteGroup?,
    showHistory: Boolean, logoFocus: FocusRequester? = null) {
    MainSectionHeader(
        title = if (showHistory) stringResource(R.string.last_watched)
            else selectedGroup?.name ?: stringResource(R.string.favorites),
        navController = navController, logoFocusRequester = logoFocus
    )
}

@Composable
private fun TvFavoritesHeader(
    navController: NavController, selectedGroup: FavoriteGroup?, hasFavorites: Boolean,
    showHistory: Boolean, onCreateGroup: () -> Unit, onDeleteAll: () -> Unit,
    headerFocus: FocusRequester? = null
) {
    if (!TV_PIN_HEADER) TvFavoritesMainHeader(navController, selectedGroup, showHistory)
    if (!showHistory) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End) {
            val createInteraction = remember { MutableInteractionSource() }
            val createFocused by createInteraction.collectIsFocusedAsState()
            IconButton(onClick = onCreateGroup, interactionSource = createInteraction,
                modifier = Modifier.focusProperties { if (headerFocus != null) up = headerFocus }
                    .tvFocusItem("favorite-create").size(48.dp).tvBringIntoViewOnFocus()
                    .settingsFocusBorder(createFocused, CircleShape)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_playlist),
                    tint = MaterialTheme.colorScheme.primary)
            }
            if (hasFavorites) {
                val deleteInteraction = remember { MutableInteractionSource() }
                val deleteFocused by deleteInteraction.collectIsFocusedAsState()
                IconButton(onClick = onDeleteAll, interactionSource = deleteInteraction,
                    modifier = Modifier.focusProperties { if (headerFocus != null) up = headerFocus }
                        .tvFocusItem("favorite-delete-all").size(48.dp).tvBringIntoViewOnFocus()
                        .settingsFocusBorder(deleteFocused, CircleShape)) {
                    Icon(Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_all_favorites),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TvFavoritesTabs(
    showHistory: Boolean, favoritesTab: FocusRequester, historyTab: FocusRequester,
    downTarget: FocusRequester?, onTabSelected: (Boolean) -> Unit,
    headerFocus: FocusRequester? = null
) {
    val sidebar = LocalSidebarFocusRequester.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(Modifier.fillMaxWidth().focusGroup().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryTabButton(
            text = stringResource(R.string.favorites), selected = !showHistory,
            onClick = { onTabSelected(false) },
            modifier = Modifier.focusRequester(favoritesTab)
                .focusProperties {
                    if (downTarget != null) down = downTarget
                    if (headerFocus != null) up = headerFocus
                    if (sidebar != null) {
                        if (isRtl) right = sidebar else left = sidebar
                    }
                }
                .tvFocusItem("favorite-tab-playlists", initial = !showHistory)
                .tvBringIntoViewOnFocus()
        )
        LibraryTabButton(
            text = stringResource(R.string.last_watched), selected = showHistory,
            onClick = { onTabSelected(true) },
            modifier = Modifier.focusRequester(historyTab)
                .focusProperties {
                    if (downTarget != null) down = downTarget
                    if (headerFocus != null) up = headerFocus
                }
                .tvFocusItem("favorite-tab-history", initial = showHistory)
                .tvBringIntoViewOnFocus()
        )
    }
}

@Composable
private fun TvFavoriteGroups(
    groups: List<FavoriteGroup>, selectedGroup: FavoriteGroup?,
    favoritesTab: FocusRequester, groupFocus: FocusRequester, firstCard: FocusRequester,
    onGroupSelected: (FavoriteGroup) -> Unit,
    onRenameGroup: (FavoriteGroup) -> Unit, onDeleteGroup: (FavoriteGroup) -> Unit
) {
    val sidebar = LocalSidebarFocusRequester.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Card(modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.playlists), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp).weight(1f))
            }
            LazyRow(Modifier.fillMaxWidth().focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups, key = { it.id }) { group ->
                    Box {
                        var showGroupMenu by remember { mutableStateOf(false) }
                        val interaction = remember(group.id) { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        val groupRequester = if (selectedGroup?.id == group.id ||
                            selectedGroup == null && groups.firstOrNull()?.id == group.id)
                            Modifier.focusRequester(groupFocus) else Modifier
                        Card(onClick = { onGroupSelected(group) }, interactionSource = interaction,
                            modifier = groupRequester
                                .focusProperties {
                                    up = favoritesTab; down = firstCard
                                    if (sidebar != null && groups.firstOrNull()?.id == group.id) {
                                        if (isRtl) right = sidebar else left = sidebar
                                    }
                                }
                                .tvFocusItem("favorite-group-${group.id}")
                                .padding(vertical = 4.dp)
                                .tvBringIntoViewOnFocus()
                                .settingsFocusBorder(focused, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation =
                                if (selectedGroup?.id == group.id) 4.dp else 0.dp),
                            colors = if (selectedGroup?.id == group.id)
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            else CardDefaults.cardColors()
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(group.name)
                                if (!group.isDefault) {
                                    IconButton(onClick = { showGroupMenu = true },
                                        modifier = Modifier
                                            .focusProperties { up = favoritesTab; down = firstCard }
                                            .tvFocusItem("favorite-group-menu-${group.id}")
                                            .size(48.dp).tvFocusBorder(RoundedCornerShape(8.dp))) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Group options",
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        DropdownMenu(expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false }) {
                            DropdownMenuItem(text = { Text("Rename") },
                                modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(),
                                onClick = { showGroupMenu = false; onRenameGroup(group) })
                            DropdownMenuItem(text = { Text("Delete") },
                                modifier = Modifier.tvFocusBorder(),
                                onClick = { showGroupMenu = false; onDeleteGroup(group) })
                        }
                    }
                }
            }
        }
    }
}
