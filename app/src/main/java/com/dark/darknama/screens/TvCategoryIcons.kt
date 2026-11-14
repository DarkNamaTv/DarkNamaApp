package com.dark.darknama.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/** Icons for feed-provided category names; unknown names keep a neutral category icon. */
internal fun tvCategoryIcon(name: String): ImageVector {
    val category = name.trim().lowercase(Locale.ROOT).replace('ي', 'ی').replace('ك', 'ک')
    return when {
        category == "all" || category == "همه" -> Icons.Default.Apps
        category.contains("news") || category.contains("خبر") ||
            category.contains("اخبار") -> Icons.Default.Newspaper
        category.contains("sport") || category.contains("ورزش") -> Icons.Default.SportsSoccer
        category.contains("movie") || category.contains("film") ||
            category.contains("فیلم") || category.contains("سینما") -> Icons.Default.Movie
        category.contains("series") || category.contains("سریال") -> Icons.Default.LiveTv
        category.contains("kid") || category.contains("child") ||
            category.contains("کودک") -> Icons.Default.ChildCare
        category.contains("music") || category.contains("موسیقی") -> Icons.Default.MusicNote
        category.contains("documentary") || category.contains("مستند") -> Icons.Default.MenuBook
        category.contains("education") || category.contains("آموزش") -> Icons.Default.School
        category.contains("entertainment") || category.contains("سرگرم") -> Icons.Default.TheaterComedy
        category.contains("science") || category.contains("علمی") -> Icons.Default.Science
        category.contains("business") || category.contains("اقتصاد") -> Icons.Default.BusinessCenter
        category.contains("religion") || category.contains("مذهب") -> Icons.Default.SelfImprovement
        category.contains("cook") || category.contains("food") ||
            category.contains("آشپزی") -> Icons.Default.Restaurant
        category.contains("travel") || category.contains("گردشگر") -> Icons.Default.Flight
        category.contains("game") || category.contains("بازی") -> Icons.Default.SportsEsports
        category.contains("weather") || category.contains("هواشناس") -> Icons.Default.WbSunny
        category.contains("general") || category.contains("عمومی") -> Icons.Default.Public
        else -> Icons.Default.Category
    }
}
