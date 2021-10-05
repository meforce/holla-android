package com.hollabrowser.meforce

import android.content.Intent
import android.view.KeyEvent
import android.webkit.CookieManager
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import io.reactivex.Completable

class MainActivity : BrowserActivity() {

    public override fun updateCookiePreference(): Completable = Completable.fromAction {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(userPreferences.cookiesEnabled)
    }

    override fun onNewIntent(intent: Intent) {
        handleNewIntent(intent)
        super.onNewIntent(intent)
    }

    /**
     * This is called once our activity is not visible anymore.
     * That's where we should save our data according to the docs.
     * https://developer.android.com/guide/components/activities/activity-lifecycle#onstop
     * Saving data can't wait for onDestroy as there is no guarantee onDestroy will ever be called.
     * In fact even when user closes our Task from recent Task list our activity is just terminated without getting any notifications.
     */
    override fun onStop() {
        super.onStop()
        saveOpenTabsIfNeeded()
    }

    override fun updateHistory(title: String?, url: String) = addItemToHistory(title, url)

    override fun isIncognito() = false

    override fun closeActivity() = closePanels {
        performExitCleanUp()
        moveTaskToBack(true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_P ->
                    // Open a new private window
                    if (event.isShiftPressed) {
                        startActivity(IncognitoActivity.createIntent(this))
                        return true
                    }
            }
        }
        return super.dispatchKeyEvent(event)
    }

}
