package com.hollabrowser.meforce.di

import com.hollabrowser.meforce.database.adblock.UserRulesDatabase
import com.hollabrowser.meforce.database.adblock.UserRulesRepository
import com.hollabrowser.meforce.adblock.allowlist.AllowListModel
import com.hollabrowser.meforce.adblock.allowlist.SessionAllowListModel
import com.hollabrowser.meforce.adblock.source.AssetsHostsDataSource
import com.hollabrowser.meforce.adblock.source.HostsDataSource
import com.hollabrowser.meforce.adblock.source.HostsDataSourceProvider
import com.hollabrowser.meforce.adblock.source.PreferencesHostsDataSourceProvider
import com.hollabrowser.meforce.browser.cleanup.DelegatingExitCleanup
import com.hollabrowser.meforce.browser.cleanup.ExitCleanup
import com.hollabrowser.meforce.database.adblock.HostsDatabase
import com.hollabrowser.meforce.database.adblock.HostsRepository
import com.hollabrowser.meforce.database.allowlist.AdBlockAllowListDatabase
import com.hollabrowser.meforce.database.allowlist.AdBlockAllowListRepository
import com.hollabrowser.meforce.database.bookmark.BookmarkDatabase
import com.hollabrowser.meforce.database.bookmark.BookmarkRepository
import com.hollabrowser.meforce.database.downloads.DownloadsDatabase
import com.hollabrowser.meforce.database.downloads.DownloadsRepository
import com.hollabrowser.meforce.database.history.HistoryDatabase
import com.hollabrowser.meforce.database.history.HistoryRepository
import com.hollabrowser.meforce.database.javascript.JavaScriptDatabase
import com.hollabrowser.meforce.database.javascript.JavaScriptRepository
import com.hollabrowser.meforce.ssl.SessionSslWarningPreferences
import com.hollabrowser.meforce.ssl.SslWarningPreferences
import dagger.Binds
import dagger.Module

/**
 * Dependency injection module used to bind implementations to interfaces.
 */
@Module
interface AppBindsModule {

    @Binds
    fun bindsExitCleanup(delegatingExitCleanup: DelegatingExitCleanup): ExitCleanup

    @Binds
    fun bindsBookmarkModel(bookmarkDatabase: BookmarkDatabase): BookmarkRepository

    @Binds
    fun bindsDownloadsModel(downloadsDatabase: DownloadsDatabase): DownloadsRepository

    @Binds
    fun bindsHistoryModel(historyDatabase: HistoryDatabase): HistoryRepository

    @Binds
    fun bindsJavaScriptModel(javaScriptDatabase: JavaScriptDatabase): JavaScriptRepository

    @Binds
    fun bindsAdBlockAllowListModel(adBlockAllowListDatabase: AdBlockAllowListDatabase): AdBlockAllowListRepository

    @Binds
    fun bindsAllowListModel(sessionAllowListModel: SessionAllowListModel): AllowListModel

    @Binds
    fun bindsSslWarningPreferences(sessionSslWarningPreferences: SessionSslWarningPreferences): SslWarningPreferences

    @Binds
    fun bindsHostsDataSource(assetsHostsDataSource: AssetsHostsDataSource): HostsDataSource

    @Binds
    fun bindsHostsRepository(hostsDatabase: HostsDatabase): HostsRepository

    @Binds
    fun bindsAbpRulesRepository(apbRulesDatabase: UserRulesDatabase): UserRulesRepository

    @Binds
    fun bindsHostsDataSourceProvider(preferencesHostsDataSourceProvider: PreferencesHostsDataSourceProvider): HostsDataSourceProvider
}
