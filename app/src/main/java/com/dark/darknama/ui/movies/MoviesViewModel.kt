package com.dark.darknama.ui.movies

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.Genre
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.repository.GenreRepository
import com.dark.darknama.data.repository.MovieRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MoviesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(application)
    private val genreRepository = GenreRepository(application)

    var movies by mutableStateOf<List<Movie>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingMore by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    /** Last raw API page fetched (not the page that happened to contain a survivor). */
    var currentPage by mutableStateOf(0)
        private set
    var canLoadMore by mutableStateOf(true)
        private set
    /** A bounded batch yielded no new items; offer an explicit next-batch action. */
    var needsManualLoadMore by mutableStateOf(false)
        private set
    var genres by mutableStateOf<List<Genre>>(emptyList())
        private set
    var selectedGenreId by mutableStateOf(0)
        private set
    var selectedFilterType by mutableStateOf(FilterType.DEFAULT)
        private set

    private var loadJob: Job? = null
    private var requestId = 0L
    private var failedPage = 0

    init {
        loadGenres()
        loadMovies()
    }

    fun loadGenres() {
        viewModelScope.launch {
            try {
                genres = genreRepository.getGenres()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }
    }

    fun selectGenre(genreId: Int) {
        if (selectedGenreId == genreId) return
        selectedGenreId = genreId
        movies = emptyList()
        refresh()
    }

    fun selectFilterType(filterType: FilterType) {
        if (selectedFilterType == filterType) return
        selectedFilterType = filterType
        movies = emptyList()
        refresh()
    }

    fun loadMovies(page: Int = 0) {
        if (loadJob?.isActive == true) return
        val id = ++requestId
        val genre = selectedGenreId
        val sort = selectedFilterType
        loadJob = viewModelScope.launch {
            if (page == 0) {
                isLoading = true
                canLoadMore = true
            } else isLoadingMore = true
            needsManualLoadMore = false
            errorMessage = null
            try {
                val batch = repository.getMovieBatch(page, genre, sort)
                if (id == requestId) {
                    val previous = if (page == 0) emptyList() else movies
                    val updated = (previous + batch.items).distinctBy { it.id }
                    movies = updated
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

    fun loadMoreMovies() {
        if (!isLoading && !isLoadingMore && canLoadMore && errorMessage == null) {
            loadMovies(currentPage + 1)
        }
    }

    fun retry() { loadMovies(failedPage) }

    fun refresh() {
        loadJob?.cancel()
        failedPage = 0
        loadMovies(0)
    }
}
