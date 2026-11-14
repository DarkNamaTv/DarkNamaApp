package com.dark.darknama.data.repository

import android.content.Context

import com.dark.darknama.data.model.Country
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class CountryRepository(context: Context) : BaseRepository(context) {
    private val BASE_URL = "${com.dark.darknama.data.model.ServerSettings.DEFAULT_SERVER_1}/api/country/all"
    
    suspend fun getAllCountries(): List<Country> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/$API_KEY/"
                val jsonData = executeRequest(url) { Request.Builder().url(it).build() }
                parseCountries(jsonData)
            } catch (e: Exception) {
                throw Exception("Error fetching countries: ${e.message}")
            }
        }
    }
    
    private fun parseCountries(jsonData: String): List<Country> {
        val jsonArray = JSONArray(jsonData)
        val countries = mutableListOf<Country>()
        
        for (i in 0 until jsonArray.length()) {
            try {
                val countryObj = jsonArray.getJSONObject(i)
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
