package com.dark.darknama.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.Country
import com.dark.darknama.data.model.Poster
import com.dark.darknama.data.repository.CountryRepository
import com.dark.darknama.data.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SearchRepository(application)
    private val countryRepository = CountryRepository(application)
    
    var searchResults by mutableStateOf<List<Poster>>(emptyList())
        private set
    
    var countries by mutableStateOf<List<Country>>(emptyList())
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var isCountriesLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    var searchQuery by mutableStateOf("")
        private set
    
    // New state to track if a search has been performed
    var hasSearched by mutableStateOf(false)
        private set
    
    private var searchJob: Job? = null
    
    init {
        loadCountries()
    }
    
    fun updateSearchQuery(query: String) {
        if (query == searchQuery) return
        // Both the TV EditText and the phone field search only on submission.
        // Do not leave stale results (or a late response to the old query) on screen.
        searchJob?.cancel()
        searchQuery = query
        searchResults = emptyList()
        errorMessage = null
        hasSearched = false
        isLoading = false
    }

    fun triggerSearch() {
        val query = searchQuery.trim()
        if (query.isNotEmpty()) {
            hasSearched = true
            search(query)
        }
    }
    
    private fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                isLoading = true
                errorMessage = null
                
                val result = repository.search(query)
                
                // The repository removes promotional movies; series stay untouched.
                searchResults = result.posters
            } catch (e: CancellationException) {
                throw e // A later query must not surface as an error or overwrite results.
            } catch (e: Exception) {
                errorMessage = e.message
                searchResults = emptyList()
            } finally {
                if (currentCoroutineContext().isActive) isLoading = false
            }
        }
    }
    
    private fun loadCountries() {
        viewModelScope.launch {
            try {
                isCountriesLoading = true
                countries = countryRepository.getAllCountries()
            } catch (e: Exception) {
                // Handle error silently for countries
                countries = emptyList()
            } finally {
                isCountriesLoading = false
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        searchQuery = ""
        searchResults = emptyList()
        errorMessage = null
        hasSearched = false
        isLoading = false
    }
}