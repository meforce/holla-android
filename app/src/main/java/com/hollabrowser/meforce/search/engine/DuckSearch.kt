package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The DuckDuckGo search engine.
 */
class DuckSearch : BaseSearchEngine(
    "file:///android_asset/duckduckgo.webp",
    "https://duckduckgo.com/?t=styx&q=",
    R.string.search_engine_duckduckgo
)
