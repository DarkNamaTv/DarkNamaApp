package com.dark.darknama.utils

import android.content.Context
import android.util.Log
import com.dark.darknama.data.model.FavoriteChannel
import com.dark.darknama.data.model.FavoriteGroup
import com.dark.darknama.data.model.FavoriteItem
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.PlaybackKey
import com.dark.darknama.data.model.PlaybackRecord
import com.dark.darknama.data.model.Series
import com.dark.darknama.data.model.SeriesLinkSelection
import com.dark.darknama.data.model.toSeries
import com.dark.darknama.data.model.SubtitleSettings
import com.dark.darknama.data.model.VideoPlayerSettings
import com.dark.darknama.data.model.FontSettings
import com.dark.darknama.data.model.FontType
import com.dark.darknama.data.model.WatchedEpisode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.Date

object StorageUtils {
    private const val TAG = "StorageUtils"
    // Older saved media and player preferences may contain fields retired by newer builds.
    internal val savedJson = Json { ignoreUnknownKeys = true }
    
    // Favorites functions - using a single JSON file for all favorites
    fun saveFavorite(context: Context, favorite: FavoriteItem) {
        try {
            val favorites = loadAllFavorites(context).toMutableList()
            
            // Remove existing favorite with same id and type if it exists
            favorites.removeAll { it.id == favorite.id && it.type == favorite.type }
            
            // Add the new favorite at the beginning of the list (newest first)
            favorites.add(0, favorite)
            
            // Save all favorites to a single file
            val jsonString = Json.encodeToString(favorites)
            val file = File(context.filesDir, "favorites.json")
            file.writeText(jsonString)
            Log.d(TAG, "Favorite saved: ${favorite.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite", e)
        }
    }
    
    fun removeFavorite(context: Context, id: Int, type: String) {
        try {
            val favorites = loadAllFavorites(context).toMutableList()
            
            // Remove the favorite with matching id and type
            favorites.removeAll { it.id == id && it.type == type }
            
            // Save updated favorites list
            val jsonString = Json.encodeToString(favorites)
            val file = File(context.filesDir, "favorites.json")
            file.writeText(jsonString)
            Log.d(TAG, "Favorite removed: $id ($type)")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing favorite", e)
        }
    }
    
    fun clearAllFavorites(context: Context) {
        try {
            val file = File(context.filesDir, "favorites.json")
            if (file.exists()) {
                file.delete()
            }
            Log.d(TAG, "All favorites cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all favorites", e)
        }
    }
    
    fun isFavorite(context: Context, id: Int, type: String): Boolean {
        return try {
            val favorites = loadAllFavorites(context)
            favorites.any { it.id == id && it.type == type }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if favorite exists", e)
            false
        }
    }
    
