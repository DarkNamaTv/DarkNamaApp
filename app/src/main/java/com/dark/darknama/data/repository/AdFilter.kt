package com.dark.darknama.data.repository

import android.util.Log
import com.dark.darknama.BuildConfig
import com.dark.darknama.data.model.Movie
import com.dark.darknama.data.model.Poster
import com.dark.darknama.utils.containsPersian

/** One permanent rule for promotional *movie* entries in API-loaded lists. */
fun isPromotional(movie: Movie): Boolean = movie.title.containsPersian()

/** Country and search feeds also contain series: never suppress those here. */
fun isPromotional(poster: Poster): Boolean = poster.isMovie() && poster.title.containsPersian()

internal fun logSkippedPromotionalTitle(title: String) {
    if (BuildConfig.DEBUG) Log.d("AdFilter", "Skipping '$title': movie title contains Persian characters")
}
