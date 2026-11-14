package com.dark.darknama.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.dark.darknama.R
import com.dark.darknama.data.model.AppFont
import com.dark.darknama.data.model.EnglishFontType
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.utils.StorageUtils

/**
 * One resource/weight/fallback pipeline for app typography, previews and player subtitles.
 * The app includes only Vazirmatn. Optional English TTF names in res/font:
 * inter_regular.ttf, inter_medium.ttf, inter_semibold.ttf, inter_bold.ttf;
 * poppins_regular.ttf, poppins_medium.ttf, poppins_semibold.ttf, poppins_bold.ttf;
 * montserrat_regular.ttf, montserrat_medium.ttf, montserrat_semibold.ttf, montserrat_bold.ttf;
 * nunito_regular.ttf, nunito_medium.ttf, nunito_semibold.ttf, nunito_bold.ttf;
 * lato_regular.ttf, lato_medium.ttf, lato_semibold.ttf, lato_bold.ttf.
 * No fonts are downloaded or synthesized. Only present files may become compiled resources.
 * SIL Open Font License 1.1 attribution (if supplied): Inter — Rasmus Andersson;
 * Poppins — Indian Type Foundry and Jonny Pinhorn; Montserrat — Julieta Ulanovsky
 * and collaborators; Nunito — Vernon Adams and collaborators; Lato — Łukasz Dziedzic.
 * Upstream OFL copyright holders: The Inter Project Authors, The Poppins Project
 * Authors, The Montserrat.Git Project Authors, The Nunito Project Authors, and
 * tyPoland Łukasz Dziedzic, respectively. Bundled Persian Vazirmatn is by
 * Saber Rastikerdar (SIL OFL 1.1). Preserve each supplied font's actual
 * original copyright notice and full OFL text with redistributed builds.
 */
object FontManager {
    // Both enum types share the same lookup/cache; DEFAULT has no files.
    private val resourceIds = mutableMapOf<Any, Map<String, Int>>()
    private val families = mutableMapOf<Any, FontFamily?>()
    private val typefaces = mutableMapOf<Any, Typeface?>()

    private fun ids(context: Context, type: Any): Map<String, Int> = synchronized(resourceIds) {
        resourceIds.getOrPut(type) {
            if (type == FontType.VAZIRMATN) {
                // Bundled: direct references keep these existing files through shrinker passes.
                mapOf(
                    "vazirmatn_light" to R.font.vazirmatn_light,
                    "vazirmatn_regular" to R.font.vazirmatn_regular,
                    "vazirmatn_medium" to R.font.vazirmatn_medium,
                    "vazirmatn_semibold" to R.font.vazirmatn_semibold,
                    "vazirmatn_bold" to R.font.vazirmatn_bold
                )
            } else {
                // Dynamic lookup is safe when optional files do not exist. The
                // resource keep rule preserves them in a release when supplied.
                val names = when (type) {
                    is FontType -> fontResourceNames(type)
                    is EnglishFontType -> fontResourceNames(type)
                    else -> emptyMap()
                }
                names.values.associateWith { name ->
                    context.resources.getIdentifier(name, "font", context.packageName)
                }.filterValues { it != 0 }
            }
        }
    }

    private fun mapped(type: Any, names: Set<String>): Map<Int, String> = when (type) {
        is FontType -> mappedFontWeights(type, names)
        is EnglishFontType -> mappedFontWeights(type, names)
        else -> emptyMap()
    }

    fun isAvailable(context: Context, type: EnglishFontType): Boolean =
        type == EnglishFontType.DEFAULT || ids(context, type).isNotEmpty()

    fun loadFontFamily(context: Context, fontType: FontType): FontFamily? = loadFamily(context, fontType)
    fun loadFontFamily(context: Context, fontType: EnglishFontType): FontFamily? = loadFamily(context, fontType)

    fun loadFontFamily(context: Context, settings: FontSettings, language: String): FontFamily? =
        when (val active = settings.forLanguage(language)) {
            is AppFont.Persian -> loadFontFamily(context, active.type)
            is AppFont.English -> loadFontFamily(context, active.type)
        }

    private fun loadFamily(context: Context, type: Any): FontFamily? {
        if (type == FontType.DEFAULT || type == EnglishFontType.DEFAULT) return null
        return synchronized(families) {
            if (!families.containsKey(type)) {
                val available = ids(context, type)
                val fonts = mapped(type, available.keys).map { (weight, name) ->
                    Font(available.getValue(name), FontWeight(weight))
                }
                families[type] = if (fonts.isEmpty()) null else FontFamily(*fonts.toTypedArray())
            }
            families[type]
        }
    }

    /** Media3 SubtitleView needs an Android Typeface, not a Compose FontFamily. */
    fun loadSubtitleTypeface(context: Context, fontType: FontType): Typeface? =
        loadTypeface(context, fontType)
    fun loadSubtitleTypeface(context: Context, fontType: EnglishFontType): Typeface? =
        loadTypeface(context, fontType)

    fun loadSubtitleTypeface(context: Context, settings: FontSettings, language: String): Typeface? =
        when (val active = settings.forLanguage(language)) {
            is AppFont.Persian -> loadSubtitleTypeface(context, active.type)
            is AppFont.English -> loadSubtitleTypeface(context, active.type)
        }

    private fun loadTypeface(context: Context, type: Any): Typeface? {
        if (type == FontType.DEFAULT || type == EnglishFontType.DEFAULT) return null
        return synchronized(typefaces) {
            if (!typefaces.containsKey(type)) {
                val available = ids(context, type)
                val regular = mapped(type, available.keys)[400]
                typefaces[type] = regular?.let { name ->
                    try {
                        ResourcesCompat.getFont(context, available.getValue(name))
                    } catch (_: Exception) {
                        null // Absent/invalid optional TTF: system glyph fallback.
                    }
                }
            }
            typefaces[type]
        }
    }
}

@Composable
fun rememberFontSettings(): FontSettings {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) { StorageUtils.loadFontSettings(context) }
}
