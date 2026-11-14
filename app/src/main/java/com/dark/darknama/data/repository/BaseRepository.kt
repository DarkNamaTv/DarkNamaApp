package com.dark.darknama.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

open class BaseRepository(context: Context) {
    // Timeouts are per attempt; callTimeout caps the complete failover (including probes).
    protected val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(ApiFailoverInterceptor(ServerSettingsRepository.get(context), ServerSwitchEvents::announce))
        .build()

    protected val API_KEY = ApiConfig.KEY

    protected suspend fun executeRequest(
        primaryUrl: String,
        requestBuilder: (String) -> Request
    ): String = withContext(Dispatchers.IO) {
        val request = requestBuilder(primaryUrl).newBuilder()
            .tag(CatalogueApiCall::class.java, CatalogueApiCall)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Catalogue API returned HTTP ${response.code}")
            response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Empty catalogue API response")
        }
    }
}
