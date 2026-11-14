package com.dark.darknama.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import com.dark.darknama.ui.theme.GlassDropdownMenu as DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Genre
import com.dark.darknama.R
import com.dark.darknama.navigation.tvDialogInitialFocus
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.utils.DeviceUtils

@Composable
fun GenreFilterSection(
    genres: List<Genre>,
    selectedGenreId: Int,
    selectedFilterType: FilterType,
    onGenreSelected: (Int) -> Unit,
    onFilterTypeSelected: (FilterType) -> Unit,
    sortFocusRequester: FocusRequester? = null,
    headerFocusRequester: FocusRequester? = null
) {
    val isTv = DeviceUtils.isTv(LocalContext.current)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (!isTv) Text(
            text = stringResource(R.string.filters_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Filter row with filter type on left and genre selector on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(if (isTv) 12.dp else 8.dp)
        ) {
            // Filter type selector on the left
            FilterTypeSelector(
                selectedFilterType = selectedFilterType,
                onFilterTypeSelected = onFilterTypeSelected,
                modifier = (if (isTv && headerFocusRequester != null)
                    Modifier.focusProperties { up = headerFocusRequester } else Modifier)
                    .then(if (isTv && sortFocusRequester != null)
                        Modifier.focusRequester(sortFocusRequester) else Modifier)
            )
            
            // Genre selector on the right
            GenreSelector(
                genres = genres,
                selectedGenreId = selectedGenreId,
                onGenreSelected = onGenreSelected,
                modifier = if (isTv && headerFocusRequester != null)
                    Modifier.focusProperties { up = headerFocusRequester } else Modifier
            )
        }
    }
}

@Composable
fun FilterTypeSelector(
    selectedFilterType: FilterType,
    onFilterTypeSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val openMenu = { expanded = true }
    val isTv = DeviceUtils.isTv(LocalContext.current)

    Card(
        onClick = if (isTv) openMenu else null,
        modifier = modifier.tvFocusItem("filter-sort")
            .width(150.dp).height(36.dp)
            .tvBringIntoViewOnFocus().tvFocusBorder(RoundedCornerShape(18.dp))
            .then(if (isTv) Modifier else Modifier.clickable(onClick = openMenu)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort_label, stringResource(when (selectedFilterType) {
                        FilterType.DEFAULT -> R.string.sort_default
                        FilterType.BY_YEAR -> R.string.sort_by_year
                        FilterType.BY_IMDB -> R.string.sort_by_imdb
                    })),
                    style = if (isTv) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.filters_sort_options),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(16.dp)
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_default),
                        color = if (selectedFilterType == FilterType.DEFAULT)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    modifier = (if (selectedFilterType == FilterType.DEFAULT)
                        Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                    onClick = {
                        onFilterTypeSelected(FilterType.DEFAULT)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_by_year),
                        color = if (selectedFilterType == FilterType.BY_YEAR)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    modifier = (if (selectedFilterType == FilterType.BY_YEAR)
                        Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                    onClick = {
                        onFilterTypeSelected(FilterType.BY_YEAR)
                        expanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.sort_by_imdb),
                        color = if (selectedFilterType == FilterType.BY_IMDB)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    modifier = (if (selectedFilterType == FilterType.BY_IMDB)
                        Modifier.tvDialogInitialFocus() else Modifier).tvFocusBorder(),
                    onClick = {
                        onFilterTypeSelected(FilterType.BY_IMDB)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GenreSelector(
    genres: List<Genre>,
    selectedGenreId: Int,
    onGenreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val openMenu = { expanded = true }
    val isTv = DeviceUtils.isTv(LocalContext.current)

    // Find the selected genre title
    val selectedGenreTitle = if (selectedGenreId == 0) {
        stringResource(R.string.genre_all)
    } else {
        genres.find { it.id == selectedGenreId }?.title ?: stringResource(R.string.genre_all)
    }
    
    Card(
        onClick = if (isTv) openMenu else null,
        modifier = modifier.tvFocusItem("filter-genre")
            .width(150.dp).height(36.dp)
            .tvBringIntoViewOnFocus().tvFocusBorder(RoundedCornerShape(18.dp))
            .then(if (isTv) Modifier else Modifier.clickable(onClick = openMenu)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedGenreTitle,
                    style = if (isTv) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.filters_genre_options),
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.height(16.dp)
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.genre_all), color = if (selectedGenreId == 0)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    modifier = (if (selectedGenreId == 0) Modifier.tvDialogInitialFocus()
                        else Modifier).tvFocusBorder(),
                    onClick = {
                        onGenreSelected(0)
                        expanded = false
                    }
                )
                
                genres.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre.title, color = if (genre.id == selectedGenreId)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                        modifier = (if (genre.id == selectedGenreId) Modifier.tvDialogInitialFocus()
                            else Modifier).tvFocusBorder(),
                        onClick = {
                            onGenreSelected(genre.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}