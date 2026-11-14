package com.dark.darknama

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import com.dark.darknama.ui.theme.GlassAlertDialog
import com.dark.darknama.navigation.LocalSidebarFocusRequester
import com.dark.darknama.navigation.LocalTvFocusRegistry
import com.dark.darknama.navigation.TvFocusRegistry
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dark.darknama.components.UpdateDialog
import com.dark.darknama.components.tvFocusBorder
import com.dark.darknama.ui.settings.UpdateViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.dark.darknama.navigation.AppNavigation
import com.dark.darknama.navigation.AppScreens
import com.dark.darknama.navigation.BottomNavigationBar
import com.dark.darknama.navigation.SidebarNavigation
import com.dark.darknama.ui.theme.DarkNamaTheme
import com.dark.darknama.ui.theme.LocalVisualStyle
import com.dark.darknama.ui.theme.VisualStyle
import com.dark.darknama.ui.theme.ThemeManager
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.utils.StorageUtils
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.repository.ServerSwitchEvents
import com.dark.darknama.utils.DeviceUtils
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.dark.darknama.utils.LanguageUtils.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set default orientation to portrait for mobile/tablet
        // For TV, we don't set orientation as it's typically fixed
        if (!DeviceUtils.isTv(this)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        
        // Only use edge-to-edge on supported versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            enableEdgeToEdge()
        }
        
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATE, false) == true) {
            ViewModelProvider(this)[UpdateViewModel::class.java].openUpdate()
        }
        setContent {
            MainApp()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_UPDATE, false)) {
            ViewModelProvider(this)[UpdateViewModel::class.java].openUpdate()
        }
    }

    companion object {
        const val EXTRA_OPEN_UPDATE = "com.dark.darknama.OPEN_UPDATE"
    }
}

