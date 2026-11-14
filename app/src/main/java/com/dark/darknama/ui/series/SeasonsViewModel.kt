package com.dark.darknama.ui.series

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.Season
import com.dark.darknama.data.model.EpisodeQualityLinks
import com.dark.darknama.data.repository.EpisodeLinkGroups
import com.dark.darknama.data.repository.SeasonsRepository
import kotlinx.coroutines.launch

class SeasonsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SeasonsRepository(application)
    
    var seasons by mutableStateOf<List<Season>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Distinguishes a successfully fetched (possibly empty) response from initial state/error.
    var loadedSeriesId by mutableStateOf<Int?>(null)
        private set
    
    fun episodeLinksByQuality(season: Season): List<EpisodeQualityLinks> =
        EpisodeLinkGroups.forSeason(season)

    fun loadSeasons(seriesId: Int) {
        loadedSeriesId = null
        viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                seasons = repository.getSeasons(seriesId)
                loadedSeriesId = seriesId
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }
}