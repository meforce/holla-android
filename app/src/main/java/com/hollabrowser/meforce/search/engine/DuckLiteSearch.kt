package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The DuckDuckGo Lite search engine.
 */
class DuckLiteSearch : BaseSearchEngine(
    "file:///android_asset/duckduckgo.webp",
    "https://duckduckgo.com/lite/?t=styx&q=",
    R.string.search_engine_duckduckgo_lite
)
