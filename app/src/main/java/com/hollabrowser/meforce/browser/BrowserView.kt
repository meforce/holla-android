package com.hollabrowser.meforce.browser

import android.view.View
import androidx.annotation.StringRes
import com.hollabrowser.meforce.ssl.SslState

interface BrowserView {

    fun setTabView(view: View)

    fun removeTabView()

    fun updateUrl(url: String?, isLoading: Boolean)

    fun updateProgress(progress: Int)

    fun updateTabNumber(number: Int)

    fun updateSslState(sslState: SslState)

    fun closeBrowser()

    fun closeActivity()

    fun showBlockedLocalFileDialog(onPositiveClick: () -> Unit)

    fun showSnackbar(@StringRes resource: Int)

    fun setForwardButtonEnabled(enabled: Boolean)

    fun setBackButtonEnabled(enabled: Boolean)

    fun notifyTabViewRemoved(position: Int)

    fun notifyTabViewAdded()

    fun notifyTabViewChanged(position: Int)

    fun notifyTabViewInitialized()

}
