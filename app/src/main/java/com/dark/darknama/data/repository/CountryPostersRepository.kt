package com.dark.darknama.data.repository

import android.content.Context

import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Poster
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class CountryPostersRepository(context: Context) : BaseRepository(context) {
    private val BASE_URL = "${com.dark.darknama.data.model.ServerSettings.DEFAULT_SERVER_1}/api/poster/by/filtres"
    
    suspend fun getPostersByCountry(countryId: Int, page: Int = 0,
                                    filterType: FilterType = FilterType.DEFAULT): List<Poster> =
        getPosterBatch(countryId, page, filterType).items

    suspend fun getPosterBatch(countryId: Int, page: Int = 0,
                               filterType: FilterType = FilterType.DEFAULT,
                               genreId: Int = 0): FilteredPage<Poster> =
        withContext(Dispatchers.IO) {
            try {
                fetchFilteredPages(
                    startPage = page,
                    fetchPage = { nextPage ->
                        val url = buildUrl(countryId, filterType, nextPage, genreId)
                        parsePosters(executeRequest(url) { Request.Builder().url(it).build() })
                    },
                    isExcluded = { isPromotional(it) },
                    onExcluded = { logSkippedPromotionalTitle(it.title) }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                throw IOException("Error fetching posters for country $countryId", e)
            }
        }
    
    private fun buildUrl(countryId: Int, filterType: FilterType, page: Int, genreId: Int): String {
        return when (filterType) {
            FilterType.DEFAULT -> "$BASE_URL/$genreId/$countryId/created/$page/$API_KEY"
            FilterType.BY_YEAR -> "$BASE_URL/$genreId/$countryId/year/$page/$API_KEY"
            FilterType.BY_IMDB -> "$BASE_URL/$genreId/$countryId/imdb/$page/$API_KEY"
        }
    }
    
    private fun parsePosters(jsonData: String): FeedPage<Poster> {
        val posters = mutableListOf<Poster>()
        val jsonArray = JSONArray(jsonData)

        for (i in 0 until jsonArray.length()) {
            try {
                posters.add(parsePoster(jsonArray.getJSONObject(i)))
            } catch (_: Exception) {
                // An invalid entry does not mark the end of the raw API feed.
            }
        }
        return FeedPage(posters, hasMore = jsonArray.length() > 0)
    }
    
    private fun parsePoster(posterObj: JSONObject): Poster {
        return Poster(
            id = posterObj.optInt("id", 0),
            title = posterObj.optString("title", "Unknown Title"),
            type = posterObj.optString("type", ""),
            description = posterObj.optString("description", "No description available"),
            year = posterObj.optInt("year", 0),
            imdb = posterObj.optDouble("imdb", 0.0),
            rating = posterObj.optDouble("rating", 0.0),
            duration = posterObj.optString("duration", null).takeIf { it != "null" && it != "N/A" },
            image = posterObj.optString("image", ""),
            cover = posterObj.optString("cover", ""),
            genres = try {
                val genresArray = posterObj.getJSONArray("genres")
                val genres = mutableListOf<com.dark.darknama.data.model.Genre>()
                for (j in 0 until genresArray.length()) {
                    val genreObj = genresArray.getJSONObject(j)
                    genres.add(
                        com.dark.darknama.data.model.Genre(
                            id = genreObj.optInt("id", 0),
                            title = genreObj.optString("title", "Unknown")
                        )
                    )
                }
                genres
            } catch (e: Exception) {
                emptyList()
            },
            sources = try {
                val sourcesArray = posterObj.getJSONArray("sources")
                val sources = mutableListOf<com.dark.darknama.data.model.Source>()
                for (j in 0 until sourcesArray.length()) {
                    val sourceObj = sourcesArray.getJSONObject(j)
                    sources.add(MediaSourceParser.parse(sourceObj))
                }
                sources
            } catch (e: Exception) {
                emptyList()
            },
            country = try {
                val countriesArray = posterObj.getJSONArray("country")
                val countries = mutableListOf<com.dark.darknama.data.model.Country>()
                for (j in 0 until countriesArray.length()) {
                    val countryObj = countriesArray.getJSONObject(j)
                    countries.add(
                        com.dark.darknama.data.model.Country(
                            id = countryObj.optInt("id", 0),
                            title = countryObj.optString("title", "Unknown"),
                            image = countryObj.optString("image", "")
                        )
                    )
                }
                countries
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
}
