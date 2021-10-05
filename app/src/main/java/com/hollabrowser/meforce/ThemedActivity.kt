package com.hollabrowser.meforce

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.ThemeUtils
import javax.inject.Inject

abstract class ThemedActivity : AppCompatActivity() {

    @Inject lateinit var userPreferences: UserPreferences

    protected var accentId: AccentTheme = AccentTheme.BLUE
    protected var themeId: AppTheme = AppTheme.LIGHT
    private var isDarkTheme: Boolean = false
    val useDarkTheme get() = isDarkTheme

    /**
     * Override this to provide an alternate theme that should be set for every instance of this
     * activity regardless of the user's preference.
     */
    protected open fun provideThemeOverride(): AppTheme? = null

    protected open fun provideAccentThemeOverride(): AccentTheme? = null

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    protected open fun onWindowVisibleToUserAfterResume() = Unit

    /**
     * Implement this to provide themes resource style ids.
     */
    @StyleRes
    abstract fun themeStyle(aTheme: AppTheme): Int

    @StyleRes
    protected abstract fun accentStyle(accentTheme: AccentTheme): Int?

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        themeId = userPreferences.useTheme
        accentId = userPreferences.useAccent
        // set the theme
        applyTheme(provideThemeOverride()?:themeId)
        applyAccent()
        super.onCreate(savedInstanceState)
        resetPreferences()
    }

    /**
     *
     */
    protected fun resetPreferences() {
        if (userPreferences.useBlackStatusBar) {
            window.statusBarColor = Color.BLACK
        } else {
            window.statusBarColor = ThemeUtils.getStatusBarColor(this)
        }
    }

    /**
     *
     */
    protected fun applyTheme(themeId: AppTheme) {
        setTheme(themeStyle(themeId))
        // Check if we have a dark theme
        val mode = resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
        isDarkTheme = themeId == AppTheme.BLACK // Black qualifies as dark theme
                || themeId == AppTheme.DARK // Dark is indeed a dark theme
                // Check if we are using system default theme and it is currently set to dark
                || (themeId == AppTheme.DEFAULT && mode == Configuration.UI_MODE_NIGHT_YES)
    }

    /**
     *
     */
    protected fun applyAccent() {
        accentStyle(accentId)?.let { setTheme(it) }
    }

}
