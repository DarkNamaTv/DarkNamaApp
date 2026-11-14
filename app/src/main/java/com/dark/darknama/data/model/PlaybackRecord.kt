package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

/** A stable catalogue identity; changing quality must not create another history entry. */
data class PlaybackKey(
    val type: String,
    val contentId: Int,
    val seasonId: Int? = null,
    val episodeId: Int? = null
)

/** Live TV has no catalogue identity and is intentionally excluded from playback history. */
@Serializable
data class PlaybackRecord(
    val type: String, // "movie" or "series"
    val contentId: Int,
    val title: String,
    val image: String,
    val url: String,
    val quality: String,
    val sourceType: String?,
    val seasonId: Int? = null,
    val seasonTitle: String? = null,
    val episodeId: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
    val ended: Boolean = false
) {
    val key: PlaybackKey get() = PlaybackKey(type, contentId, seasonId, episodeId)
    val isFinished: Boolean get() = PlaybackProgress.isFinished(positionMs, durationMs, ended)
    val progress: Float get() = if (durationMs > 0L)
        (positionMs.toDouble() / durationMs).toFloat().coerceIn(0f, 1f) else 0f
    val remainingMs: Long get() = (durationMs - positionMs.coerceAtLeast(0L)).coerceAtLeast(0L)
}

/** A completed replay becomes unfinished again as soon as it is stopped before the end. */
object PlaybackProgress {
    fun isFinished(positionMs: Long, durationMs: Long, ended: Boolean = false): Boolean {
        if (ended) return true // Also handles streams whose duration was unknown at the end.
        if (positionMs <= 0L || durationMs <= 0L) return false
        // For videos under a minute, the 60-second rule would mark the start as finished.
        return positionMs >= durationMs - durationMs / 20L ||
            (durationMs > 60_000L && durationMs - positionMs <= 60_000L)
    }
}
