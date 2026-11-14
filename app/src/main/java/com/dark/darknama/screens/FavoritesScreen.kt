package com.dark.darknama.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import com.dark.darknama.ui.theme.GlassAlertDialog as AlertDialog
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.dark.darknama.ui.theme.GlassDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.components.MainSectionHeader
import com.dark.darknama.components.LibraryTabButton
import com.dark.darknama.components.CompactPosterCard
import com.dark.darknama.components.settingsFocusBorder
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.data.model.FavoriteGroup
import com.dark.darknama.data.model.FavoriteItem
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.utils.StorageUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    var favorites by remember { mutableStateOf<List<FavoriteItem>>(emptyList()) }
    var groups by remember { mutableStateOf<List<FavoriteGroup>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<FavoriteGroup?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    // Only the TV route remembers the active playlist across destination reloads.
    var tvSelectedGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var tvDataLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<FavoriteItem?>(null) }
    var focusEmptyAfterClear by remember { mutableStateOf(false) }
    var focusGroupAfterDelete by remember { mutableStateOf(false) }
    
    // Saved across tab switches; keys keep the first visible TV card stable on reload.
    val tvGridState = rememberLazyGridState()
    val tvHistoryState = rememberLazyListState()

    // On TV a keyed grid may be restored deep in a named playlist. Reload that
    // playlist instead of resetting its cards/header to the default group.
    // The phone initialization remains exactly as before.
    LaunchedEffect(Unit) {
        if (DeviceUtils.isTv(context)) {
            groups = StorageUtils.loadAllFavoriteGroups(context)
            selectedGroup = groups.firstOrNull { it.id == tvSelectedGroupId }
                ?: groups.firstOrNull { it.isDefault } ?: groups.firstOrNull()
            tvSelectedGroupId = selectedGroup?.id
            favorites = if (selectedGroup != null && !selectedGroup!!.isDefault)
                StorageUtils.getFavoritesInGroup(context, selectedGroup!!.id)
            else StorageUtils.loadAllFavorites(context)
            // Do not measure a restored deep grid against a temporary 4-item
            // empty list before its saved playlist has finished loading.
            tvDataLoaded = true
        } else {
            favorites = StorageUtils.loadAllFavorites(context)
            groups = StorageUtils.loadAllFavoriteGroups(context)
            selectedGroup = groups.firstOrNull { it.isDefault } ?: groups.firstOrNull()
        }
    }
    
    // Confirmation dialog for deleting all favorites
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All Favorites") },
            text = { Text("Are you sure you want to delete all favorites? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = {
                        StorageUtils.clearAllFavorites(context)
                        favorites = emptyList()
                        if (DeviceUtils.isTv(context)) focusEmptyAfterClear = true
                        showDeleteAllDialog = false
                        // Show toast
                        android.widget.Toast.makeText(context, "All favorites deleted", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAllDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Dialog for creating a new group
    if (showCreateGroupDialog) {
        var groupName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                if (DeviceUtils.isTv(context)) {
                    TvSearchField(query = groupName,
                        placeholder = stringResource(R.string.playlist_name_hint),
                        onQueryChange = { groupName = it },
                        onSearch = {}, requestInitialFocus = true)
                } else OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text(stringResource(R.string.playlist_name_hint)) },
                    modifier = Modifier.tvDialogInitialFocus().fillMaxWidth()
                        .tvFocusBorder(RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent),
                    singleLine = true, // Prevent new lines
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (groupName.isNotBlank()) {
                                // Check if group with same name already exists
                                val existingGroup = groups.find { 
                                    it.name.equals(groupName, ignoreCase = true) && !it.isDefault
                                }
                                
                                if (existingGroup == null) {
                                    val newGroup = FavoriteGroup(
                                        id = UUID.randomUUID().toString(),
                                        name = groupName,
                                        isDefault = false
                                    )
                                    StorageUtils.saveFavoriteGroup(context, newGroup)
                                    groups = StorageUtils.loadAllFavoriteGroups(context)
                                    showCreateGroupDialog = false
                                    groupName = ""
                                    // Show success message
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Playlist created successfully", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Show error message
                                    android.widget.Toast.makeText(
                                        context, 
                                        "A playlist with this name already exists", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = {
                        if (groupName.isNotBlank()) {
                            // Check if group with same name already exists
                            val existingGroup = groups.find { 
                                it.name.equals(groupName, ignoreCase = true) && !it.isDefault
                            }
                            
                            if (existingGroup == null) {
                                val newGroup = FavoriteGroup(
                                    id = UUID.randomUUID().toString(),
                                    name = groupName,
                                    isDefault = false
                                )
                                StorageUtils.saveFavoriteGroup(context, newGroup)
                                groups = StorageUtils.loadAllFavoriteGroups(context)
                                showCreateGroupDialog = false
                                groupName = ""
                                // Show success message
                                android.widget.Toast.makeText(
                                    context, 
                                    "Playlist created successfully", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Show error message
                                android.widget.Toast.makeText(
                                    context, 
                                    "A playlist with this name already exists", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = { showCreateGroupDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Dialog for renaming a group
    var showRenameGroupDialog by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<FavoriteGroup?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    
    if (showRenameGroupDialog && groupToRename != null) {
        AlertDialog(
            onDismissRequest = { showRenameGroupDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                if (DeviceUtils.isTv(context)) {
                    TvSearchField(query = newGroupName,
                        placeholder = stringResource(R.string.playlist_new_name_hint),
                        onQueryChange = { newGroupName = it },
                        onSearch = {}, requestInitialFocus = true)
                } else OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text(stringResource(R.string.playlist_new_name_hint)) },
                    modifier = Modifier.tvDialogInitialFocus().fillMaxWidth()
                        .tvFocusBorder(RoundedCornerShape(12.dp)),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newGroupName.isNotBlank() && groupToRename != null) {
                                // Check if another group with same name already exists
                                val existingGroup = groups.find { 
                                    it.id != groupToRename!!.id && 
                                    it.name.equals(newGroupName, ignoreCase = true) && 
                                    !it.isDefault
                                }
                                
                                if (existingGroup == null) {
                                    val updatedGroup = groupToRename!!.copy(name = newGroupName)
                                    StorageUtils.saveFavoriteGroup(context, updatedGroup)
                                    groups = StorageUtils.loadAllFavoriteGroups(context)
                                    if (DeviceUtils.isTv(context) && selectedGroup?.id == updatedGroup.id)
                                        selectedGroup = groups.firstOrNull { it.id == updatedGroup.id }
                                    showRenameGroupDialog = false
                                    groupToRename = null
                                    newGroupName = ""
                                    // Show success message
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Playlist renamed successfully", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    // Show error message
                                    android.widget.Toast.makeText(
                                        context, 
                                        "A playlist with this name already exists", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = {
                        if (newGroupName.isNotBlank() && groupToRename != null) {
                            // Check if another group with same name already exists
                            val existingGroup = groups.find { 
                                it.id != groupToRename!!.id && 
                                it.name.equals(newGroupName, ignoreCase = true) && 
                                !it.isDefault
                            }
                            
                            if (existingGroup == null) {
                                val updatedGroup = groupToRename!!.copy(name = newGroupName)
                                StorageUtils.saveFavoriteGroup(context, updatedGroup)
                                groups = StorageUtils.loadAllFavoriteGroups(context)
                                if (DeviceUtils.isTv(context) && selectedGroup?.id == updatedGroup.id)
                                    selectedGroup = groups.firstOrNull { it.id == updatedGroup.id }
                                showRenameGroupDialog = false
                                groupToRename = null
                                newGroupName = ""
                                // Show success message
                                android.widget.Toast.makeText(
                                    context, 
                                    "Playlist renamed successfully", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                // Show error message
                                android.widget.Toast.makeText(
                                    context, 
                                    "A playlist with this name already exists", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = newGroupName.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = { 
                        showRenameGroupDialog = false
                        groupToRename = null
                        newGroupName = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Dialog for deleting a group
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<FavoriteGroup?>(null) }
    
    if (showDeleteGroupDialog && groupToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupDialog = false },
            title = { Text("Delete Playlist") },
            text = { 
                Text("Are you sure you want to delete the playlist \"${groupToDelete!!.name}\"? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = {
                        if (groupToDelete != null) {
                            StorageUtils.removeFavoriteGroup(context, groupToDelete!!.id)
                            groups = StorageUtils.loadAllFavoriteGroups(context)
                            if (DeviceUtils.isTv(context)) focusGroupAfterDelete = true
                            
                            // If the deleted group was the selected group, switch to default
                            if (selectedGroup?.id == groupToDelete!!.id) {
                                selectedGroup = groups.firstOrNull { it.isDefault }
                                if (DeviceUtils.isTv(context)) tvSelectedGroupId = selectedGroup?.id
                                favorites = StorageUtils.loadAllFavorites(context)
                            }
                            
                            showDeleteGroupDialog = false
                            groupToDelete = null
                            // Show success message
                            android.widget.Toast.makeText(
                                context, 
                                "Playlist deleted successfully", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteGroupDialog = false
                        groupToDelete = null
                    },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Dialog for moving item to groups (multi-select)
    if (showMoveToGroupDialog && selectedItem != null) {
        val item = selectedItem!!
        var selectedGroups by remember { mutableStateOf<List<String>>(emptyList()) }
        
        // Initialize selected groups
        LaunchedEffect(item) {
            val currentGroups = StorageUtils.getGroupsForFavorite(context, item.id, item.type)
            selectedGroups = currentGroups.map { it.id }
        }
        
        AlertDialog(
            onDismissRequest = { showMoveToGroupDialog = false },
            title = { Text("Select Playlists") },
            text = {
                LazyColumn {
                    items(groups.filter { !it.isDefault }, key = { it.id }) { group ->
                        val isChecked = selectedGroups.contains(group.id)
                        Row(
                            modifier = Modifier.fillMaxWidth().tvFocusBorder(RoundedCornerShape(0.dp))
                                .clickable {
                                    selectedGroups = if (isChecked)
                                        selectedGroups.filter { it != group.id }
                                    else selectedGroups + group.id
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // The row is the only focus target; OK on the checkbox toggles the row.
                            androidx.compose.material3.Checkbox(checked = isChecked,
                                onCheckedChange = null)
                            Text(
                                text = group.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.tvFocusBorder(CircleShape),
                    onClick = {
                        // Update group memberships
                        groups.filter { !it.isDefault }.forEach { group ->
                            if (selectedGroups.contains(group.id)) {
                                // Add to group if not already there
                                if (!StorageUtils.isFavoriteInGroup(context, group.id, item.id, item.type)) {
                                    StorageUtils.addFavoriteToGroup(context, group.id, item.id, item.type)
                                }
                            } else {
                                // Remove from group if it was there
                                if (StorageUtils.isFavoriteInGroup(context, group.id, item.id, item.type)) {
                                    StorageUtils.removeFavoriteFromGroup(context, group.id, item.id, item.type)
                                }
                            }
                        }
                        
                        // Refresh data
                        groups = StorageUtils.loadAllFavoriteGroups(context)
                        favorites = if (selectedGroup != null && !selectedGroup!!.isDefault) {
                            StorageUtils.getFavoritesInGroup(context, selectedGroup!!.id)
                        } else {
                            StorageUtils.loadAllFavorites(context)
                        }
                        
                        showMoveToGroupDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showMoveToGroupDialog = false },
                    modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (DeviceUtils.isTv(context) && !tvDataLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (DeviceUtils.isTv(context)) {
        TvFavoritesContent(
            navController = navController, favorites = favorites, groups = groups,
            selectedGroup = selectedGroup, showHistory = showHistory,
            moveDialogOpen = showMoveToGroupDialog,
            gridState = tvGridState, historyState = tvHistoryState,
            restoreEmptyFocus = focusEmptyAfterClear,
            restoreGroupFocus = focusGroupAfterDelete,
            onEmptyRestored = { focusEmptyAfterClear = false },
            onGroupRestored = { focusGroupAfterDelete = false },
            onTabSelected = { showHistory = it },
            onGroupSelected = { group ->
                selectedGroup = group
                tvSelectedGroupId = group.id
                favorites = if (group.isDefault) StorageUtils.loadAllFavorites(context)
                    else StorageUtils.getFavoritesInGroup(context, group.id)
            },
            onCreateGroup = { showCreateGroupDialog = true },
            onDeleteAll = { showDeleteAllDialog = true },
            onRenameGroup = { group ->
                groupToRename = group
                newGroupName = group.name
                showRenameGroupDialog = true
            },
            onDeleteGroup = { group ->
                groupToDelete = group
                showDeleteGroupDialog = true
            },
            onMoveToGroup = { item ->
                selectedItem = item
                showMoveToGroupDialog = true
            },
            onFavoritesChanged = { favorites = it }
        )
    } else {
        val commonHeader: @Composable () -> Unit = {
            Column {
        // Top App Bar
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            Column {
                MainSectionHeader(
                    title = if (showHistory) stringResource(R.string.last_watched)
                        else selectedGroup?.name ?: stringResource(R.string.favorites),
                    navController = navController
                )
                // Playlist actions stay visible without competing with the centered logo.
                if (!showHistory) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End) {
                        val createInteraction = remember { MutableInteractionSource() }
                        val createFocused by createInteraction.collectIsFocusedAsState()
                        IconButton(onClick = { showCreateGroupDialog = true },
                            interactionSource = createInteraction,
                            modifier = Modifier.tvFocusItem("favorite-create")
                                .size(48.dp).settingsFocusBorder(createFocused, CircleShape)) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_playlist),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        if (favorites.isNotEmpty()) {
                            val deleteInteraction = remember { MutableInteractionSource() }
                            val deleteFocused by deleteInteraction.collectIsFocusedAsState()
                            IconButton(onClick = { showDeleteAllDialog = true },
                                interactionSource = deleteInteraction,
                                modifier = Modifier.tvFocusItem("favorite-delete-all")
                                    .size(48.dp).settingsFocusBorder(deleteFocused, CircleShape)) {
                                Icon(Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_all_favorites),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryTabButton(
                text = stringResource(R.string.favorites), selected = !showHistory,
                onClick = { showHistory = false },
                modifier = Modifier.tvFocusItem("favorite-tab-playlists").weight(1f)
            )
            LibraryTabButton(
                text = stringResource(R.string.last_watched), selected = showHistory,
                onClick = { showHistory = true },
                modifier = Modifier.tvFocusItem("favorite-tab-history").weight(1f)
            )
        }

            }
        }
        val playlistHeader: @Composable () -> Unit = {
            Column {
                commonHeader()
        // Group selector
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(animationSpec = tween(400, delayMillis = 100)),
            exit = fadeOut(animationSpec = tween(400)) + slideOutVertically(animationSpec = tween(400))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(R.string.playlists),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                    }
                    
                    // Display groups as selectable chips
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            Box {
                                var showGroupMenu by remember { mutableStateOf(false) }
                                val chipInteraction = remember(group.id) { MutableInteractionSource() }
                                val chipFocused by chipInteraction.collectIsFocusedAsState()
                                Card(
                                    onClick = {
                                        selectedGroup = group
                                        favorites = if (group.isDefault)
                                            StorageUtils.loadAllFavorites(context)
                                        else StorageUtils.getFavoritesInGroup(context, group.id)
                                    },
                                    interactionSource = chipInteraction,
                                    modifier = Modifier.tvFocusItem("favorite-group-${group.id}")
                                        .padding(vertical = 4.dp)
                                        .settingsFocusBorder(chipFocused, RoundedCornerShape(16.dp)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (selectedGroup?.id == group.id) 4.dp else 0.dp),
                                    colors = if (selectedGroup?.id == group.id) {
                                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        CardDefaults.cardColors()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = group.name)
                                        
                                        // Show menu icon for non-default groups
                                        if (!group.isDefault) {
                                            IconButton(
                                                onClick = { showGroupMenu = true },
                                                modifier = Modifier.tvFocusItem("favorite-group-menu-${group.id}")
                                                    .size(if (DeviceUtils.isTv(context)) 48.dp else 24.dp)
                                                    .tvFocusBorder(RoundedCornerShape(8.dp))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "Group options",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Group menu for rename/delete options
                                DropdownMenu(
                                    expanded = showGroupMenu,
                                    onDismissRequest = { showGroupMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(),
                                        onClick = {
                                            groupToRename = group
                                            newGroupName = group.name
                                            showRenameGroupDialog = true
                                            showGroupMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        modifier = Modifier.tvFocusBorder(),
                                        onClick = {
                                            groupToDelete = group
                                            showDeleteGroupDialog = true
                                            showGroupMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
            }
        }
        if (showHistory) {
            LastWatchedPanel(modifier = Modifier.fillMaxSize(), phoneHeader = commonHeader)
        } else {
        // Content
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500, delayMillis = 200)),
            exit = fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500))
        ) {
            if (favorites.isEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    item(key = "header") { playlistHeader() }
                    item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 16.dp)
                        )
                        Text(text = stringResource(R.string.no_favorites_yet))
                    }
                }
                    }
                }
            } else {
                // State variables for the confirmation dialog
                var showRemoveFavoriteDialog by remember { mutableStateOf(false) }
                var favoriteToRemove by remember { mutableStateOf<FavoriteItem?>(null) }
                
                // Confirmation dialog for removing from favorites
                if (showRemoveFavoriteDialog && favoriteToRemove != null) {
                    AlertDialog(
                        onDismissRequest = { showRemoveFavoriteDialog = false },
                        title = { Text("Remove from Favorites") },
                        text = { Text("Are you sure you want to remove \"${favoriteToRemove!!.title}\" from your favorites?") },
                        confirmButton = {
                            TextButton(
                                modifier = Modifier.tvFocusBorder(CircleShape),
                                onClick = {
                                    StorageUtils.removeFavorite(context, favoriteToRemove!!.id, favoriteToRemove!!.type)
                                    // Refresh the favorites list
                                    favorites = if (selectedGroup != null && !selectedGroup!!.isDefault) {
                                        StorageUtils.getFavoritesInGroup(context, selectedGroup!!.id)
                                    } else {
                                        StorageUtils.loadAllFavorites(context)
                                    }
                                    showRemoveFavoriteDialog = false
                                    favoriteToRemove = null
                                    // Show toast
                                    android.widget.Toast.makeText(context, "Removed from favorites", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Remove")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { 
                                    showRemoveFavoriteDialog = false
                                    favoriteToRemove = null
                                },
                                modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(CircleShape)
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                Card(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(DeviceUtils.getGridColumns(context)),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { playlistHeader() }
                        items(favorites, key = { "${it.type}:${it.id}" }) { favorite ->
                                FavoriteItemCard(
                                    favorite = favorite,
                                    onClick = {
                                        // Save the favorite to the appropriate database before navigating
                                        StorageUtils.saveFavoriteToDatabase(context, favorite)
                                        
                                        // Navigate to the appropriate screen based on type
                                        when (favorite.type) {
                                            "movie" -> {
                                                navController.navigate("${AppScreens.SingleMovie.route.replace("{movieId}", favorite.id.toString())}")
                                            }
                                            "series" -> {
                                                navController.navigate("${AppScreens.SingleSeries.route.replace("{seriesId}", favorite.id.toString())}")
                                            }
                                        }
                                    },
                                    onDelete = {
                                        favoriteToRemove = favorite
                                        showRemoveFavoriteDialog = true
                                    },
                                    onMoveToGroup = { item ->
                                        selectedItem = item
                                        showMoveToGroupDialog = true
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

@Composable
fun FavoriteItemCard(
    favorite: FavoriteItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveToGroup: (FavoriteItem) -> Unit,
    modifier: Modifier = Modifier,
    cardFocusRequester: FocusRequester? = null,
    thinTvFocus: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var restoreAfterMenu by remember { mutableStateOf(false) }
    var focusedCard by remember { mutableStateOf(false) }
    var consumedLongPress by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val menuInteraction = remember { MutableInteractionSource() }
    val menuFocused by menuInteraction.collectIsFocusedAsState()

    LaunchedEffect(showMenu, restoreAfterMenu) {
        if (!showMenu) consumedLongPress = false
        if (!showMenu && restoreAfterMenu && cardFocusRequester != null) {
            withFrameNanos { }
            try { cardFocusRequester.requestFocus() } catch (_: IllegalStateException) { }
            restoreAfterMenu = false
        }
    }
    val tvModifier = if (cardFocusRequester != null) modifier
        .focusRequester(cardFocusRequester)
        .onFocusChanged { focusedCard = it.hasFocus }
        .onPreviewKeyEvent { event ->
            val code = event.nativeKeyEvent.keyCode
            val isOk = code == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                code == android.view.KeyEvent.KEYCODE_ENTER
            if (event.type == KeyEventType.KeyUp && isOk && consumedLongPress) {
                consumedLongPress = false
                return@onPreviewKeyEvent true // Never click after opening actions with long OK.
            }
            if (!focusedCard) return@onPreviewKeyEvent false
            when {
                code == android.view.KeyEvent.KEYCODE_MENU -> {
                    if (event.type == KeyEventType.KeyDown) {
                        showMenu = true
                        restoreAfterMenu = true
                    }
                    true
                }
                isOk -> {
                    if (event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.repeatCount > 0) {
                        consumedLongPress = true
                        showMenu = true
                        restoreAfterMenu = true
                        true
                    } else false
                }
                else -> false
            }
        } else modifier

    CompactPosterCard(
        title = favorite.title, image = favorite.image, imdb = favorite.imdb,
        year = favorite.year,
        typeLabel = stringResource(if (favorite.type == "movie") R.string.card_movie else R.string.card_series),
        onClick = onClick, focusKey = "favorite-${favorite.type}-${favorite.id}",
        modifier = tvModifier, thinTvFocus = thinTvFocus,
        overlay = {
            Box(Modifier.align(Alignment.BottomEnd)) {
                IconButton(
                    onClick = { showMenu = true; restoreAfterMenu = true }, interactionSource = menuInteraction,
                    modifier = Modifier.tvFocusItem("favorite-menu-${favorite.type}-${favorite.id}")
                        .size(if (DeviceUtils.isTv(context)) 48.dp else 34.dp)
                        .settingsFocusBorder(menuFocused, RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.favorite_more_options),
                        tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.favorite_move_playlist)) },
                        modifier = Modifier.tvDialogInitialFocus().tvFocusBorder(),
                        onClick = { showMenu = false; restoreAfterMenu = false; onMoveToGroup(favorite) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.favorite_delete)) },
                        modifier = Modifier.tvFocusBorder(),
                        onClick = { showMenu = false; restoreAfterMenu = false; onDelete() })
                }
            }
        }
    )
}
