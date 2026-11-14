package com.dark.darknama.data.model

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Only the catalogue API uses these servers; media URLs and IPTV have their own hosts. */
data class ServerSettings(
    val automatic: Boolean = true,
    val currentServer: Int = 1,
    val server2Url: String = DEFAULT_SERVER_2,
    val server3Url: String = DEFAULT_SERVER_3
) {
    fun urlFor(number: Int): String = when (number) {
        1 -> DEFAULT_SERVER_1
        2 -> server2Url
        3 -> server3Url
        else -> throw IllegalArgumentException("Unknown server number")
    }

    companion object {
        const val DEFAULT_SERVER_1 = "https://server-hi-speed-iran.info"
        const val DEFAULT_SERVER_2 = "http://server-majazi-iran.info"
        const val DEFAULT_SERVER_3 = "http://server-win-iran.info"

        /**
         * A server is an origin, not an arbitrary URL/path. Android permits cleartext only
         * for the two built-in HTTP hosts; user-supplied hosts must use HTTPS.
         */
        fun normalizeEditableUrl(input: String): String? {
            val url = input.trim().toHttpUrlOrNull() ?: return null
            if (url.username.isNotEmpty() || url.password.isNotEmpty() ||
                url.encodedPath != "/" || url.encodedQuery != null || url.fragment != null
            ) return null
            if (url.scheme != "https" &&
                !(url.scheme == "http" && url.host in setOf(
                    "server-majazi-iran.info", "server-win-iran.info"
                ))
            ) return null
            return url.toString().removeSuffix("/")
        }
    }
}
