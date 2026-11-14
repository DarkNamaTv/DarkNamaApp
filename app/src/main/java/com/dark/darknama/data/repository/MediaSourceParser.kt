package com.dark.darknama.data.repository

import com.dark.darknama.data.model.Source
import org.json.JSONObject

/** Shared source parser for movie, search, country and season APIs. */
object MediaSourceParser {
    fun parse(obj: JSONObject): Source = Source(
        id = obj.optInt("id", 0),
        quality = obj.optString("quality", "Unknown"),
        type = obj.optString("type", "Unknown"),
        url = obj.optString("url", "")
    )
}
