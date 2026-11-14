package com.dark.darknama.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.view.WindowManager

object DeviceUtils {
    /** Some Android TV boxes advertise Leanback but report a non-TV UI mode. */
    fun isTv(context: Context): Boolean {
        val currentMode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)
            ?.currentModeType ?: Configuration.UI_MODE_TYPE_UNDEFINED
        val features = context.packageManager
        return isTvEnvironment(
            context.resources.configuration.uiMode,
            currentMode,
            features.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
            features.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        )
    }

    /** Separate the decision from Android services so TV and phone cases can be unit tested. */
    internal fun isTvEnvironment(
        configurationMode: Int, currentMode: Int, leanback: Boolean, television: Boolean
    ): Boolean =
        (configurationMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION ||
            currentMode == Configuration.UI_MODE_TYPE_TELEVISION || leanback || television

    /** Check if the device is a tablet. Phone/tablet behavior is unchanged. */
    fun isTablet(context: Context): Boolean {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val width = display.width
        val height = display.height
        val density = context.resources.displayMetrics.density
        val dpHeight = height / density
        val dpWidth = width / density
        val smallestWidth = Math.min(dpWidth, dpHeight)
        return smallestWidth >= 600
    }

    /** Use the same TV decision for navigation, grid width, and layout adaptations. */
    fun getGridColumns(context: Context): Int {
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = resources.configuration.screenWidthDp.takeIf { it > 0 }
            ?: (displayMetrics.widthPixels / displayMetrics.density).toInt()
        return compactGridColumns(screenWidthDp, isTv(context))
    }

    /** Phones keep their existing layout; TV uses six compact cards on 1080p and 4K. */
    fun compactGridColumns(widthDp: Int, isTv: Boolean): Int = when {
        isTv -> 6
        widthDp >= 600 -> (widthDp / 140).coerceIn(4, 8)
        else -> 3
    }
}
