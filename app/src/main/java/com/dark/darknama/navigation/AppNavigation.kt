package com.dark.darknama.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.screens.AboutScreen
import com.dark.darknama.screens.CountryScreen
import com.dark.darknama.screens.FavoritesScreen
import com.dark.darknama.screens.LiveTvScreen
import com.dark.darknama.screens.MoviesScreen
import com.dark.darknama.screens.SearchScreen
import com.dark.darknama.screens.SeriesScreen
import com.dark.darknama.screens.SettingsScreen
import com.dark.darknama.screens.SingleMovieScreen
import com.dark.darknama.screens.SingleSeriesScreen
import com.dark.darknama.screens.SplashScreen
import com.dark.darknama.screens.settings.FontSettingsScreen
import com.dark.darknama.screens.settings.PlayerSettingsScreen
import com.dark.darknama.screens.settings.ServerSettingsScreen
import com.dark.darknama.screens.settings.ThemeSettingsScreen
import com.dark.darknama.ui.country.CountryViewModel
import com.dark.darknama.ui.movies.MoviesViewModel
import com.dark.darknama.ui.search.SearchViewModel
import com.dark.darknama.ui.series.SeriesViewModel
import com.dark.darknama.ui.theme.ThemeSettings
import com.dark.darknama.ui.tv.LiveTvViewModel
import com.dark.darknama.utils.DeviceUtils

/** A back-stack entry is a stable focus key even when two details have the same route pattern. */
@Composable
private fun TvDestination(entry: NavBackStackEntry, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTvFocusEntry provides entry.id, content = content)
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    onThemeSettingsChanged: (ThemeSettings) -> Unit = {},
    onFontSettingsChanged: (FontSettings) -> Unit = {}
) {
    val context = LocalContext.current
    // ViewModels are shared across destinations so filtering and server failover survive Back.
    val moviesViewModel = viewModel<MoviesViewModel>()
    val seriesViewModel = viewModel<SeriesViewModel>()
    val searchViewModel = viewModel<SearchViewModel>()
    val countryViewModel = viewModel<CountryViewModel>()
    val liveTvViewModel = viewModel<LiveTvViewModel>()

    NavHost(navController = navController, startDestination = AppScreens.Splash.route) {
        composable(AppScreens.Splash.route) { entry -> TvDestination(entry) {
            SplashScreen(onTimeout = {
                if (DeviceUtils.isTv(context)) {
                    // A TV remote must never go Back into the splash screen.
                    navController.navigate(AppScreens.Movies.route) {
                        popUpTo(AppScreens.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.popBackStack()
                    navController.navigate(AppScreens.Movies.route) { launchSingleTop = true }
                }
            }, backgroundColor = MaterialTheme.colorScheme.background)
        } }
        composable(AppScreens.Movies.route) { entry -> TvDestination(entry) {
            MoviesScreen(viewModel = moviesViewModel, navController = navController)
        } }
        composable(AppScreens.Series.route) { entry -> TvDestination(entry) {
            SeriesScreen(viewModel = seriesViewModel, navController = navController)
        } }
        composable(AppScreens.Search.route) { entry -> TvDestination(entry) {
            SearchScreen(viewModel = searchViewModel, navController = navController)
        } }
        composable(AppScreens.LiveTv.route) { entry -> TvDestination(entry) {
            LiveTvScreen(viewModel = liveTvViewModel, navController = navController)
        } }
        composable(AppScreens.Settings.route) { entry -> TvDestination(entry) {
            SettingsScreen(onThemeSettingsChanged, onFontSettingsChanged, navController)
        } }
        composable(AppScreens.Favorites.route) { entry -> TvDestination(entry) {
            FavoritesScreen(navController)
        } }
        composable(AppScreens.About.route) { entry -> TvDestination(entry) {
            AboutScreen(navController)
        } }
        composable(AppScreens.ThemeSettings.route) { entry -> TvDestination(entry) {
            ThemeSettingsScreen(navController, onThemeSettingsChanged)
        } }
        composable(AppScreens.PlayerSettings.route) { entry -> TvDestination(entry) {
            PlayerSettingsScreen(navController)
        } }
        composable(AppScreens.ServerSettings.route) { entry -> TvDestination(entry) {
            ServerSettingsScreen(navController)
        } }
        composable(AppScreens.FontSettings.route) { entry -> TvDestination(entry) {
            FontSettingsScreen(navController, onFontSettingsChanged)
        } }
        composable(AppScreens.SingleMovie.route,
            arguments = listOf(navArgument("movieId") { defaultValue = "0" })) { entry ->
            TvDestination(entry) {
                SingleMovieScreen(entry.arguments?.getString("movieId")?.toIntOrNull() ?: 0, navController)
            }
        }
        composable(AppScreens.SingleSeries.route,
            arguments = listOf(navArgument("seriesId") { defaultValue = "0" })) { entry ->
            TvDestination(entry) {
                SingleSeriesScreen(entry.arguments?.getString("seriesId")?.toIntOrNull() ?: 0, navController)
            }
        }
        composable(AppScreens.Country.route,
            arguments = listOf(navArgument("countryId") { defaultValue = "0" })) { entry ->
            TvDestination(entry) {
                CountryScreen(entry.arguments?.getString("countryId")?.toIntOrNull() ?: 0,
                    viewModel = countryViewModel, navController = navController)
            }
        }
    }
}
