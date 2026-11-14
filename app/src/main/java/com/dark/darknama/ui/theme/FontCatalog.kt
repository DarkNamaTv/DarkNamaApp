package com.dark.darknama.ui.theme

import com.dark.darknama.data.model.EnglishFontType
import com.dark.darknama.data.model.FontType
import java.util.Locale
import kotlin.math.abs

/** Resource names, not R.font IDs: optional TTFs are only compiled when present. */
internal fun fontResourceNames(type: FontType): Map<Int, String> = when (type) {
    FontType.DEFAULT -> emptyMap()
    FontType.VAZIRMATN -> mapOf(
        300 to "vazirmatn_light", 400 to "vazirmatn_regular",
        500 to "vazirmatn_medium", 600 to "vazirmatn_semibold",
        700 to "vazirmatn_bold"
    )
}

/**
 * Optional Latin families. These exact resource names correspond to
 * <name>.ttf in res/font. No optional English TTF ships in this checkout.
 */
internal fun fontResourceNames(type: EnglishFontType): Map<Int, String> =
    if (type == EnglishFontType.DEFAULT) emptyMap()
    else {
        val prefix = type.name.lowercase(Locale.ROOT)
        mapOf(
            400 to "${prefix}_regular", 500 to "${prefix}_medium",
            600 to "${prefix}_semibold", 700 to "${prefix}_bold"
        )
    }

/** A partial set uses the nearest existing file (lower weight on a tie). */
private fun closestFontWeights(names: Map<Int, String>, available: Set<String>): Map<Int, String> {
    val present = names.filterValues { it in available }
    if (present.isEmpty()) return emptyMap()
    return listOf(300, 400, 500, 600, 700).associateWith { weight ->
        present.entries.minWith(compareBy<Map.Entry<Int, String>>(
            { abs(it.key - weight) }, { if (it.key > weight) 1 else 0 }, { it.key }
        )).value
    }
}

internal fun mappedFontWeights(type: FontType, available: Set<String>): Map<Int, String> =
    closestFontWeights(fontResourceNames(type), available)

internal fun mappedFontWeights(type: EnglishFontType, available: Set<String>): Map<Int, String> =
    closestFontWeights(fontResourceNames(type), available)
