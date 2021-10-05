package com.hollabrowser.meforce.browser.cleanup

import android.webkit.WebView
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.database.history.HistoryDatabase
import com.hollabrowser.meforce.di.DatabaseScheduler
import com.hollabrowser.meforce.log.Logger
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.WebUtils
import io.reactivex.Scheduler
import javax.inject.Inject

/**
 * Exit cleanup that should run whenever the main browser process is exiting.
 */
class NormalExitCleanup @Inject constructor(
    private val userPreferences: UserPreferences,
    private val logger: Logger,
    private val historyDatabase: HistoryDatabase,
    @DatabaseScheduler private val databaseScheduler: Scheduler
) : ExitCleanup {
    override fun cleanUp(webView: WebView?, context: BrowserActivity) {
        if (userPreferences.clearCacheExit) {
            WebUtils.clearCache(webView, context)
            logger.log(TAG, "Cache Cleared")
        }
        if (userPreferences.clearHistoryExitEnabled) {
            WebUtils.clearHistory(context, historyDatabase, databaseScheduler)
            logger.log(TAG, "History Cleared")
        }
        if (userPreferences.clearCookiesExitEnabled) {
            WebUtils.clearCookies()
            logger.log(TAG, "Cookies Cleared")
        }
        if (userPreferences.clearWebStorageExitEnabled) {
            WebUtils.clearWebStorage()
            logger.log(TAG, "WebStorage Cleared")
        }
    }

    companion object {
        const val TAG = "NormalExitCleanup"
    }
}
