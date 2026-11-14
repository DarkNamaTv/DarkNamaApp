package com.dark.darknama.data.repository

import com.dark.darknama.data.model.GitHubReleaseResponse
import com.dark.darknama.data.model.UpdateAsset
import com.dark.darknama.data.model.UpdateRelease
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Release metadata and APK bytes stay in the repository, never in a composable. */
class UpdateRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val releaseUrl: String = "https://api.github.com/repos/DarknamaTv/DarkNamaApp/releases/latest"
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun latestRelease(): UpdateRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(releaseUrl)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "DarkNama-App")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Release request failed: HTTP ${response.code}")
            val body = response.body?.string() ?: throw IOException("Empty release response")
            json.decodeFromString<GitHubReleaseResponse>(body).toUpdateRelease()
        }
    }

    /** Writes a private, complete APK and deletes the partial download on failure/cancel. */
    suspend fun download(
        asset: UpdateAsset, directory: File, onProgress: (Int?) -> Unit
    ): File = suspendCancellableCoroutine { continuation ->
        if (!asset.url.startsWith("https://", ignoreCase = true)) {
            continuation.resumeWithException(IOException("APK URL must use HTTPS"))
            return@suspendCancellableCoroutine
        }
        val apk = File(directory, "DarkNama.apk")
        val partial = File(directory, "DarkNama.apk.part")
        val request = Request.Builder().url(asset.url).header("User-Agent", "DarkNama-App").build()
        val call = client.newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
            partial.delete()
            apk.delete()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                partial.delete()
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!directory.isDirectory && !directory.mkdirs())
                        throw IOException("Cannot create update directory")
                    // A previous version must never be installed after a failed retry.
                    apk.delete()
                    partial.delete()
                    response.use { result ->
                        if (!result.isSuccessful) throw IOException("APK download failed: HTTP ${result.code}")
                        val body = result.body ?: throw IOException("Empty APK response")
                        if (body.contentType()?.type == "text") throw IOException("APK response was not a file")
                        val total = body.contentLength().takeIf { it > 0 }
                            ?: asset.size.takeIf { it > 0 }
                        var received = 0L
                        var lastPercent: Int? = null
                        body.byteStream().use { input ->
                            partial.outputStream().buffered().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    if (!continuation.isActive) throw IOException("Download canceled")
                                    val count = input.read(buffer)
                                    if (count == -1) break
                                    output.write(buffer, 0, count)
                                    received += count
                                    val percent = total?.let { (received * 100 / it).toInt().coerceIn(0, 100) }
                                    if (percent != lastPercent) {
                                        onProgress(percent)
                                        lastPercent = percent
                                    }
                                }
                            }
                        }
                        if (received == 0L || (total != null && received < total))
                            throw IOException("Incomplete APK download")
                    }
                    if (!continuation.isActive) throw IOException("Download canceled")
                    if (!partial.renameTo(apk)) throw IOException("Cannot finish APK download")
                    if (continuation.isActive) continuation.resume(apk) else apk.delete()
                } catch (e: Exception) {
                    partial.delete()
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        })
    }
}
