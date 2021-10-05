package com.hollabrowser.meforce.utils

import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hollabrowser.meforce.BrowserApp
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.database.HistoryEntry
import com.hollabrowser.meforce.dialog.BrowserDialog.setDialogSize
import com.hollabrowser.meforce.extensions.canScrollVertically
import com.hollabrowser.meforce.extensions.snackbar
import com.hollabrowser.meforce.preference.UserPreferences
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.URI
import java.net.URISyntaxException
import java.text.SimpleDateFormat
import java.util.*

object Utils {
    private const val TAG = "Utils"

    lateinit var userPreferences: UserPreferences

    /**
     * Creates a new intent that can launch the email
     * app with a subject, address, body, and cc. It
     * is used to handle mail:to links.
     *
     * @param address the address to send the email to.
     * @param subject the subject of the email.
     * @param body    the body of the email.
     * @param cc      extra addresses to CC.
     * @return a valid intent.
     */
    fun newEmailIntent(address: String, subject: String?,
                       body: String?, cc: String?): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_CC, cc)
        intent.type = "message/rfc822"
        return intent
    }

    /**
     * Workaround reversed layout bug: https://github.com/Slion/Fulguris/issues/212
     */
    fun fixScrollBug(aList : RecyclerView): Boolean {
        val lm = (aList.layoutManager as LinearLayoutManager)
        // Can't change stackFromEnd when computing layout or scrolling otherwise it throws an exception
        if (aList.isComputingLayout) {
            if (userPreferences.toolbarsBottom) {
                // Workaround reversed layout bug: https://github.com/Slion/Fulguris/issues/212
                if (lm.stackFromEnd != aList.canScrollVertically()) {
                    lm.stackFromEnd = !lm.stackFromEnd
                    return true
                }
            } else {
                // Make sure this is set properly when not using bottom toolbars
                // No need to check if the value is already set properly as this is already done internally
                lm.stackFromEnd = false
            }
        }

        return false
    }

    /**
     * Creates a dialog with only a title, message, and okay button.
     *
     * @param activity the activity needed to create a dialog.
     * @param title    the title of the dialog.
     * @param message  the message of the dialog.
     */
    fun createInformativeDialog(activity: AppCompatActivity, @StringRes title: Int, @StringRes message: Int) {
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton(activity.resources.getString(R.string.action_ok)
                ) { _: DialogInterface?, _: Int -> }
        val alert = builder.create()
        alert.show()
        setDialogSize(activity, alert)
    }

    /**
     * Converts Density Pixels (DP) to Pixels (PX).
     *
     * @param dp the number of density pixels to convert.
     * @return the number of pixels that the conversion generates.
     */
    @JvmStatic
    fun dpToPx(dp: Float): Int {
        val metrics = Resources.getSystem().displayMetrics
        return (dp * metrics.density + 0.5f).toInt()
    }

    /**
     * Extracts the domain name from a URL.
     * NOTE: Should be used for display only.
     *
     * @param url the URL to extract the domain from.
     * @return the domain name, or the URL if the domain
     * could not be extracted. The domain name may include
     * HTTPS if the URL is an SSL supported URL.
     */
    fun getDisplayDomainName(url: String?): String {
        var urls = url
        if (url == null || url.isEmpty()) return ""
        val index = url.indexOf('/', 8)
        if (index != -1) {
            urls = url.substring(0, index)
        }
        val uri: URI
        var domain: String?
        try {
            uri = URI(urls)
            domain = uri.host
        } catch (e: URISyntaxException) {
            Log.e(TAG, "Unable to parse URI", e)
            domain = null
        }
        if (domain == null || domain.isEmpty()) {
            return url
        }
        return if (domain.startsWith("www.")) domain.substring(4) else domain
    }

    @JvmStatic
    fun trimCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (ignored: Exception) {
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (aChildren in children) {
                val success = deleteDir(File(dir, aChildren))
                if (!success) {
                    return false
                }
            }
        }
        // The directory is now empty so delete it
        return dir != null && dir.delete()
    }

    fun mixTwoColors(color1: Int, color2: Int, amount: Float): Int {
        val alphachannel: Byte = 24
        val redchannel: Byte = 16
        val greenchannel: Byte = 8
        val inverseAmount = 1.0f - amount
        val r = ((color1 shr redchannel.toInt() and 0xff).toFloat() * amount + (color2 shr redchannel.toInt() and 0xff).toFloat() * inverseAmount).toInt() and 0xff
        val g = ((color1 shr greenchannel.toInt() and 0xff).toFloat() * amount + (color2 shr greenchannel.toInt() and 0xff).toFloat() * inverseAmount).toInt() and 0xff
        val b = ((color1 and 0xff).toFloat() * amount + (color2 and 0xff).toFloat() * inverseAmount).toInt() and 0xff
        return 0xff shl alphachannel.toInt() or (r shl redchannel.toInt()) or (g shl greenchannel.toInt()) or b
    }

    fun buildMiningPage(
        color: String?,
        title: String?,
        error: String?,
        tip3: String?,
        reload: String?,
        showButton: Boolean,
        reloadCode: String = "window.history.back();"
    ): String {
        var reloadButtonCode =
            "<button onclick=\"reload();\" id=\"reload-button\" class=\"blue-button text-button reload\">$reload</button>"
        val background = htmlColor(ThemeUtils.getSurfaceColor(BrowserApp.currentContext()))
        val text =
            htmlColor(ThemeUtils.getColor(BrowserApp.currentContext(), R.attr.colorOnPrimary))
        val accent = htmlColor(ThemeUtils.getColor(BrowserApp.currentContext(), R.attr.colorAccent))

        when (showButton) {
            false -> reloadButtonCode = ""
        }

        return "<html>" +
                "<head>" +
                "<script language=\"javascript\"> " +
                "function reload(){setTimeout(function(){$reloadCode}, 500);" +
                "};</script>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<style>html{-webkit-text-size-adjust: 100%;font-size: 125%;}body{background-color: $background; color: $text; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif, \"Apple Color Emoji\", \"Segoe UI Emoji\", \"Segoe UI Symbol\"; font-size: 75%;}img{pointer-events: none;}.unselectable{-webkit-user-select: none; -webkit-touch-callout: none; -moz-user-select: none; -ms-user-select: none; user-select: none;}div{display:block;}p{color: $text;}h1{margin-top: 0; color: $text; font-size: 1.6em; font-weight: normal; line-height: 1.25em; margin-bottom: 16px;}button{border: solid 1px; border-radius: 4px; border-color: $accent; padding: 0 16px; min-width: 64px; line-height: 34px; background-color: transparent; -webkit-user-select: none; text-transform: uppercase; color: $accent; box-sizing: border-box; cursor: pointer; font-size: .875em; margin: 0; font-weight: 500;}button:hover{box-shadow: 0 1px 2px rgba(1, 1, 1, 0.5);}.error-code{color: $text; display: inline; font-size: .86667em; margin-top: 15px; opacity: .5; text-transform: uppercase;}.interstitial-wrapper{box-sizing: border-box;font-size: 1em;margin: 100px auto 0;max-width: 600px;width: 100%;}.offline .interstitial-wrapper{color: #2b2b2b;font-size: 1em;line-height: 1.55;margin: 0 auto;max-width: 600px;padding-top: 100px;width: 100%;}.hidden{display: none;}.nav-wrapper{margin-top: 51px; display:inline-block;}#buttons::after{clear: both; content: ''; display: block; width: 100%;}.nav-wrapper::after{clear: both; content: ''; display: table; width: 100%;}.small-link{color: #696969; font-size: .875em;}@media (max-width: 640px), (max-height: 640px){h1{margin: 0 0 15px;}button{width: 100%;}}" +
                "</style>" +
                "</head>" +
                "<center>" +
                "<body class=\"offline\">" +
                "<div class=\"interstitial-wrapper\">" +
                "<div id=\"main-content\">" +
                "<img src=\"file:///android_asset/warning.webp\" height=\"128\" width=\"128\"><br><br>" +
                "<div class=\"icon icon-offline\"></div>" +
                "<div id=\"main-message\">" +
                "<h1 class=\"unselectable\">$title</h1>" +
                "<p class=\"unselectable\">$tip3</p>" +
                "</h1><p></p><div class=\"error-code\">$error" +
                "</div></div></div><div id=\"buttons\" class=\"nav-wrapper\"><div id=\"control-buttons\">$reloadButtonCode" +
                "</div></div></div></body></center></html>" +
                color
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val imageFileName = "JPEG_" + timeStamp + '_'
        val storageDir = BrowserApp.instance.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    /**
     * Quietly closes a closeable object like an InputStream or OutputStream without
     * throwing any errors or requiring you do do any checks.
     *
     * @param closeable the object to close
     */
    @JvmStatic
    fun close(closeable: Closeable?) {
        if (closeable == null) {
            return
        }
        try {
            closeable.close()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to close closeable", e)
        }
    }

    /**
     * Creates a shortcut on the homescreen using the
     * [HistoryEntry] information that opens the
     * browser. The icon, URL, and title are used in
     * the creation of the shortcut.
     *
     * @param activity the activity needed to create
     * the intent and show a snackbar message
     * @param historyEntry     the HistoryEntity to create the shortcut from
     */
    @Suppress("DEPRECATION")
    fun createShortcut(activity: AppCompatActivity, historyEntry: HistoryEntry, favicon: Bitmap) {
        val shortcutIntent = Intent(Intent.ACTION_VIEW)
        shortcutIntent.data = Uri.parse(historyEntry.url)
        val title = if (TextUtils.isEmpty(historyEntry.title)) activity.getString(R.string.untitled) else historyEntry.title
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val addIntent = Intent()
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, favicon)
            addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            activity.sendBroadcast(addIntent)
        } else {
            val shortcutManager = activity.getSystemService(ShortcutManager::class.java)
            if (shortcutManager.isRequestPinShortcutSupported) {
                val pinShortcutInfo = ShortcutInfo.Builder(activity, "browser-shortcut-" + historyEntry.url.hashCode())
                        .setIntent(shortcutIntent)
                        .setIcon(Icon.createWithBitmap(favicon))
                        .setShortLabel(title)
                        .build()
                shortcutManager.requestPinShortcut(pinShortcutInfo, null)
            } else {
                activity.snackbar(R.string.shortcut_message_failed_to_add, Gravity.BOTTOM)
            }
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options,
                              reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight
                    && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @JvmStatic
    fun guessFileExtension(filename: String): String? {
        val lastIndex = filename.indexOf('.') + 1
        return if (lastIndex > 0 && filename.length > lastIndex) {
            filename.substring(lastIndex)
        } else null
    }

    /**
     * Construct an intent to display downloads folder either by using a file browser application
     * or using system download manager.
     *
     * @param aContext
     * @param aDownloadFolder
     * @return
     */
    @JvmStatic
    fun getIntentForDownloads(aContext: Context, aDownloadFolder: String?): Intent {
        // This is the solution from there: https://stackoverflow.com/a/26651827/3969362
        // Build an intent to open our download folder in a file explorer app
        val intent = Intent(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.setDataAndType(Uri.parse(aDownloadFolder), "resource/folder")
        // Check that there is an app activity handling that intent on our system
        return if (intent.resolveActivityInfo(aContext.packageManager, 0) != null) {
            // Yes there is one use it
            intent
        } else {
            // Just launch system download manager activity if no custom file explorer found
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    fun adjustBottomSheet() {
        // Get our private class
        val classEdgeToEdgeCallback = Class.forName("com.google.android.material.bottomsheet.BottomSheetDialog\$EdgeToEdgeCallback")
        // Get our private method
        val methodSetPaddingForPosition: Method = classEdgeToEdgeCallback.getDeclaredMethod("setPaddingForPosition", View::class.java)
        methodSetPaddingForPosition.isAccessible = true
        // Get private field containing our EdgeToEdgeCallback instance
        val fieldEdgeToEdgeCallback = BottomSheetDialog::class.java.getDeclaredField("edgeToEdgeCallback")
        fieldEdgeToEdgeCallback.isAccessible = true
        // Get our bottom sheet view field
        val fieldBottomField = BottomSheetDialog::class.java.getDeclaredField("bottomSheet")
        fieldBottomField.isAccessible = true
    }

    fun startActivityForFolder(aContext: Context, aFolder: String?) {
        // This is the solution from there: https://stackoverflow.com/a/26651827/3969362
        // Build an intent to open our download folder in a file explorer app
        val intent =
            Intent(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.setDataAndType(Uri.parse(aFolder), "resource/folder")
        // Check that there is an app activity handling that intent on our system
        if (intent.resolveActivityInfo(aContext.packageManager, 0) != null) {
            // Yes, there is one, use it then
            aContext.startActivity(intent)
        }
    }
}
