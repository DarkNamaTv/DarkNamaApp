package com.dark.darknama.utils

/** True when a title contains Arabic/Persian *letters*, including presentation forms.
 * Digits and punctuation in these blocks are not letters and do not make an ad. */
fun String?.containsPersian(): Boolean = this?.any { char ->
    char.isLetter() && (char in '\u0600'..'\u06FF' ||
        char in '\u0750'..'\u077F' ||
        char in '\uFB50'..'\uFDFF' ||
        char in '\uFE70'..'\uFEFF')
} ?: false
