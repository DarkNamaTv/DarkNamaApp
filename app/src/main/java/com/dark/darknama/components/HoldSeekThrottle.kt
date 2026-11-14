package com.dark.darknama.components

internal const val HOLD_SEEK_MULTIPLIER = 2
internal const val HOLD_SEEK_INTERVAL_MS = 300L

/** Shared by the activity and focused seek bar; the clock is supplied by the caller. */
internal class HoldSeekThrottle {
    private var lastKey: Int? = null
    private var lastSeekTime = 0L

    fun shouldSeek(key: Int, repeatCount: Int, now: Long): Boolean {
        if (repeatCount == 0 || lastKey != key) {
            lastKey = key
            lastSeekTime = now
            return true
        }
        if (now - lastSeekTime < HOLD_SEEK_INTERVAL_MS) return false
        lastSeekTime = now
        return true
    }

    fun reset(key: Int) {
        if (lastKey == key) lastKey = null
    }
}

internal fun holdSeekStep(stepMs: Long, repeatCount: Int): Long {
    val step = stepMs.coerceAtLeast(0L)
    return if (repeatCount == 0) step else
        if (step > Long.MAX_VALUE / HOLD_SEEK_MULTIPLIER) Long.MAX_VALUE
        else step * HOLD_SEEK_MULTIPLIER
}
