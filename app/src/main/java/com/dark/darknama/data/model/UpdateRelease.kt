package com.dark.darknama.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0L
)

@Serializable
data class GitHubReleaseResponse(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String? = null,
    val assets: List<UpdateAsset> = emptyList()
) {
    /** Prefer the canonical APK; older releases may only have a versioned APK name. */
    fun preferredApk(): UpdateAsset? {
        val apks = assets.filter { it.name.endsWith(".apk", ignoreCase = true) &&
            it.url.startsWith("https://", ignoreCase = true) }
        return apks.firstOrNull { it.name.equals("DarkNama.apk", ignoreCase = true) }
            ?: apks.firstOrNull()
    }

    fun toUpdateRelease(): UpdateRelease = UpdateRelease(
        version = tagName,
        title = name.ifBlank { tagName },
        notes = body.orEmpty(),
        asset = preferredApk()
    )
}

data class UpdateRelease(
    val version: String,
    val title: String,
    val notes: String,
    val asset: UpdateAsset?
)
