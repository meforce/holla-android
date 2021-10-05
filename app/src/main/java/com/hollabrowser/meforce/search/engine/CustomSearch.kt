package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.preference.UserPreferences

/**
 * A custom search engine.
 */
class CustomSearch(queryUrl: String, userPreferences: UserPreferences) : BaseSearchEngine(
    userPreferences.imageUrlString,
    queryUrl,
    R.string.search_engine_custom
)
