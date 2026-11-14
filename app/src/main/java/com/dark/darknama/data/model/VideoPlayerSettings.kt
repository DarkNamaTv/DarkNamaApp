package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoPlayerSettings(
    val seekTimeSeconds: Int = 10,
    val progressColor: Int = DEFAULT_PROGRESS_COLOR
) {
    companion object {
        // Older saved settings without a bar color keep the original default.
        const val DEFAULT_PROGRESS_COLOR: Int = -0x99AF5C // #FF6650A4
        val DEFAULT = VideoPlayerSettings()
    }
}
