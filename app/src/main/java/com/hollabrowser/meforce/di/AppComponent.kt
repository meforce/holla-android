package com.hollabrowser.meforce.di

import android.app.Application
import com.hollabrowser.meforce.BrowserApp
import com.hollabrowser.meforce.ThemedActivity
import com.hollabrowser.meforce.adblock.AbpBlocker
import com.hollabrowser.meforce.adblock.BloomFilterAdBlocker
import com.hollabrowser.meforce.adblock.NoOpAdBlocker
import com.hollabrowser.meforce.browser.BrowserPopupMenu
import com.hollabrowser.meforce.browser.SearchBoxModel
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.browser.activity.ThemedBrowserActivity
import com.hollabrowser.meforce.browser.bookmarks.BookmarksAdapter
import com.hollabrowser.meforce.browser.bookmarks.BookmarksDrawerView
import com.hollabrowser.meforce.browser.sessions.SessionsPopupWindow
import com.hollabrowser.meforce.browser.tabs.TabsDrawerView
import com.hollabrowser.meforce.device.BuildInfo
import com.hollabrowser.meforce.dialog.StyxDialogBuilder
import com.hollabrowser.meforce.download.StyxDownloadListener
import com.hollabrowser.meforce.reading.ReadingActivity
import com.hollabrowser.meforce.search.SuggestionsAdapter
import com.hollabrowser.meforce.settings.activity.SettingsActivity
import com.hollabrowser.meforce.settings.activity.ThemedSettingsActivity
import com.hollabrowser.meforce.settings.fragment.*
import com.hollabrowser.meforce.view.StyxChromeClient
import com.hollabrowser.meforce.view.StyxView
import com.hollabrowser.meforce.view.StyxWebClient
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [(AppModule::class), (AppBindsModule::class)])
interface AppComponent {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun buildInfo(buildInfo: BuildInfo): Builder

        fun build(): AppComponent
    }

    fun inject(activity: BrowserActivity)

    fun inject(fragment: ImportExportSettingsFragment)

    fun inject(builder: StyxDialogBuilder)

    fun inject(styxView: StyxView)

    fun inject(activity: ThemedBrowserActivity)

    fun inject(app: BrowserApp)

    fun inject(activity: ReadingActivity)

    fun inject(webClient: StyxWebClient)

    fun inject(activity: SettingsActivity)

    fun inject(activity: ThemedSettingsActivity)

    fun inject(listener: StyxDownloadListener)

    fun inject(fragment: PrivacySettingsFragment)

    fun inject(fragment: ExtensionsSettingsFragment)

    fun inject(fragment: TabsSettingsFragment)

    fun inject(suggestionsAdapter: SuggestionsAdapter)

    fun inject(chromeClient: StyxChromeClient)

    fun inject(searchBoxModel: SearchBoxModel)

    fun inject(generalSettingsFragment: GeneralSettingsFragment)

    fun inject(displaySettingsFragment: DisplaySettingsFragment)

    fun inject(adBlockSettingsFragment: AdBlockSettingsFragment)

    fun inject(aboutSettingsFragment: AboutSettingsFragment)

    fun inject(bookmarksView: BookmarksDrawerView)

    fun inject(popupMenu: BrowserPopupMenu)

    fun inject(popupMenu: SessionsPopupWindow)

    fun inject(appsSettingsFragment: AppsSettingsFragment)

    fun inject(themedActivity: ThemedActivity)

    fun inject(tabsDrawerView: TabsDrawerView)

    fun inject(bookmarksAdapter: BookmarksAdapter)

    fun provideBloomFilterAdBlocker(): BloomFilterAdBlocker

    fun provideAbpAdBlocker(): AbpBlocker

    fun provideNoOpAdBlocker(): NoOpAdBlocker

}
