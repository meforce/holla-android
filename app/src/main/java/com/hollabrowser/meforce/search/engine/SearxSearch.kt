package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The Searx search engine.
 */
class SearxSearch : BaseSearchEngine(
    "file:///android_asset/searx.webp",
    "https://searx.prvcy.eu/search?q=",
    R.string.search_engine_searx
)
