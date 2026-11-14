package com.dark.darknama.ui.country

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.repository.GenreRepository
import com.dark.darknama.data.model.Poster
import com.dark.darknama.data.repository.CountryPostersRepository
import com.dark.darknama.data.repository.CountryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CountryViewModel(application: Application) : AndroidViewModel(application) {
    private val postersRepository = CountryPostersRepository(application)
    private val countryRepository = CountryRepository(application)
    private val genreRepository = GenreRepository(application)

    var posters by mutableStateOf<List<Poster>>(emptyList())
        private set
    var countryName by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var currentPage by mutableStateOf(0)
        private set
    var canLoadMore by mutableStateOf(true)
        private set
    var needsManualLoadMore by mutableStateOf(false)
        private set
    var countryId by mutableStateOf<Int?>(null)
        private set
    var selectedFilterType by mutableStateOf(FilterType.DEFAULT)
        private set

    var genres by mutableStateOf<List<Genre>>(emptyList())
        private set
    var selectedGenreId by mutableStateOf(0)
        private set
    private var genresJob: Job? = null

    fun loadGenres() {
        if (genres.isNotEmpty() || genresJob?.isActive == true) return
        genresJob = viewModelScope.launch {
            try {
                genres = genreRepository.getGenres()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Retain the all-genres option on failure.
            }
        }
    }

    fun selectGenre(id: Int) {
        if (selectedGenreId != id) {
            selectedGenreId = id
            posters = emptyList()
            refresh()
        }
    }

    private var loadJob: Job? = null
    private var requestId = 0L
    private var failedPage = 0

    fun setCountryId(id: Int) {
        if (countryId != id) {
            countryId = id
            posters = emptyList()
            loadCountryName()
            refresh()
        }
    }

    fun selectFilterType(filterType: FilterType) {
        if (selectedFilterType != filterType) {
            selectedFilterType = filterType
            posters = emptyList()
            refresh()
        }
    }

    private fun loadCountryName() {
        viewModelScope.launch {
            try {
                val countries = countryRepository.getAllCountries()
                countryName = countries.find { it.id == countryId }?.title ?: "Country"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                countryName = "Country"
            }
        }
    }

    fun loadPosters(page: Int = 0) {
        val country = countryId ?: return
        if (loadJob?.isActive == true) return
        val id = ++requestId
        val sort = selectedFilterType
        val genre = selectedGenreId
        loadJob = viewModelScope.launch {
            if (page == 0) {
                isLoading = true
                canLoadMore = true
            } else isLoadingMore = true
            needsManualLoadMore = false
            errorMessage = null
            try {
                val batch = postersRepository.getPosterBatch(country, page, sort, genre)
                if (id == requestId) {
                    val previous = if (page == 0) emptyList() else posters
                    val updated = (previous + batch.items).distinctBy { "${it.type}:${it.id}" }
                    posters = updated
                    currentPage = batch.nextPage - 1
                    canLoadMore = batch.hasMore
                    needsManualLoadMore = batch.hasMore && updated.size == previous.size
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (id == requestId) {
                    failedPage = page
                    errorMessage = e.message
                }
            } finally {
                if (id == requestId) {
                    isLoading = false
                    isLoadingMore = false
                }
            }
        }
    }

    fun loadMorePosters() {
        if (!isLoading && !isLoadingMore && canLoadMore && errorMessage == null) {
            loadPosters(currentPage + 1)
        }
    }

    fun retry() { loadPosters(failedPage) }

    fun refresh() {
        loadJob?.cancel()
        failedPage = 0
        loadPosters(0)
    }
}
