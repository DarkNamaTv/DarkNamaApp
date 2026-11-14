package com.dark.darknama.utils

import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import java.util.Locale

/** The catalogue's Source.type can identify a stream whose URL has no file extension. */
internal object PlaybackSupport {
    const val MAX_AUTO_RETRIES = 2

    fun mimeType(sourceType: String?): String? = when (
        sourceType?.trim()?.substringBefore(';')?.lowercase(Locale.ROOT)
    ) {
        "hls", "m3u8", "application/x-mpegurl", "application/vnd.apple.mpegurl" ->
            MimeTypes.APPLICATION_M3U8
        "dash", "mpd", "application/dash+xml" -> MimeTypes.APPLICATION_MPD
        else -> null // Let Media3 infer MP4, MKV, and other progressive formats from the URL.
    }

    /** Only transient network/server errors are worth retrying automatically. */
    fun shouldAutoRetry(errorCode: Int, httpStatus: Int?): Boolean = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_TIMEOUT -> true
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            httpStatus == 408 || httpStatus == 429 || (httpStatus != null && httpStatus >= 500)
        else -> false // Not-found, denied, cleartext, decoding/format errors need user action.
    }
}
