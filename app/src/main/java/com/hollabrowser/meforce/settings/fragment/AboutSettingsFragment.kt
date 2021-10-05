/*
 * Copyright 2014 A.C.R. Development
 */
package com.hollabrowser.meforce.settings.fragment

import android.os.Bundle
import androidx.webkit.WebViewCompat
import com.hollabrowser.meforce.BuildConfig
import com.hollabrowser.meforce.R

class AboutSettingsFragment : AbstractSettingsFragment() {

    override fun providePreferencesXmlResource() = R.xml.preference_about

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        var webview = resources.getString(R.string.unknown)

        context?.let {
            WebViewCompat.getCurrentWebViewPackage(it)?.versionName?.let { it1 ->
                webview = it1
            }
        }

        clickablePreference(
            preference = SETTINGS_VERSION,
            summary = "${getString(R.string.pref_app_version_summary)} ${BuildConfig.VERSION_NAME} (${getString(R.string.app_version_name)})"
        )

        clickablePreference(
            preference = WEBVIEW_VERSION,
            summary = webview,
            onClick = { }
        )

    }

    companion object {
        private const val SETTINGS_VERSION = "pref_version"
        private const val WEBVIEW_VERSION = "pref_webview"
    }
}