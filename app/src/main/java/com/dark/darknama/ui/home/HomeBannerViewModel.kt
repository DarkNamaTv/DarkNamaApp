package com.dark.darknama.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dark.darknama.data.model.HomeSlide
import com.dark.darknama.data.repository.HomeBannerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class HomeBannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeBannerRepository(application)

    var slides by mutableStateOf<List<HomeSlide>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var hasError by mutableStateOf(false)
        private set

    init { load() }

    fun load() {
        if (isLoading && slides.isNotEmpty()) return
        viewModelScope.launch {
            isLoading = true
            hasError = false
            try {
                slides = repository.latest()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }
}
