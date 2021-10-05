package com.hollabrowser.meforce.settings.fragment

import android.os.Bundle
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.preference.UserPreferences
import javax.inject.Inject

/**
 * The extension settings of the app.
 */
class TabsSettingsFragment : AbstractSettingsFragment() {

    @Inject lateinit var userPreferences: UserPreferences

    override fun providePreferencesXmlResource() = R.xml.preference_tabs

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_tabs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        injector.inject(this)

        switchPreference(
            preference = SETTINGS_LAST_TAB,
            isChecked = userPreferences.closeOnLastTab,
            onCheckChange = { userPreferences.closeOnLastTab = it }
        )

    }

    companion object {
        private const val SETTINGS_LAST_TAB = "last_tab"
    }
}
