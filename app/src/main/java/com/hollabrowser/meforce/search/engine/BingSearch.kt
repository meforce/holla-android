package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The Bing search engine.
 */
class BingSearch : BaseSearchEngine(
    "file:///android_asset/bing.webp",
    "https://www.bing.com/search?q=",
    R.string.search_engine_bing
)
