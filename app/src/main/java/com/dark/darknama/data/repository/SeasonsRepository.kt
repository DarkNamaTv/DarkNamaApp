package com.dark.darknama.data.repository

import android.content.Context

import com.dark.darknama.data.model.Episode
import com.dark.darknama.data.model.Season
import com.dark.darknama.data.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SeasonsRepository(context: Context) : BaseRepository(context) {
    private val BASE_URL = "${com.dark.darknama.data.model.ServerSettings.DEFAULT_SERVER_1}/api/season/by/serie"
    
    suspend fun getSeasons(seriesId: Int): List<Season> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/$seriesId/$API_KEY/"
                
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                
                parseSeasons(jsonData)
            } catch (e: Exception) {
                throw Exception("Error fetching seasons: ${e.message}")
            }
        }
    }
    
    private fun parseSeasons(jsonData: String): List<Season> {
        val seasons = mutableListOf<Season>()
        val jsonArray = JSONArray(jsonData)
        
        for (i in 0 until jsonArray.length()) {
            try {
                val seasonObj = jsonArray.getJSONObject(i)
                val season = parseSeason(seasonObj)
                seasons.add(season)
            } catch (e: Exception) {
                // Skip seasons that fail to parse
                continue
            }
        }
        
        return seasons
    }
    
    private fun parseSeason(seasonObj: JSONObject): Season {
        return Season(
            id = seasonObj.optInt("id", 0),
            title = seasonObj.optString("title", "Unknown Season"),
            episodes = try {
                parseEpisodes(seasonObj.getJSONArray("episodes"))
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
    
    private fun parseEpisodes(episodesArray: JSONArray): List<Episode> {
        val episodes = mutableListOf<Episode>()
        for (i in 0 until episodesArray.length()) {
            try {
                val episodeObj = episodesArray.getJSONObject(i)
                episodes.add(
                    Episode(
                        id = episodeObj.optInt("id", 0),
                        title = episodeObj.optString("title", "Episode ${i + 1}"),
                        description = episodeObj.optString("description", ""),
                        duration = episodeObj.optString("duration", null).takeIf { it != "null" },
                        image = episodeObj.optString("image", ""),
                        sources = try {
                            parseSources(episodeObj.getJSONArray("sources"))
                        } catch (e: Exception) {
                            emptyList()
                        }
                    )
                )
            } catch (e: Exception) {
                // Skip episodes that fail to parse
                continue
            }
        }
        return episodes
    }
    
    private fun parseSources(sourcesArray: JSONArray): List<Source> {
        val sources = mutableListOf<Source>()
        for (i in 0 until sourcesArray.length()) {
            try {
                val sourceObj = sourcesArray.getJSONObject(i)
                sources.add(MediaSourceParser.parse(sourceObj))
            } catch (e: Exception) {
                // Skip sources that fail to parse
                continue
            }
        }
        return sources
    }
}
