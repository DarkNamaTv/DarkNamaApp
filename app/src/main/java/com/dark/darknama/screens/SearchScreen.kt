package com.dark.darknama.screens

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import com.dark.darknama.ui.theme.GlassCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.dark.darknama.R
import com.dark.darknama.navigation.tvFocusItem
import com.dark.darknama.components.tvBringIntoViewOnFocus
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.components.goldFocusColor
import com.dark.darknama.components.SettingsIconButton
import com.dark.darknama.components.copyTitleOnClick
import com.dark.darknama.components.CompactPosterCard
import com.dark.darknama.data.model.Country
import com.dark.darknama.data.model.Poster
import com.dark.darknama.ui.search.SearchViewModel
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.StorageUtils

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val submitFocusRequester = remember { FocusRequester() }
    val isTv = remember(context) { DeviceUtils.isTv(context) }
    
    // Request focus when the screen is first displayed (mobile only; on TV the
    // native EditText handles its own focus and IME)
    LaunchedEffect(Unit) {
        if (!isTv) {
            // The search field is now a lazy header and may not be attached
            // until the first layout frame.
            androidx.compose.runtime.withFrameNanos { }
            try { focusRequester.requestFocus() } catch (_: IllegalStateException) { }
        }
    }
    
    if (isTv) {
        TvSearchResultsContent(viewModel, navController, context)
        return
    }

    val phoneHeader: @Composable () -> Unit = {
    Column(Modifier.fillMaxWidth()) {
        // Top header with title and settings gear
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.search),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SettingsIconButton(navController = navController,
                modifier = Modifier.tvFocusItem("search-settings", initial = true))
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search bar
        if (isTv) {
            // On Android TV, the Compose TextField does not integrate correctly
            // with the TV IME: D-pad events leave the on-screen keyboard and move
            // focus back to the app. A native EditText integrates properly with
            // the leanback keyboard, so we use one via AndroidView on TV.
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvSearchField(
                    query = viewModel.searchQuery,
                    placeholder = stringResource(R.string.search_placeholder),
                    modifier = Modifier.weight(1f),
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = {
                        if (viewModel.searchQuery.isNotBlank()) viewModel.triggerSearch()
                    }
                )
                IconButton(onClick = {
                    if (viewModel.searchQuery.isNotBlank()) viewModel.triggerSearch()
                }, modifier = Modifier.tvFocusItem("search-submit")
                    .focusRequester(submitFocusRequester).size(48.dp).tvFocusBorder(CircleShape)) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
                if (viewModel.searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        // The clear button disappears with the query; move focus to
                        // the still-present search action before removing it.
                        submitFocusRequester.requestFocus()
                        viewModel.clearSearch()
                    }, modifier = Modifier.tvFocusItem("search-clear")
                            .size(48.dp).tvFocusBorder(CircleShape)) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            }
        } else {
            TextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .tvFocusBorder(RoundedCornerShape(24.dp)),
                placeholder = { 
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                },
                leadingIcon = {
                    IconButton(onClick = { 
                        // Trigger search when clicking search icon
                        if (viewModel.searchQuery.isNotEmpty()) {
                            viewModel.triggerSearch()
                            focusManager.clearFocus()
                        }
                    }, modifier = Modifier.tvFocusBorder(CircleShape)) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.clearSearch()
                            focusManager.clearFocus() // Dismiss keyboard when clearing search
                        }, modifier = Modifier.tvFocusBorder(CircleShape)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.search_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        // Trigger search when pressing Enter
                        if (viewModel.searchQuery.isNotEmpty()) {
                            viewModel.triggerSearch()
                        }
                        focusManager.clearFocus()
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
        }
        
        // Country stories section - only visible when no search has been performed
        if (!viewModel.hasSearched) {
            if (viewModel.isCountriesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (viewModel.countries.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.countries, key = { it.id }) { country ->
                        CountryStoryItem(
                            country = country,
                            onClick = {
                                // Navigate to country screen
                                navController?.navigate("country/${country.id}")
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
    }
    }
    if (!viewModel.isLoading && viewModel.errorMessage == null && viewModel.searchResults.isNotEmpty()) {
        SearchResultsGrid(viewModel.searchResults, navController, context, header = phoneHeader)
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
            item(key = "header") { phoneHeader() }
            item(key = "state") {
        // Search results
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            viewModel.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${viewModel.errorMessage}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.try_again),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.triggerSearch() },
                            modifier = Modifier.tvFocusItem("search-retry")
                                .padding(16.dp).tvFocusBorder(CircleShape)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            // Only show "No results found" after a search has been performed
            viewModel.hasSearched && viewModel.searchQuery.isNotEmpty() && !viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // Initial state - show search instructions
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.search_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.search_hint_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
            }
        }
    }
}

/**
 * Native EditText based search field for Android TV.
 *
 * The Compose TextField does not integrate correctly with the leanback IME:
 * while the on-screen keyboard is open, D-pad navigation events escape the
 * keyboard and move focus around the underlying app. A classic EditText
 * participates in the native View focus system, so the TV keyboard keeps
 * D-pad events for itself until the user confirms/dismisses it.
 */
