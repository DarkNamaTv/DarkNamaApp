package com.dark.darknama.data.repository

/** A raw API page may contain records which could not be parsed. Only the feed's
 * own end marker (an empty raw page), not an empty filtered result, ends paging. */
internal data class FeedPage<T>(val items: List<T>, val hasMore: Boolean)

data class FilteredPage<T>(
    val items: List<T>,
    /** First page not yet requested; use this for the next infinite-scroll batch. */
    val nextPage: Int,
    val hasMore: Boolean
)

/** Fetch up to [maxRequests] pages in one batch. A feed with five consecutive
 * all-ad pages returns an empty but *loadable* batch instead of looping forever. */
internal suspend fun <T> fetchFilteredPages(
    startPage: Int,
    minItems: Int = 20,
    maxRequests: Int = 5,
    fetchPage: suspend (Int) -> FeedPage<T>,
    isExcluded: (T) -> Boolean,
    onExcluded: (T) -> Unit = {}
): FilteredPage<T> {
    require(startPage >= 0 && minItems > 0 && maxRequests > 0)
    val collected = mutableListOf<T>()
    var page = startPage
    repeat(maxRequests) {
        val raw = fetchPage(page++)
        for (item in raw.items) {
            if (isExcluded(item)) onExcluded(item) else collected.add(item)
        }
        if (!raw.hasMore) return FilteredPage(collected, page, false)
        if (collected.size >= minItems) return FilteredPage(collected, page, true)
    }
    return FilteredPage(collected, page, true)
}
