package com.dark.darknama.data.repository

import android.content.Context

import com.dark.darknama.data.model.Country
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Series
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SeriesRepository(context: Context) : BaseRepository(context) {
    private val BASE_URL = "${com.dark.darknama.data.model.ServerSettings.DEFAULT_SERVER_1}/api/serie/by/filtres"
    
    suspend fun getSeries(page: Int = 0, genreId: Int = 0, filterType: FilterType = FilterType.DEFAULT): List<Series> {
        return withContext(Dispatchers.IO) {
            try {
                val url = buildUrl(BASE_URL, genreId, filterType, page)
                
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                
                parseSeries(jsonData)
            } catch (e: Exception) {
                throw Exception("Error fetching series: ${e.message}")
            }
        }
    }
    
    private fun buildUrl(baseUrl: String, genreId: Int, filterType: FilterType, page: Int): String {
        return when (filterType) {
            FilterType.DEFAULT -> "$baseUrl/$genreId/created/$page/$API_KEY"
            FilterType.BY_YEAR -> "$baseUrl/$genreId/year/$page/$API_KEY"
            FilterType.BY_IMDB -> "$baseUrl/$genreId/imdb/$page/$API_KEY"
        }
    }
    
    private fun parseSeries(jsonData: String): List<Series> {
        val seriesList = mutableListOf<Series>()
        val jsonArray = JSONArray(jsonData)
        
        for (i in 0 until jsonArray.length()) {
            try {
                val seriesObj = jsonArray.getJSONObject(i)
                val series = parseSeriesItem(seriesObj)
                seriesList.add(series)
            } catch (e: Exception) {
                // Skip items that fail to parse
                continue
            }
        }
        
        return seriesList
    }
    
    private fun parseSeriesItem(seriesObj: JSONObject): Series {
        return Series(
            id = seriesObj.optInt("id", 0),
            type = seriesObj.optString("type", ""),
            title = seriesObj.optString("title", "Unknown Title"),
            description = seriesObj.optString("description", "No description available"),
            year = seriesObj.optInt("year", 0),
            imdb = seriesObj.optDouble("imdb", 0.0),
            rating = seriesObj.optDouble("rating", 0.0),
            duration = seriesObj.optString("duration", null).takeIf { it != "null" && it != "N/A" },
            image = seriesObj.optString("image", ""),
            cover = seriesObj.optString("cover", ""),
            genres = try {
                parseGenres(seriesObj.getJSONArray("genres"))
            } catch (e: Exception) {
                emptyList()
            },
            country = try {
                parseCountries(seriesObj.getJSONArray("country"))
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
    
    private fun parseGenres(genresArray: JSONArray): List<Genre> {
        val genres = mutableListOf<Genre>()
        for (i in 0 until genresArray.length()) {
            try {
                val genreObj = genresArray.getJSONObject(i)
                genres.add(
                    Genre(
                        id = genreObj.optInt("id", 0),
                        title = genreObj.optString("title", "Unknown")
                    )
                )
            } catch (e: Exception) {
                // Skip genres that fail to parse
                continue
            }
        }
        return genres
    }
    
    private fun parseCountries(countriesArray: JSONArray): List<Country> {
        val countries = mutableListOf<Country>()
        for (i in 0 until countriesArray.length()) {
            try {
                val countryObj = countriesArray.getJSONObject(i)
                countries.add(
                    Country(
                        id = countryObj.optInt("id", 0),
                        title = countryObj.optString("title", "Unknown"),
                        image = countryObj.optString("image", "")
                    )
                )
            } catch (e: Exception) {
                // Skip countries that fail to parse
                continue
            }
        }
        return countries
    }
}
