package com.dark.darknama.data.repository

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.dark.darknama.data.model.ServerSettings
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object CatalogueApiCall

internal object ApiConfig {
    const val KEY = "4F5A9C3D9A86FA54EACEDDD635185"
}

/**
 * Replays only tagged, idempotent catalogue API requests. A candidate must answer the
 * lightweight genre endpoint AND the original request before it becomes the active server.
 * No media/IPTV requests are routed through this interceptor.
 */
class ApiFailoverInterceptor(
    private val selection: ServerSelection,
    private val onSwitched: (Int) -> Unit = {},
    private val healthPath: String = "/api/genre/all/${ApiConfig.KEY}",
    private val serverBaseUrl: (ServerSettings, Int) -> String = { settings, number -> settings.urlFor(number) }
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.tag(CatalogueApiCall::class.java) == null) return chain.proceed(original)

        val initial = selection.snapshot()
        val start = initial.currentServer
        val canRetry = initial.automatic && original.method == "GET"
        val order = if (canRetry) listOf(start, start % 3 + 1, (start + 1) % 3 + 1)
                    else listOf(start)
        val visited = mutableSetOf<String>()
        var lastFailure: IOException? = null

        for (number in order) {
            // Do not continue an in-flight failover if the user has turned Auto off.
            if (number != start && !selection.snapshot().automatic) break
            val base = serverBaseUrl(initial, number).toHttpUrl()
            if (!visited.add(base.toString())) continue
            if (number != start && !isHealthy(chain, base)) continue

            val url = base.newBuilder()
                .encodedPath(original.url.encodedPath)
                .encodedQuery(original.url.encodedQuery)
                .fragment(null)
                .build()
            val request = original.newBuilder().url(url).build()
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful) {
                    // Fully read the JSON before promoting a candidate. Timeouts/truncated bodies
                    // during consumption must also trigger failover, not just connection errors.
                    val contentType = response.body?.contentType()
                    val data = try {
                        response.body?.bytes() ?: byteArrayOf()
                    } catch (error: IOException) {
                        response.close()
                        throw error
                    }
                    response.close()
                    if (data.any { byte ->
                            when (byte.toInt()) {
                                9, 10, 13, 32 -> false // JSON whitespace
                                else -> true
                            }
                        }) {
                        if (number != start && selection.promoteIfAuto(start, number)) {
                            onSwitched(number)
                        }
                        return response.newBuilder().body(data.toResponseBody(contentType)).build()
                    }
                    lastFailure = IOException("Empty catalogue API response")
                    if (!canRetry) throw IOException("Empty catalogue API response")
                } else {
                    // A 404/401 on the current server is a request error, not an outage.
                    if (!canRetry || (number == start && !retryable(response.code))) return response
                    lastFailure = IOException("Catalogue API returned HTTP ${response.code}")
                    response.close()
                }
            } catch (error: IOException) {
                lastFailure = error
                if (!canRetry) throw error
            }
        }
        throw lastFailure ?: IOException("No healthy catalogue server is available")
    }

    private fun retryable(code: Int): Boolean = code == 408 || code == 429 || code >= 500

    private fun isHealthy(chain: Interceptor.Chain, base: HttpUrl): Boolean {
        val healthUrl = base.newBuilder().encodedPath(healthPath).build()
        val probe = Request.Builder().url(healthUrl).get().build()
        return try {
            // Use the same call, but give the probe a shorter per-attempt timeout.
            chain.withConnectTimeout(2, TimeUnit.SECONDS)
                .withReadTimeout(3, TimeUnit.SECONDS)
                .proceed(probe).use { response ->
                    response.isSuccessful && response.peekBody(64).string().trimStart().startsWith("[")
                }
        } catch (_: IOException) {
            false
        }
    }
}
