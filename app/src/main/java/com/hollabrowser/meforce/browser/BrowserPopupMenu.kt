package com.hollabrowser.meforce.browser

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import androidx.core.view.isVisible
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.adblock.AbpUserRules
import com.hollabrowser.meforce.browser.activity.BrowserActivity
import com.hollabrowser.meforce.database.bookmark.BookmarkRepository
import com.hollabrowser.meforce.databinding.PopupMenuBrowserBinding
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.*
import javax.inject.Inject

class BrowserPopupMenu

(layoutInflater: LayoutInflater, aBinding: PopupMenuBrowserBinding = inflate(layoutInflater)) : PopupWindow(aBinding.root, WRAP_CONTENT, WRAP_CONTENT, true) {

    @Inject internal lateinit var bookmarkModel: BookmarkRepository
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var abpUserRules: AbpUserRules

    var iBinding: PopupMenuBrowserBinding = aBinding
    var iIsIncognito = false

    init {
        aBinding.root.context.injector.inject(this)

        elevation = 100F

        animationStyle = R.style.AnimationMenu

        setBackgroundDrawable(ColorDrawable())

        // Hide incognito menu item if we are already incognito
        iIsIncognito = (aBinding.root.context as BrowserActivity).isIncognito()
        if (iIsIncognito) {
            aBinding.menuItemIncognito.isVisible = false
            // No sessions in incognito mode
            aBinding.menuItemSessions.isVisible = false
        }
    }

    fun onMenuItemClicked(menuView: View, onClick: () -> Unit) {
        menuView.setOnClickListener {
            onClick()
            dismiss()
        }
    }

    fun show(aAnchor: View) {

        (contentView.context as BrowserActivity).tabsManager.let { it ->
            // Set desktop mode checkbox according to current tab
            iBinding.menuItemDesktopMode.isChecked = it.currentTab?.desktopMode ?: false

            // Same with dark mode
            iBinding.menuItemDarkMode.isChecked = it.currentTab?.darkMode ?: false

            // And ad block
            iBinding.menuItemAdBlock.isChecked = it.currentTab?.url?.let { url -> !abpUserRules.isAllowed(Uri.parse(url)) } ?: false

            (contentView.context as BrowserActivity).tabsManager.let { tm ->
                tm.currentTab?.let { tab ->
                    (!(tab.url.isSpecialUrl() || tab.url.isAppScheme())).let {
                        // Those menu items won't be displayed for special URLs
                        iBinding.menuItemAddToHome.isVisible = it
                        iBinding.menuItemShare.isVisible = it
                        iBinding.menuItemPrint.isVisible = it
                        iBinding.menuItemPageTools.isVisible = it
                        iBinding.menuItemFind.isVisible = it
                        iBinding.menuItemTranslate.isVisible = it
                        iBinding.menuItemReaderMode.isVisible = it
                        iBinding.menuItemDesktopMode.isVisible = it
                        iBinding.menuItemDarkMode.isVisible = it
                        iBinding.menuItemAdBlock.isVisible = it && userPreferences.adBlockEnabled
                        iBinding.menuItemAddBookmark.isVisible = it
                        iBinding.menuItemExit.isVisible = userPreferences.menuShowExit || iIsIncognito
                        iBinding.divider2.isVisible = it
                        iBinding.divider3.isVisible = it
                        iBinding.divider4.isVisible = it
                    }
                }
            }

            if (userPreferences.navbar) {
                iBinding.header.visibility = GONE
                iBinding.divider1.visibility = GONE
                iBinding.menuShortcutRefresh.visibility = GONE
                iBinding.menuShortcutHome.visibility = GONE
                iBinding.menuShortcutForward.visibility = GONE
                iBinding.menuShortcutBack.visibility = GONE
                iBinding.menuShortcutBookmarks.visibility = GONE
            }
        }

        // Get our anchor location
        val anchorLoc = IntArray(2)
        aAnchor.getLocationInWindow(anchorLoc)

        // Show our popup menu from the right side of the screen below our anchor
        val gravity = if (userPreferences.toolbarsBottom) Gravity.BOTTOM or Gravity.END else Gravity.TOP or Gravity.END
        val yOffset = if (userPreferences.toolbarsBottom) (contentView.context as BrowserActivity).iBinding.root.height - anchorLoc[1] - aAnchor.height else anchorLoc[1]
        showAtLocation(aAnchor, gravity,
        // Offset from the right screen edge
        Utils.dpToPx(10F),
        // Above our anchor
        yOffset)
    }

    companion object {

        fun inflate(layoutInflater: LayoutInflater): PopupMenuBrowserBinding {
            return PopupMenuBrowserBinding.inflate(layoutInflater)
        }

    }
}