@Composable
fun MainApp() {
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    var themeSettings by remember { mutableStateOf(themeManager.loadThemeSettings()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var fontSettings by remember { mutableStateOf(StorageUtils.loadFontSettings(context)) }

    LaunchedEffect(context) {
        ServerSwitchEvents.switches.collect { number ->
            Toast.makeText(
                context, context.getString(R.string.switched_to_server, number), Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Save theme settings when they change
    LaunchedEffect(themeSettings) {
        themeManager.saveThemeSettings(themeSettings)
    }
    
    DarkNamaTheme(themeSettings, fontSettings) {
        MainScreen(
            onThemeSettingsChanged = { themeSettings = it },
            onFontSettingsChanged = { fontSettings = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {},
    onFontSettingsChanged: (FontSettings) -> Unit = {}
) {
    val navController = rememberNavController()
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    val themeSettings = themeManager.loadThemeSettings()
    val context = androidx.compose.ui.platform.LocalContext.current
    val fontSettings = StorageUtils.loadFontSettings(context)
    val configuration = LocalConfiguration.current
    val isTv = remember(context, configuration) { DeviceUtils.isTv(context) }
    val tvFocusRegistry = remember { TvFocusRegistry() }
    val sidebarFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val direction = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showExitDialog by remember { mutableStateOf(false) }
    var exitDialogOpened by remember { mutableStateOf(false) }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val entryId = currentBackStackEntry?.id
    val updates: UpdateViewModel = viewModel(viewModelStoreOwner = context as ComponentActivity)
    val updateState by updates.state.collectAsState()
    val automaticChecks by updates.automaticChecks.collectAsState()

    LaunchedEffect(currentRoute, automaticChecks) {
        if (currentRoute == AppScreens.Movies.route && automaticChecks) updates.checkAutomatically()
    }
    LaunchedEffect(updateState.upToDateNotice) {
        if (updateState.upToDateNotice) {
            Toast.makeText(context, context.getString(R.string.app_up_to_date), Toast.LENGTH_SHORT).show()
            updates.consumeUpToDateNotice()
        }
    }

    // Wait for the destination's real controls to attach before restoring its last focused item.
    // This also restores the underlying control after the shared update dialog closes.
    LaunchedEffect(entryId, isTv, updateState.showDialog) {
        if (isTv && !updateState.showDialog) repeat(4) {
            withFrameNanos { }
            if (tvFocusRegistry.restore(entryId)) return@LaunchedEffect
        }
    }
    DisposableEffect(lifecycleOwner, entryId, isTv) {
        val observer = LifecycleEventObserver { _, event ->
            if (isTv && event == Lifecycle.Event.ON_RESUME) scope.launch {
                withFrameNanos { }
                tvFocusRegistry.restore(entryId) // Returning from the separate player activity.
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(enabled = isTv && !showExitDialog && !updateState.showDialog &&
        entryId != null && currentRoute != AppScreens.Splash.route &&
        navController.previousBackStackEntry == null) { showExitDialog = true }
    BackHandler(enabled = isTv && showExitDialog) { showExitDialog = false }
    LaunchedEffect(showExitDialog) {
        if (showExitDialog) exitDialogOpened = true
        else if (exitDialogOpened && isTv) {
            exitDialogOpened = false
            withFrameNanos { }
            tvFocusRegistry.restore(entryId)
        }
    }

    // Find the current screen, including the SingleMovie and SingleSeries screens
    val currentScreen = when {
        currentRoute?.startsWith("single_movie") == true -> AppScreens.SingleMovie
        currentRoute?.startsWith("single_series") == true -> AppScreens.SingleSeries
        currentRoute?.startsWith("country") == true -> AppScreens.Country
        currentRoute == "favorites" -> AppScreens.Favorites
        currentRoute == AppScreens.ThemeSettings.route -> AppScreens.ThemeSettings
        currentRoute == AppScreens.PlayerSettings.route -> AppScreens.PlayerSettings
        currentRoute == AppScreens.ServerSettings.route -> AppScreens.ServerSettings
        currentRoute == AppScreens.FontSettings.route -> AppScreens.FontSettings
        currentRoute == "settings" -> AppScreens.Settings
        currentRoute == "about" -> AppScreens.About
        else -> AppScreens.screens.find { it.route == currentRoute } ?: AppScreens.Movies
    }
    
    // System UI controller for edge-to-edge support
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = themeSettings.themeMode != com.dark.darknama.ui.theme.ThemeMode.DARK
    
    LaunchedEffect(themeSettings) {
        systemUiController.setStatusBarColor(
            color = androidx.compose.ui.graphics.Color.Transparent,
            darkIcons = useDarkIcons
        )
        systemUiController.setNavigationBarColor(
            color = androidx.compose.ui.graphics.Color.Transparent,
            darkIcons = useDarkIcons
        )
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = if (LocalVisualStyle.current == VisualStyle.DEFAULT)
            MaterialTheme.colorScheme.background else Color.Transparent,
        bottomBar = {
            // Only show bottom bar if the current screen requires it and we're not on splash
            // and we're not on TV (TV uses sidebar instead)
            if (!isTv && currentScreen.showBottomBar && currentRoute != AppScreens.Splash.route) {
                BottomNavigationBar(navController)
            }
        }
    ) { innerPadding ->
        if (isTv) {
            // TV layout with sidebar navigation
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (currentRoute != AppScreens.Splash.route && currentRoute != null) {
                    SidebarNavigation(navController, sidebarFocus,
                        onEnterContent = { tvFocusRegistry.restore(entryId) })
                }

                // Unhandled horizontal keys move through content, then reach the rail at its edge.
                // In RTL the rail is on the right; keep phone key behavior untouched.
                val towardRail = if (direction == LayoutDirection.Ltr) Key.DirectionLeft else Key.DirectionRight
                val focusDirection = if (direction == LayoutDirection.Ltr) FocusDirection.Left else FocusDirection.Right
                Box(
                    modifier = Modifier.weight(1f)
                        .padding(start = 16.dp, end = 24.dp)
                        // Scrolling TV sections own their vertical safe area; detail and
                        // settings screens retain the original inset.
                        .padding(vertical = if (currentRoute in listOf(
                            AppScreens.Movies.route, AppScreens.Series.route,
                            AppScreens.Search.route, AppScreens.LiveTv.route,
                            AppScreens.Favorites.route, AppScreens.Country.route
                        )) 0.dp else 24.dp)
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == towardRail &&
                                currentRoute != null && currentRoute != AppScreens.Splash.route) {
                                if (!focusManager.moveFocus(focusDirection)) {
                                    try { sidebarFocus.requestFocus() } catch (_: IllegalStateException) { }
                                }
                                true
                            } else false
                        }
                ) {
                    CompositionLocalProvider(
                        LocalTvFocusRegistry provides tvFocusRegistry,
                        LocalSidebarFocusRequester provides sidebarFocus
                    ) {
                        AppNavigation(navController, onThemeSettingsChanged, onFontSettingsChanged)
                    }
                }
            }
        } else {
            // Mobile/tablet layout with bottom navigation
            Box(modifier = Modifier.padding(innerPadding)) {
                // Pass theme settings and font settings callback to navigation
                AppNavigation(navController, onThemeSettingsChanged, onFontSettingsChanged)
            }
        }
    }
    UpdateDialog(updates)
    if (isTv && showExitDialog) {
        val cancelFocus = remember { FocusRequester() }
        LaunchedEffect(showExitDialog) {
            withFrameNanos { }
            try { cancelFocus.requestFocus() } catch (_: IllegalStateException) { }
        }
        GlassAlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.tv_exit_title)) },
            text = { Text(stringResource(R.string.tv_exit_message)) },
            confirmButton = {
                TextButton(onClick = { (context as? ComponentActivity)?.finish() },
                    modifier = Modifier.tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.tv_exit_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false },
                    modifier = Modifier.focusRequester(cancelFocus).tvFocusBorder(CircleShape)) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val themeManager = ThemeManager(androidx.compose.ui.platform.LocalContext.current)
    val themeSettings = themeManager.loadThemeSettings()
    
    DarkNamaTheme(themeSettings) {
        MainScreen()
    }
}