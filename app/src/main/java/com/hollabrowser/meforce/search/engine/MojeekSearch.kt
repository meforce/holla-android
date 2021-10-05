package com.hollabrowser.meforce.search.engine

import com.hollabrowser.meforce.R

/**
 * The Mojeek search engine.
 */
class MojeekSearch : BaseSearchEngine(
    "file:///android_asset/mojeek.webp",
    "https://www.mojeek.com/search?q=",
    R.string.search_engine_mojeek
)
