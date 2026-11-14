package com.dark.darknama.ui.theme

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.utils.DeviceUtils
import com.dark.darknama.utils.LanguageUtils

// Set of Material typography styles to start with
fun appTypography(fontFamily: FontFamily?): androidx.compose.material3.Typography {
    return androidx.compose.material3.Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily ?: FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/** Black/white contrast is equal at luminance ~0.179; choose the stronger side. */
fun contrastingOn(color: Color): Color =
    if (color.luminance() > 0.179f) Color.Black else Color.White

/** Contrast against what is actually visible after a translucent selection fill
 * and themed surface are composited (not against the accent in isolation). */
fun contrastingOver(fill: Color, surface: Color, background: Color): Color =
    contrastingOn(fill.compositeOver(surface.compositeOver(background)))

fun contrastRatio(foreground: Color, background: Color): Float {
    val light = maxOf(foreground.luminance(), background.luminance())
    val dark = minOf(foreground.luminance(), background.luminance())
    return (light + 0.05f) / (dark + 0.05f)
}

/** Keep the saved hue but make primary *text* legible on every app surface. */
fun readableAccent(selected: Color, dark: Boolean, surfaces: List<Color>): Color {
    val target = if (dark) Color.White else Color.Black
    for (step in 0..100) {
        val candidate = lerp(selected, target, step / 100f)
        if (surfaces.all { contrastRatio(candidate, it) >= 4.5f }) return candidate
    }
    return target
}

@Composable
fun DarkNamaTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    fontSettings: FontSettings = FontSettings(),
    // Keep the player video unoccluded; it only needs the scheme and visual-style local.
    showStyleBackdrop: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Neon uses a dark base regardless of the light/system mode; the other
    // styles retain the user's chosen light/dark preference.
    val darkTheme = themeSettings.style == VisualStyle.NEON_GLASS || when (themeSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Recreate on locale toggle; no sizes/line heights/letter spacing change.
    val fontFamily = FontManager.loadFontFamily(context, fontSettings,
        LanguageUtils.getSavedLanguage(context))
    
    // The colour scheme is always derived from the user's selected primary
    // colour (including the first, default swatch).
    //
    // NOTE: previously, picking the default colour fell through to Android 12+
    // dynamic (wallpaper-based) colours, which made the "Default theme" option
    // look random / broken. The dynamic branch has been removed so the default
    // theme is now deterministic and matches the app's brand colour.
    val style = themeSettings.style
    val background = if (darkTheme) when (style) {
        VisualStyle.DEFAULT -> Color(0xFF121212)
        VisualStyle.GLASS -> Color(0xFF101624)
        VisualStyle.NEON_GLASS -> Color(0xFF060919)
    } else if (style == VisualStyle.GLASS) Color(0xFFE5EEFA) else Color(0xFFFFFBFE)
    val surface = if (darkTheme) when (style) {
        VisualStyle.DEFAULT -> Color(0xFF1E1E1E)
        VisualStyle.GLASS -> Color(0xCC273249)
        VisualStyle.NEON_GLASS -> Color(0xBF142039)
    } else if (style == VisualStyle.GLASS) Color(0xDCF7FAFF) else Color(0xFFFFFBFE)
    val surfaceVariant = if (darkTheme) when (style) {
        VisualStyle.DEFAULT -> Color(0xFF2D2D2D)
        VisualStyle.GLASS -> Color(0xD4384560)
        VisualStyle.NEON_GLASS -> Color(0xD41C2A49)
    } else if (style == VisualStyle.GLASS) Color(0xD3DAE7F5) else Color(0xFFE7E0EC)
    // Persist the actual swatch; derive a legible shade only for rendering text and controls.
    val primary = readableAccent(themeSettings.primaryColor, darkTheme,
        listOf(background, surface, surfaceVariant))
    val secondary = if (style == VisualStyle.DEFAULT) primary else readableAccent(
        themeSettings.secondaryColor, darkTheme, listOf(background, surface, surfaceVariant))
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = contrastingOn(primary),
            primaryContainer = primary.copy(alpha = 0.30f),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = contrastingOn(secondary),
            tertiary = Pink80,
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onTertiary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFE0E5F2)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = contrastingOn(primary),
            primaryContainer = primary.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFF1C1B1F),
            secondary = secondary,
            onSecondary = contrastingOn(secondary),
            tertiary = Pink40,
            background = background,
            surface = surface,
            surfaceVariant = surfaceVariant,
            onTertiary = Color.White,
            onBackground = Color(0xFF1C1B1F),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = if (style == VisualStyle.GLASS) Color(0xFF3E475A) else Color(0xFF49454F)
        )
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val barColor = if (style == VisualStyle.DEFAULT) colorScheme.primary else colorScheme.background
            window.statusBarColor = barColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                barColor.luminance() > 0.5f
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = appTypography(fontFamily)) {
        // Both setContent activities use this theme. Supply the background's content
        // color without drawing a new layer over the glass backdrop or player video.
        CompositionLocalProvider(
            LocalVisualStyle provides style,
            LocalContentColor provides colorScheme.onBackground
        ) {
            if (style == VisualStyle.DEFAULT || !showStyleBackdrop) content()
            else StyleBackdrop(style, content)
        }
    }
}

/** A single blurred accent layer, never a per-card backdrop blur. No blur on TV/low-RAM/API <31. */
@Composable
private fun StyleBackdrop(style: VisualStyle, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !lowRam && !DeviceUtils.isTv(context)
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.linearGradient(listOf(colors.background,
            lerp(colors.background, colors.primary, if (style == VisualStyle.NEON_GLASS) 0.17f else 0.10f),
            lerp(colors.background, colors.secondary, 0.12f), colors.background))
    )) {
        if (canBlur) {
            val glow = if (style == VisualStyle.NEON_GLASS) {
                val transition = rememberInfiniteTransition(label = "neonBackdrop")
                val alpha by transition.animateFloat(0.12f, 0.26f,
                    animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
                    label = "neonGlow")
                alpha
            } else 0.12f
            Box(Modifier.matchParentSize().blur(28.dp).background(
                Brush.radialGradient(listOf(colors.primary.copy(alpha = glow),
                    colors.secondary.copy(alpha = glow / 2), Color.Transparent),
                    center = Offset(250f, 400f), radius = 900f)
            ))
        }
        content()
    }
}
