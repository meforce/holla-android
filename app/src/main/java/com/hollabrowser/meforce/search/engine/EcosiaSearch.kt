package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The Ecosia search engine.
 */
class EcosiaSearch : BaseSearchEngine(
    "file:///android_asset/ecosia.webp",
    "https://www.ecosia.org/search?q=",
    R.string.search_engine_ecosia
)
