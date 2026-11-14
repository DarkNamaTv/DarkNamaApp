package com.dark.darknama.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import com.dark.darknama.R

sealed class AppScreens(
    val route: String,
    @StringRes val resourceId: Int,
    val icon: ImageVector? = null,
    val showBottomBar: Boolean = true
) {
    data object Splash : AppScreens(
        route = "splash",
        resourceId = R.string.app_name
    )

    data object Movies : AppScreens(
        route = "movies",
        resourceId = R.string.movies,
        icon = Icons.Default.Movie
    )

    data object Series : AppScreens(
        route = "series",
        resourceId = R.string.series,
        icon = Icons.Default.Tv
    )

    data object Search : AppScreens(
        route = "search",
        resourceId = R.string.search,
        icon = Icons.Default.Search
    )

    data object LiveTv : AppScreens(
        route = "live_tv",
        resourceId = R.string.live_tv,
        icon = Icons.Default.LiveTv
    )

    // Phone: header gear. TV: header gear or the dedicated sidebar item.
    data object Settings : AppScreens(
        route = "settings",
        resourceId = R.string.settings,
        icon = Icons.Default.Settings,
        showBottomBar = true
    )

    data object SingleMovie : AppScreens(
        route = "single_movie/{movieId}",
        resourceId = R.string.movie_details,
        icon = Icons.Default.Movie,
        showBottomBar = false
    )
    
    data object SingleSeries : AppScreens(
        route = "single_series/{seriesId}",
        resourceId = R.string.series_details,
        icon = Icons.Default.Tv,
        showBottomBar = false
    )

    data object Favorites : AppScreens(
        route = "favorites",
        resourceId = R.string.favorites,
        icon = Icons.Default.Favorite,
        showBottomBar = true
    )

    data object About : AppScreens(
        route = "about",
        resourceId = R.string.about,
        icon = Icons.Default.Info,
        showBottomBar = false
    )

    // ---- Settings sub-pages ----
    // Each settings category is a real page (instead of an inline drop-down)
    // so it can be operated comfortably with an Android TV remote.
    data object ThemeSettings : AppScreens(
        route = "settings/theme",
        resourceId = R.string.theme_settings,
        icon = Icons.Default.FormatColorFill,
        showBottomBar = false
    )

    data object PlayerSettings : AppScreens(
        route = "settings/player",
        resourceId = R.string.video_player_settings,
        icon = Icons.Default.Subtitles,
        showBottomBar = false
    )

    data object ServerSettings : AppScreens(
        route = "settings/server",
        resourceId = R.string.server_settings,
        icon = Icons.Default.Dns,
        showBottomBar = false
    )

    data object FontSettings : AppScreens(
        route = "settings/font",
        resourceId = R.string.font_settings,
        icon = Icons.Default.TextFields,
        showBottomBar = false
    )
    
    data object Country : AppScreens(
        route = "country/{countryId}",
        resourceId = R.string.country,
        showBottomBar = false
    )

    companion object {
        // The phone bar stays unchanged; TV can also navigate directly to Settings.
        val screens = listOf(Movies, Series, LiveTv, Search, Favorites)
        val sidebarScreens = screens + Settings

        /** Keep the parent section highlighted while visiting a detail or settings page. */
        fun sectionForRoute(route: String?): AppScreens = when {
            route == Series.route || route?.startsWith("single_series/") == true -> Series
            route == LiveTv.route -> LiveTv
            route == Search.route || route?.startsWith("country/") == true -> Search
            route == Favorites.route -> Favorites
            route == Settings.route || route == About.route || route?.startsWith("settings/") == true -> Settings
            else -> Movies
        }
    }
}