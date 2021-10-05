package com.hollabrowser.meforce.download

import android.app.Activity
import android.app.DownloadManager
import android.net.Uri
import android.os.Build
import android.view.Gravity
import android.webkit.CookieManager
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.constant.FILE
import com.hollabrowser.meforce.extensions.snackbar
import com.hollabrowser.meforce.preference.UserPreferences
import com.hollabrowser.meforce.utils.FileUtils
import com.hollabrowser.meforce.utils.guessFileName
import java.io.File
import java.io.IOException
import javax.inject.Inject


class DownloadHandler

@Inject
constructor(private var downloadManager: DownloadManager) {
    @Suppress("DEPRECATION")
    fun onDownloadStartNoStream(context: Activity, preferences: UserPreferences, url: String, userAgent: String, contentDisposition: String?, mimetype: String?) {
        val webAddress: WebAddress

        try {
            webAddress = WebAddress(url)
            webAddress.path = encodePath(webAddress.path)
        } catch (e: Exception) {
            if (preferences.toolbarsBottom || preferences.navbar) {
                context.snackbar(R.string.problem_download, Gravity.TOP)
            } else {
                context.snackbar(R.string.problem_download, Gravity.BOTTOM)
            }
            return
        }

        val filename = guessFileName(contentDisposition, null, url, mimetype)
        val addressString = webAddress.toString()
        val uri = Uri.parse(addressString)

        val request: DownloadManager.Request = try {
            DownloadManager.Request(uri)
        } catch (e: IllegalArgumentException) {
            if (preferences.toolbarsBottom || preferences.navbar) {
                context.snackbar(R.string.cannot_download, Gravity.TOP)
            } else {
                context.snackbar(R.string.cannot_download, Gravity.BOTTOM)
            }
            return
        }

        val cookies = CookieManager.getInstance().getCookie(url)
        var location = preferences.downloadDirectory
        val downloadFolder = Uri.parse(location)

        if (!isWriteAccessAvailable(downloadFolder)) {
            if (preferences.toolbarsBottom || preferences.navbar) {
                context.snackbar(R.string.problem_location_download, Gravity.TOP)
            } else {
                context.snackbar(R.string.problem_location_download, Gravity.BOTTOM)
            }
            return
        }

        location = FileUtils.addNecessarySlashes(location)
        request.setDestinationUri(Uri.parse(FILE + location + filename))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            request.setVisibleInDownloadsUi(true)
            request.allowScanningByMediaScanner()
        }
        request.addRequestHeader(COOKIE_REQUEST_HEADER, cookies)
        request.addRequestHeader(REFERER_REQUEST_HEADER, url)
        request.addRequestHeader(USERAGENT_REQUEST_HEADER, userAgent)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        downloadManager.enqueue(request)
        if (preferences.toolbarsBottom || preferences.navbar) {
            context.snackbar(context.getString(R.string.download_pending) + ' ' + filename, Gravity.TOP)
        } else {
            context.snackbar(context.getString(R.string.download_pending) + ' ' + filename, Gravity.BOTTOM)
        }
    }

    companion object {
        private const val COOKIE_REQUEST_HEADER = "Cookie"
        private const val REFERER_REQUEST_HEADER = "Referer"
        private const val USERAGENT_REQUEST_HEADER = "User-Agent"

        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private fun isWriteAccessAvailable(fileUri: Uri): Boolean {
            if (fileUri.path == null) {
                return false
            }
            val file = File(fileUri.path)
            return if (!file.isDirectory && !file.mkdirs()) {
                false
            } else try {
                if (file.createNewFile()) {
                    file.delete()
                }
                true
            } catch (ignored: IOException) {
                false
            }
        }

        private fun encodePath(path: String): String {
            val chars = path.toCharArray()
            var needed = false
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    needed = true
                    break
                }
            }
            if (!needed) {
                return path
            }
            val sb = StringBuilder()
            for (c in chars) {
                if (c == '[' || c == ']' || c == '|') {
                    sb.append('%')
                    sb.append(Integer.toHexString(c.code))
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}