@Composable
internal fun TvSearchField(
    query: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    requestInitialFocus: Boolean = false
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    // The Android TV IME requires a native EditText; use the same fixed gold as Compose focus.
    val focusedStrokeColor = goldFocusColor.toArgb()

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            EditText(ctx).apply {
                hint = placeholder
                setText(query)
                setSelection(query.length)
                isSingleLine = true
                inputType = InputType.TYPE_CLASS_TEXT
                imeOptions = EditorInfo.IME_ACTION_SEARCH or
                    EditorInfo.IME_FLAG_NO_EXTRACT_UI
                isFocusable = true
                isFocusableInTouchMode = true
                setTextColor(textColor)
                setHintTextColor(hintColor)
                textSize = 16f
                val density = ctx.resources.displayMetrics.density
                val padH = (20 * density).toInt()
                val padV = (14 * density).toInt()
                setPadding(padH, padV, padH, padV)

                fun buildBackground(focused: Boolean): android.graphics.drawable.Drawable {
                    val base = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 24 * density
                        setColor(containerColor)
                        if (focused) setStroke((8 * density).toInt(), android.graphics.Color.BLACK)
                    }
                    if (!focused) return base
                    val gold = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 24 * density
                        setColor(android.graphics.Color.TRANSPARENT)
                        setStroke((2.5f * density).toInt().coerceAtLeast(1), focusedStrokeColor)
                    }
                    // Native Android TV IME fallback: same gold + dark edge, no Compose blur.
                    return android.graphics.drawable.LayerDrawable(arrayOf(base, gold))
                }

                background = buildBackground(false)
                setOnFocusChangeListener { view, hasFocus ->
                    // AndroidView hosts a native focus target, so Compose's input-mode
                    // modifier cannot observe it. Do not draw gold for touch focus.
                    background = buildBackground(hasFocus && !view.isInTouchMode)
                }
                setOnTouchListener { _, event ->
                    if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) {
                        background = buildBackground(false)
                    }
                    false // Preserve cursor movement and the native TV keyboard.
                }
                setOnKeyListener { _, keyCode, event ->
                    val navigationKey = when (keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_UP, AndroidKeyEvent.KEYCODE_DPAD_DOWN,
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT, AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER, AndroidKeyEvent.KEYCODE_TAB -> true
                        else -> false
                    }
                    if (navigationKey && event.action == AndroidKeyEvent.ACTION_DOWN) {
                        background = buildBackground(true)
                    }
                    false // The EditText/IME still receives the key.
                }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?, start: Int, count: Int, after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?, start: Int, before: Int, count: Int
                    ) {}

                    override fun afterTextChanged(s: Editable?) {
                        onQueryChange(s?.toString() ?: "")
                    }
                })

                setOnEditorActionListener { _, actionId, keyEvent ->
                    if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                        actionId == EditorInfo.IME_ACTION_DONE ||
                        (keyEvent?.keyCode == AndroidKeyEvent.KEYCODE_ENTER &&
                            keyEvent.action == AndroidKeyEvent.ACTION_UP)) {
                        onSearch()
                        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                            ?.hideSoftInputFromWindow(windowToken, 0)
                        true
                    } else false
                }
                if (requestInitialFocus) post { requestFocus() }
            }
        },
        update = { editText ->
            // Keep the EditText in sync when the query is changed externally
            // (e.g. clear search) without disturbing the cursor while typing.
            if (editText.text.toString() != query) {
                editText.setText(query)
                editText.setSelection(query.length)
            }
        }
    )
}

@Composable
fun CountryStoryItem(
    country: com.dark.darknama.data.model.Country,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.tvFocusItem("search-country-${country.id}")
            .tvBringIntoViewOnFocus().tvFocusBorder(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(country.image)
                        .crossfade(true)
                        .build()
                ),
                contentDescription = country.title,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = country.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(70.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SearchResultsGrid(
    posters: List<Poster>,
    navController: NavController?,
    context: Context,
    header: @Composable () -> Unit = {}
) {
    val columns = DeviceUtils.getGridColumns(LocalContext.current)
    // API pages can repeat a title. Keep each focus target and lazy key unique.
    val uniquePosters = remember(posters) { posters.distinctBy { "${it.type}:${it.id}" } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) { header() }
        items(uniquePosters, key = { "${it.type}:${it.id}" }) { poster ->
            PosterItem(
                poster = poster,
                onClick = {
                    if (poster.isMovie()) {
                        // Save movie to storage and navigate to single movie screen
                        StorageUtils.saveMovieToFile(context, poster.toMovie())
                        navController?.navigate("single_movie/${poster.id}")
                    } else if (poster.isSeries()) {
                        // Save series to storage and navigate to single series screen
                        StorageUtils.saveSeriesToFile(context, poster.toSeries())
                        navController?.navigate("single_series/${poster.id}")
                    }
                }
            )
        }
    }
}

@Composable
fun PosterItem(poster: Poster, onClick: () -> Unit) {
    CompactPosterCard(
        title = poster.title, image = poster.image, imdb = poster.imdb,
        year = poster.year,
        typeLabel = stringResource(if (poster.isMovie()) R.string.card_movie else R.string.card_series),
        onClick = onClick, focusKey = "search-${poster.type}-${poster.id}"
    )
}
