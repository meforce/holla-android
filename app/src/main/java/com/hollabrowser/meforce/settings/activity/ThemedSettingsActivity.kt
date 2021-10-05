package com.hollabrowser.meforce.settings.activity

import android.graphics.Color
import com.hollabrowser.meforce.AccentTheme
import com.hollabrowser.meforce.AppTheme
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.ThemedActivity
import com.hollabrowser.meforce.extensions.setStatusBarIconsColor
import com.hollabrowser.meforce.utils.ThemeUtils
import com.hollabrowser.meforce.utils.foregroundColorFromBackgroundColor


abstract class ThemedSettingsActivity : ThemedActivity() {

    override fun onResume() {
        super.onResume()
        // Make sure icons have the right color
        window.setStatusBarIconsColor(foregroundColorFromBackgroundColor(ThemeUtils.getPrimaryColor(this)) == Color.BLACK && !userPreferences.useBlackStatusBar)
        resetPreferences()
        if (userPreferences.useTheme != themeId) {
            recreate()
        }
        if (userPreferences.useAccent != accentId) {
            recreate()
        }
    }

    /**
     * From ThemedActivity
     */
    override fun themeStyle(aTheme: AppTheme): Int {
        return when (aTheme) {
		    AppTheme.DEFAULT -> R.style.Theme_App_DayNight_Settings
            AppTheme.LIGHT -> R.style.Theme_App_Light_Settings
            AppTheme.DARK ->  R.style.Theme_App_Dark_Settings
            AppTheme.BLACK -> R.style.Theme_App_Black_Settings
        }
    }

    override fun accentStyle(accentTheme: AccentTheme): Int? {
        return when (accentTheme) {
            AccentTheme.BLUE -> R.style.Accent_Blue
            AccentTheme.PINK -> R.style.Accent_Pink
            AccentTheme.PURPLE ->  R.style.Accent_Puple
            AccentTheme.DEEP_PURPLE -> R.style.Accent_Deep_Purple
            AccentTheme.INDIGO -> R.style.Accent_Indigo
            AccentTheme.DEFAULT_ACCENT -> null
            AccentTheme.LIGHT_BLUE -> R.style.Accent_Light_Blue
            AccentTheme.CYAN -> R.style.Accent_Cyan
            AccentTheme.TEAL -> R.style.Accent_Teal
            AccentTheme.GREEN -> R.style.Accent_Green
            AccentTheme.LIGHT_GREEN -> R.style.Accent_Light_Green
            AccentTheme.LIME -> R.style.Accent_Lime
            AccentTheme.YELLOW -> R.style.Accent_Yellow
            AccentTheme.AMBER -> R.style.Accent_Amber
            AccentTheme.ORANGE -> R.style.Accent_Orange
            AccentTheme.DEEP_ORANGE -> R.style.Accent_Deep_Orange
            AccentTheme.BROWN -> R.style.Accent_Brown
        }
    }

}
