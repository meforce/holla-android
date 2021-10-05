package com.hollabrowser.meforce.search.suggestions

import android.app.Application
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.constant.UTF8
import com.hollabrowser.meforce.database.SearchSuggestion
import com.hollabrowser.meforce.extensions.map
import com.hollabrowser.meforce.extensions.preferredLocale
import com.hollabrowser.meforce.log.Logger
import com.hollabrowser.meforce.preference.UserPreferences
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * The search suggestions provider for the DuckDuckGo search engine.
 */
class DuckSuggestionsModel(
    okHttpClient: Single<OkHttpClient>,
    requestFactory: RequestFactory,
    application: Application,
    logger: Logger,
    userPreferences: UserPreferences
) : BaseSuggestionsModel(okHttpClient, requestFactory, UTF8, application.preferredLocale, logger, userPreferences) {

    private val searchSubtitle = application.getString(R.string.suggestion)

    // https://duckduckgo.com/ac/?q={query}
    override fun createQueryUrl(query: String, language: String): HttpUrl = HttpUrl.Builder()
        .scheme("https")
        .host("duckduckgo.com")
        .encodedPath("/ac/")
        .addEncodedQueryParameter("q", query)
        .build()

    @Throws(Exception::class)
    override fun parseResults(responseBody: ResponseBody): List<SearchSuggestion> {
        return JSONArray(responseBody.string())
            .map { it as JSONObject }
            .map { it.getString("phrase") }
            .map { SearchSuggestion("$searchSubtitle \"$it\"", it) }
    }

}
