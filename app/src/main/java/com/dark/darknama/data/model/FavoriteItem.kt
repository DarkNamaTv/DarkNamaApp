package com.dark.darknama.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteItem(
    val id: Int,
    val type: String, // "movie" or "series"
    val title: String,
    val description: String,
    val year: Int,
    val imdb: Double,
    val rating: Double,
    val duration: String?,
    val image: String,
    val cover: String,
    val genres: List<Genre>,
    val country: List<Country>,
    val sources: List<Source> = emptyList() // Add sources for movies
)

/** Favorite series can also be tracked for updates before their detail page is visited. */
fun FavoriteItem.toSeries(): Series = Series(
    id = id, type = type, title = title, description = description, year = year,
    imdb = imdb, rating = rating, duration = duration, image = image, cover = cover,
    genres = genres, country = country
)
