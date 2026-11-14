package com.dark.darknama.data.repository

import android.content.Context
import com.dark.darknama.data.model.FilterType
import com.dark.darknama.data.model.HomeSlide
import com.dark.darknama.data.model.HomeSlideOrder
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.Series
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Fetch the newest page of both catalogue feeds. A single failing feed does not hide the other. */
class HomeBannerRepository(context: Context) {
    private val movies = MovieRepository(context)
    private val series = SeriesRepository(context)

    suspend fun latest(): List<HomeSlide> {
        var firstError: Exception? = null
        val newestMovies: List<Movie> = try {
            // The same repository-level ad rule applies to banner movie slides.
            movies.getMovieBatch(0, 0, FilterType.DEFAULT, minItems = 5).items
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            firstError = e
            emptyList()
        }
        val newestSeries: List<Series> = try {
            series.getSeries(0, 0, FilterType.DEFAULT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (firstError != null) throw IOException("Neither catalogue feed is available", e)
            emptyList()
        }
        return HomeSlideOrder.interleave(newestMovies, newestSeries)
    }
}
