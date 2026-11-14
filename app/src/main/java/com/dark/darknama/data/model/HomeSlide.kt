package com.dark.darknama.data.model

/** Page-zero catalogue items are sorted by creation time *within* each feed. */
sealed interface HomeSlide {
    val key: String
    val title: String
    val backdrop: String
    val imdb: Double

    data class MovieSlide(val movie: Movie) : HomeSlide {
        override val key = "movie:${movie.id}"
        override val title = movie.title
        override val backdrop = movie.cover.ifBlank { movie.image }
        override val imdb = movie.imdb
    }

    data class SeriesSlide(val series: Series) : HomeSlide {
        override val key = "series:${series.id}"
        override val title = series.title
        override val backdrop = series.cover.ifBlank { series.image }
        override val imdb = series.imdb
    }
}

object HomeSlideOrder {
    /** Both feeds have no shared timestamp; preserve each one's newest-first order. */
    fun interleave(movies: List<Movie>, series: List<Series>, perType: Int = 5): List<HomeSlide> =
        buildList {
            repeat(perType) { index ->
                movies.getOrNull(index)?.let { add(HomeSlide.MovieSlide(it)) }
                series.getOrNull(index)?.let { add(HomeSlide.SeriesSlide(it)) }
            }
        }.distinctBy { it.key }
}
