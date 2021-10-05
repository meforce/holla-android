package com.hollabrowser.meforce.browser.cleanup

import android.webkit.WebView
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.log.Logger
import com.hollabrowser.meforce.utils.WebUtils
import javax.inject.Inject

/**
 * Exit cleanup that should be run when the incognito process is exited on API >= 28. This cleanup
 * clears cookies and all web data, which can be done without affecting
 */
class EnhancedIncognitoExitCleanup @Inject constructor(
    private val logger: Logger
) : ExitCleanup {
    override fun cleanUp(webView: WebView?, context: BrowserActivity) {
        WebUtils.clearCache(webView, context)
        logger.log(TAG, "Cache Cleared")
        WebUtils.clearCookies()
        logger.log(TAG, "Cookies Cleared")
        WebUtils.clearWebStorage()
        logger.log(TAG, "WebStorage Cleared")
    }

    companion object {
        private const val TAG = "EnhancedIncognitoExitCleanup"
    }
}