    fun loadAllFavorites(context: Context): List<FavoriteItem> {
        return try {
            val file = File(context.filesDir, "favorites.json")
            if (file.exists()) {
                val jsonString = file.readText()
                savedJson.decodeFromString<List<FavoriteItem>>(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all favorites", e)
            emptyList()
        }
    }
    
    // Function to save favorite to movie or series database when navigating to it
    fun saveFavoriteToDatabase(context: Context, favorite: FavoriteItem) {
        try {
            when (favorite.type) {
                "movie" -> {
                    // Convert FavoriteItem to Movie
                    val movie = Movie(
                        id = favorite.id,
                        type = favorite.type,
                        title = favorite.title,
                        description = favorite.description,
                        year = favorite.year,
                        imdb = favorite.imdb,
                        rating = favorite.rating,
                        duration = favorite.duration,
                        image = favorite.image,
                        cover = favorite.cover,
                        genres = favorite.genres,
                        sources = favorite.sources, // Include sources from favorites
                        country = favorite.country
                    )
                    saveMovieToFile(context, movie)
                }
                "series" -> saveSeriesToFile(context, favorite.toSeries())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite to database", e)
        }
    }
    
    fun saveMovieToFile(context: Context, movie: Movie) {
        try {
            // Clear all existing movie files first
            clearAllMovies(context)
            
            val jsonString = Json.encodeToString(movie)
            val fileName = "movie_${movie.id}.json"
            val file = File(context.filesDir, fileName)
            file.writeText(jsonString)
            Log.d(TAG, "Movie saved to file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving movie to file", e)
        }
    }
    
    fun loadMovieFromFile(context: Context, movieId: Int): Movie? {
        return try {
            val fileName = "movie_$movieId.json"
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val jsonString = file.readText()
                savedJson.decodeFromString<Movie>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading movie from file", e)
            null
        }
    }
    
    /**
     * Delete all movie files from storage
     */
    fun clearAllMovies(context: Context) {
        try {
            val filesDir = context.filesDir
            val movieFiles = filesDir.listFiles { file ->
                file.name.startsWith("movie_") && file.name.endsWith(".json")
            }
            
            movieFiles?.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted movie file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all movies", e)
        }
    }
    
    // Series functions
    fun saveSeriesToFile(context: Context, series: Series) {
        try {
            // Clear all existing series files first
            clearAllSeries(context)
            
            val jsonString = Json.encodeToString(series)
            val fileName = "series_${series.id}.json"
            val file = File(context.filesDir, fileName)
            file.writeText(jsonString)
            Log.d(TAG, "Series saved to file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving series to file", e)
        }
    }
    
    fun loadSeriesFromFile(context: Context, seriesId: Int): Series? {
        return try {
            val fileName = "series_$seriesId.json"
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString<Series>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading series from file", e)
            null
        }
    }
    
    /**
     * Delete all series files from storage
     */
    fun clearAllSeries(context: Context) {
        try {
            val filesDir = context.filesDir
            val seriesFiles = filesDir.listFiles { file ->
                file.name.startsWith("series_") && file.name.endsWith(".json")
            }
            
            seriesFiles?.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted series file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all series", e)
        }
    }
    
    // Group functions
    fun saveFavoriteGroup(context: Context, group: FavoriteGroup) {
        try {
            val groups = loadAllFavoriteGroups(context).toMutableList()
            
            // Remove existing group with same id if it exists
            groups.removeAll { it.id == group.id }
            
            // Add the new group
            groups.add(group)
            
            // Save all groups to a single file
            val jsonString = Json.encodeToString(groups)
            val file = File(context.filesDir, "favorite_groups.json")
            file.writeText(jsonString)
            Log.d(TAG, "Favorite group saved: ${group.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite group", e)
        }
    }
    
    fun removeFavoriteGroup(context: Context, groupId: String) {
        try {
            val groups = loadAllFavoriteGroups(context).toMutableList()
            
            // Remove the group with matching id (but don't allow removing default group)
            groups.removeAll { it.id == groupId && !it.isDefault }
            
            // Save updated groups list
            val jsonString = Json.encodeToString(groups)
            val file = File(context.filesDir, "favorite_groups.json")
            file.writeText(jsonString)
            Log.d(TAG, "Favorite group removed: $groupId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing favorite group", e)
        }
    }
    
    fun loadAllFavoriteGroups(context: Context): List<FavoriteGroup> {
        return try {
            val file = File(context.filesDir, "favorite_groups.json")
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString<List<FavoriteGroup>>(jsonString)
            } else {
                // Return a default group if no groups exist
                listOf(FavoriteGroup(id = "default", name = "Favorites", isDefault = true))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all favorite groups", e)
            // Return a default group if there's an error
            listOf(FavoriteGroup(id = "default", name = "Favorites", isDefault = true))
        }
    }
    
    fun getDefaultGroup(context: Context): FavoriteGroup {
        val groups = loadAllFavoriteGroups(context)
        return groups.find { it.isDefault } ?: FavoriteGroup(id = "default", name = "Favorites", isDefault = true)
    }
    
    fun addFavoriteToGroup(context: Context, groupId: String, favoriteId: Int, type: String) {
        try {
            val groups = loadAllFavoriteGroups(context).toMutableList()
            val group = groups.find { it.id == groupId }
            
            if (group != null) {
                // Add to the specified group (allowing multiple groups)
                if (type == "movie") {
                    group.addMovie(favoriteId)
                } else if (type == "series") {
                    group.addSeries(favoriteId)
                }
                
                // Save updated groups
                val jsonString = Json.encodeToString(groups)
                val file = File(context.filesDir, "favorite_groups.json")
                file.writeText(jsonString)
                Log.d(TAG, "Favorite added to group: $groupId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding favorite to group", e)
        }
    }
    
    fun removeFavoriteFromGroup(context: Context, groupId: String, favoriteId: Int, type: String) {
        try {
            val groups = loadAllFavoriteGroups(context).toMutableList()
            val group = groups.find { it.id == groupId }
            
            if (group != null) {
                if (type == "movie") {
                    group.removeMovie(favoriteId)
                } else if (type == "series") {
                    group.removeSeries(favoriteId)
                }
                
                // Save updated groups
                val jsonString = Json.encodeToString(groups)
                val file = File(context.filesDir, "favorite_groups.json")
                file.writeText(jsonString)
                Log.d(TAG, "Favorite removed from group: $groupId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing favorite from group", e)
        }
    }
    
    // New function to get all groups for a specific favorite
    fun getGroupsForFavorite(context: Context, favoriteId: Int, type: String): List<FavoriteGroup> {
        return try {
            val groups = loadAllFavoriteGroups(context)
            groups.filter { group ->
                if (type == "movie") {
                    group.containsMovie(favoriteId)
                } else if (type == "series") {
                    group.containsSeries(favoriteId)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting groups for favorite", e)
            emptyList()
        }
    }
    
    // New function to check if a favorite is in a specific group
    fun isFavoriteInGroup(context: Context, groupId: String, favoriteId: Int, type: String): Boolean {
        return try {
            val groups = loadAllFavoriteGroups(context)
            val group = groups.find { it.id == groupId }
            if (group != null) {
                if (type == "movie") {
                    group.containsMovie(favoriteId)
                } else if (type == "series") {
                    group.containsSeries(favoriteId)
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if favorite is in group", e)
            false
        }
    }
    
    fun getFavoritesInGroup(context: Context, groupId: String): List<FavoriteItem> {
        return try {
            val allFavorites = loadAllFavorites(context)
            val groups = loadAllFavoriteGroups(context)
            val group = groups.find { it.id == groupId }
            
            if (group != null) {
                val movieFavorites = allFavorites.filter { favorite ->
                    favorite.type == "movie" && group.movieIds.contains(favorite.id)
                }
                
                val seriesFavorites = allFavorites.filter { favorite ->
                    favorite.type == "series" && group.seriesIds.contains(favorite.id)
                }
                
                // Combine and sort by insertion order (newest first)
                (movieFavorites + seriesFavorites).sortedByDescending { favorite ->
                    // We don't have timestamps, so we'll just return them as is
                    favorite.id
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorites in group", e)
            emptyList()
        }
    }
    
    // Subtitle settings functions
    fun saveSubtitleSettings(context: Context, settings: SubtitleSettings) {
        try {
            val jsonString = Json.encodeToString(settings)
            val file = File(context.filesDir, "subtitle_settings.json")
            file.writeText(jsonString)
            Log.d(TAG, "Subtitle settings saved to file")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving subtitle settings to file", e)
        }
    }
    
    fun loadSubtitleSettings(context: Context): SubtitleSettings {
        return try {
            val file = File(context.filesDir, "subtitle_settings.json")
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString<SubtitleSettings>(jsonString)
            } else {
                SubtitleSettings.getDefaultSettings(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading subtitle settings from file", e)
            SubtitleSettings.getDefaultSettings(context)
        }
    }
    
    // Video player settings functions
    fun saveVideoPlayerSettings(context: Context, settings: VideoPlayerSettings) {
        try {
            val jsonString = Json.encodeToString(settings)
            val file = File(context.filesDir, "video_player_settings.json")
            file.writeText(jsonString)
            Log.d(TAG, "Video player settings saved to file")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video player settings to file", e)
        }
    }
    
    fun loadVideoPlayerSettings(context: Context): VideoPlayerSettings {
        return try {
            val file = File(context.filesDir, "video_player_settings.json")
            if (file.exists()) {
                val jsonString = file.readText()
                savedJson.decodeFromString<VideoPlayerSettings>(jsonString)
            } else {
                VideoPlayerSettings.DEFAULT
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading video player settings from file", e)
            VideoPlayerSettings.DEFAULT
        }
    }
    
    // Font settings functions
    fun saveFontSettings(context: Context, settings: FontSettings) {
        try {
            val jsonString = Json.encodeToString(settings)
            val file = File(context.filesDir, "font_settings.json")
            file.writeText(jsonString)
            Log.d(TAG, "Font settings saved to file")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving font settings to file", e)
        }
    }
    
    fun loadFontSettings(context: Context): FontSettings {
        return try {
            val file = File(context.filesDir, "font_settings.json")
            if (file.exists()) readFontSettingsFile(file) else FontSettings.DEFAULT
        } catch (e: Exception) {
            Log.e(TAG, "Error loading font settings from file", e)
            FontSettings.DEFAULT
        }
    }

    /** Normalize retired Persian selections on disk, retaining the English choice and future keys. */
    internal fun readFontSettingsFile(file: File): FontSettings {
        val jsonString = file.readText()
        val settings = savedJson.decodeFromString<FontSettings>(jsonString)
        if (settings.fontType == FontType.DEFAULT) {
            val saved = savedJson.parseToJsonElement(jsonString).jsonObject
            val name = saved["fontType"]?.jsonPrimitive?.contentOrNull
            if (name != null && !name.equals(FontType.DEFAULT.name, ignoreCase = true)) {
                file.writeText(savedJson.encodeToString(
                    JsonObject(saved + ("fontType" to JsonPrimitive(FontType.DEFAULT.name)))
                ))
            }
        }
        return settings
    }
    
    // ---------------------------------------------------------------
    // Live TV favorite channels
    // ---------------------------------------------------------------
    private const val FAVORITE_CHANNELS_FILE = "favorite_channels.json"

    fun loadFavoriteChannels(context: Context): List<FavoriteChannel> {
        return try {
            val file = File(context.filesDir, FAVORITE_CHANNELS_FILE)
            if (file.exists()) {
                val jsonString = file.readText()
                Json { ignoreUnknownKeys = true }.decodeFromString<List<FavoriteChannel>>(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading favorite channels", e)
            emptyList()
        }
    }

    private fun writeFavoriteChannels(context: Context, channels: List<FavoriteChannel>) {
        try {
            val jsonString = Json.encodeToString(channels)
            File(context.filesDir, FAVORITE_CHANNELS_FILE).writeText(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving favorite channels", e)
        }
    }

    fun isFavoriteChannel(context: Context, key: String): Boolean {
        return loadFavoriteChannels(context).any { it.key == key }
    }

    fun addFavoriteChannel(context: Context, channel: FavoriteChannel) {
        val channels = loadFavoriteChannels(context).toMutableList()
        channels.removeAll { it.key == channel.key }
        // Newest first
        channels.add(0, channel)
        writeFavoriteChannels(context, channels)
        Log.d(TAG, "Favorite channel added: ${channel.name}")
    }

    fun removeFavoriteChannel(context: Context, key: String) {
        val channels = loadFavoriteChannels(context).toMutableList()
        channels.removeAll { it.key == key }
        writeFavoriteChannels(context, channels)
        Log.d(TAG, "Favorite channel removed: $key")
    }

    /**
     * Toggles the favorite state of a channel.
     * @return true if the channel is now a favorite, false if it was removed.
     */
    fun toggleFavoriteChannel(context: Context, channel: FavoriteChannel): Boolean {
        return if (isFavoriteChannel(context, channel.key)) {
            removeFavoriteChannel(context, channel.key)
            false
        } else {
            addFavoriteChannel(context, channel)
            true
        }
    }

    fun clearFavoriteChannels(context: Context) {
        try {
            val file = File(context.filesDir, FAVORITE_CHANNELS_FILE)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing favorite channels", e)
        }
    }

    // Welcome screen state functions
    fun saveWelcomeCompleted(context: Context) {
        try {
            val file = File(context.filesDir, "welcome_completed.flag")
            file.writeText("completed")
            Log.d(TAG, "Welcome screen marked as completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving welcome completed state", e)
        }
    }
    
    fun isWelcomeCompleted(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, "welcome_completed.flag")
            file.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking welcome completed state", e)
            false
        }
    }
    
    // Persist the series link selector independently from the one-item detail cache.
    private const val SERIES_LINK_PREFS = "series_link_selections"

    fun loadSeriesLinkSelection(context: Context, seriesId: Int): SeriesLinkSelection = try {
        val json = context.getSharedPreferences(SERIES_LINK_PREFS, Context.MODE_PRIVATE)
            .getString(seriesId.toString(), null)
        if (json == null) SeriesLinkSelection() else Json.decodeFromString<SeriesLinkSelection>(json)
    } catch (e: Exception) {
        Log.e(TAG, "Error loading series link selection", e)
        SeriesLinkSelection()
    }

    fun saveSeriesLinkSelection(context: Context, seriesId: Int, selection: SeriesLinkSelection) {
        try {
            context.getSharedPreferences(SERIES_LINK_PREFS, Context.MODE_PRIVATE).edit()
                .putString(seriesId.toString(), Json.encodeToString(selection)).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving series link selection", e)
        }
    }

    // Playback history is independent of favorites and the legacy "episode started" marks.
    private const val PLAYBACK_HISTORY_FILE = "playback_history.json"

    @Synchronized
    fun loadPlaybackHistory(context: Context): List<PlaybackRecord> = try {
        val file = File(context.filesDir, PLAYBACK_HISTORY_FILE)
        if (file.exists()) Json.decodeFromString<List<PlaybackRecord>>(file.readText())
            .sortedByDescending { it.updatedAt } else emptyList()
    } catch (e: Exception) {
        Log.e(TAG, "Error loading playback history", e)
        emptyList()
    }

    fun playbackFor(context: Context, key: PlaybackKey): PlaybackRecord? =
        loadPlaybackHistory(context).firstOrNull { it.key == key }

    /** Called periodically while playing, when paused/closed and at end of media. */
    @Synchronized
    fun savePlaybackProgress(
        context: Context,
        entry: PlaybackRecord,
        positionMs: Long,
        durationMs: Long,
        ended: Boolean = false
    ) {
        if (entry.contentId <= 0 || entry.url.isBlank() ||
            (entry.type != "movie" && entry.type != "series") ||
            (entry.type == "series" && (entry.seasonId == null || entry.episodeId == null))
        ) return
        try {
            val position = positionMs.coerceAtLeast(0L)
            val duration = durationMs.coerceAtLeast(0L)
            val updated = entry.copy(
                positionMs = if (duration > 0L) position.coerceAtMost(duration) else position,
                durationMs = duration,
                updatedAt = System.currentTimeMillis(),
                ended = ended
            )
            val records = loadPlaybackHistory(context).filterNot { it.key == entry.key }
            writePlaybackHistory(context, (listOf(updated) + records).sortedByDescending { it.updatedAt })
        } catch (e: Exception) {
            Log.e(TAG, "Error saving playback progress", e)
        }
    }

    @Synchronized
    fun removePlayback(context: Context, key: PlaybackKey) {
        try {
            writePlaybackHistory(context, loadPlaybackHistory(context).filterNot { it.key == key })
        } catch (e: Exception) {
            Log.e(TAG, "Error removing playback history entry", e)
        }
    }

    @Synchronized
    fun clearPlaybackHistory(context: Context) {
        File(context.filesDir, PLAYBACK_HISTORY_FILE).delete()
        File(context.filesDir, "$PLAYBACK_HISTORY_FILE.tmp").delete()
    }

    private fun writePlaybackHistory(context: Context, records: List<PlaybackRecord>) {
        val file = File(context.filesDir, PLAYBACK_HISTORY_FILE)
        val temporary = File(context.filesDir, "$PLAYBACK_HISTORY_FILE.tmp")
        val json = Json.encodeToString(records)
        temporary.writeText(json)
        // An interrupted write must leave the previous history readable.
        if (!temporary.renameTo(file)) {
            file.writeText(json)
            temporary.delete()
        }
    }

    // Watched episodes functions
    fun saveWatchedEpisode(context: Context, watchedEpisode: WatchedEpisode) {
        try {
            val watchedEpisodes = loadAllWatchedEpisodes(context).toMutableList()
            
            // Remove existing entry for the same episode if it exists
            watchedEpisodes.removeAll { 
                it.seriesId == watchedEpisode.seriesId && 
                it.seasonId == watchedEpisode.seasonId && 
                it.episodeId == watchedEpisode.episodeId 
            }
            
            // Add the new watched episode
            watchedEpisodes.add(watchedEpisode)
            
            // Save all watched episodes to a single file
            val jsonString = Json.encodeToString(watchedEpisodes)
            val file = File(context.filesDir, "watched_episodes.json")
            file.writeText(jsonString)
            Log.d(TAG, "Watched episode saved: Series ${watchedEpisode.seriesId}, Season ${watchedEpisode.seasonId}, Episode ${watchedEpisode.episodeId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving watched episode", e)
        }
    }
    
    fun removeWatchedEpisode(context: Context, seriesId: Int, seasonId: Int, episodeId: Int) {
        try {
            val watchedEpisodes = loadAllWatchedEpisodes(context).toMutableList()
            
            // Remove the watched episode with matching ids
            watchedEpisodes.removeAll { 
                it.seriesId == seriesId && 
                it.seasonId == seasonId && 
                it.episodeId == episodeId 
            }
            
            // Save updated watched episodes list
            val jsonString = Json.encodeToString(watchedEpisodes)
            val file = File(context.filesDir, "watched_episodes.json")
            file.writeText(jsonString)
            Log.d(TAG, "Watched episode removed: Series $seriesId, Season $seasonId, Episode $episodeId")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing watched episode", e)
        }
    }
    
    fun isEpisodeWatched(context: Context, seriesId: Int, seasonId: Int, episodeId: Int): Boolean {
        return try {
            val watchedEpisodes = loadAllWatchedEpisodes(context)
            watchedEpisodes.any { 
                it.seriesId == seriesId && 
                it.seasonId == seasonId && 
                it.episodeId == episodeId 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if episode is watched", e)
            false
        }
    }
    
    fun loadAllWatchedEpisodes(context: Context): List<WatchedEpisode> {
        return try {
            val file = File(context.filesDir, "watched_episodes.json")
            if (file.exists()) {
                val jsonString = file.readText()
                Json.decodeFromString<List<WatchedEpisode>>(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all watched episodes", e)
            emptyList()
        }
    }
    
    fun clearAllWatchedEpisodes(context: Context) {
        try {
            val file = File(context.filesDir, "watched_episodes.json")
            if (file.exists()) {
                file.delete()
            }
            Log.d(TAG, "All watched episodes cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all watched episodes", e)
        }
    }
}