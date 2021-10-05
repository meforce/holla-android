package com.hollabrowser.meforce.settings.fragment

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.bookmark.LegacyBookmarkImporter
import com.hollabrowser.meforce.bookmark.NetscapeBookmarkFormatImporter
import com.hollabrowser.meforce.database.bookmark.BookmarkExporter
import com.hollabrowser.meforce.database.bookmark.BookmarkRepository
import com.hollabrowser.meforce.di.DatabaseScheduler
import com.hollabrowser.meforce.di.MainScheduler
import com.hollabrowser.meforce.di.injector
import com.hollabrowser.meforce.dialog.BrowserDialog
import com.hollabrowser.meforce.dialog.DialogItem
import com.hollabrowser.meforce.extensions.fileName
import com.hollabrowser.meforce.extensions.snackbar
import com.hollabrowser.meforce.log.Logger
import com.hollabrowser.meforce.settings.activity.SettingsActivity
import com.hollabrowser.meforce.utils.Utils
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class ImportExportSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var bookmarkRepository: BookmarkRepository
    @Inject internal lateinit var application: Application
    @Inject internal lateinit var netscapeBookmarkFormatImporter: NetscapeBookmarkFormatImporter
    @Inject internal lateinit var legacyBookmarkImporter: LegacyBookmarkImporter
    @Inject @field:DatabaseScheduler internal lateinit var databaseScheduler: Scheduler
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject internal lateinit var logger: Logger

    private var importSubscription: Disposable? = null
    private var exportSubscription: Disposable? = null
    private var bookmarksSortSubscription: Disposable? = null

    override fun providePreferencesXmlResource() = R.xml.preference_import

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(this)

        clickablePreference(preference = SETTINGS_EXPORT, onClick = this::exportBookmarks)
        clickablePreference(preference = SETTINGS_IMPORT, onClick = this::importBookmarks)
        clickablePreference(preference = SETTINGS_DELETE_BOOKMARKS, onClick = this::deleteAllBookmarks)
        clickablePreference(preference = SETTINGS_SETTINGS_EXPORT, onClick = this::requestSettingsExport)
        clickablePreference(preference = SETTINGS_SETTINGS_IMPORT, onClick = this::requestSettingsImport)
        clickablePreference(preference = SETTINGS_DELETE_SETTINGS, onClick = this::clearSettings)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        exportSubscription?.dispose()
        importSubscription?.dispose()
        bookmarksSortSubscription?.dispose()
    }

    override fun onDestroy() {
        super.onDestroy()

        exportSubscription?.dispose()
        importSubscription?.dispose()
        bookmarksSortSubscription?.dispose()
    }

    @Suppress("DEPRECATION")
    private fun requestSettingsImport() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, IMPORT_SETTINGS)
    }

    @Suppress("DEPRECATION")
    private fun requestSettingsExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            var timeStamp = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dateFormat = SimpleDateFormat("-yyyy-MM-dd-(HH:mm)", Locale.US)
                timeStamp = dateFormat.format(Date())
            }
            // That is a neat feature as it guarantee no file will be overwritten.
            putExtra(Intent.EXTRA_TITLE, "StyxSettings$timeStamp.txt")
        }
        startActivityForResult(intent, EXPORT_SETTINGS)
    }

    private fun clearSettings() {
        val builder = MaterialAlertDialogBuilder(activity as Activity)
        builder.setTitle(getString(R.string.action_delete))
        builder.setMessage(getString(R.string.clean_settings))


        builder.setPositiveButton(resources.getString(R.string.action_ok)){ _, _ ->
            (activity as AppCompatActivity).snackbar(R.string.settings_reseted)

            Handler(Looper.getMainLooper()).postDelayed({
                (activity?.getSystemService(ACTIVITY_SERVICE) as ActivityManager)
                    .clearApplicationUserData()
            }, 500)
        }
        builder.setNegativeButton(resources.getString(R.string.action_cancel)){ _, _ ->

        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(true)
        alertDialog.show()
    }

    private fun exportSettings(uri: Uri) {
        val userPref = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
        val allEntries: Map<String, *> = userPref!!.all
        var string = "{"
        for (entry in allEntries.entries) {
            string += "\"${entry.key}\"=\"${entry.value}\","
        }

        string = string.substring(0, string.length - 1) + "}"

        try {
            val output: OutputStream? = requireActivity().contentResolver.openOutputStream(uri)

            output?.write(string.toByteArray())
            output?.flush()
            output?.close()
            activity?.snackbar("${getString(R.string.settings_exported)} ${uri.fileName}")
        } catch (e: IOException) {
            activity?.snackbar(R.string.settings_export_failure)
        }
    }

    private fun importBookmarks() {
        showImportBookmarksDialog()
    }

    private fun exportBookmarks() {
        showExportBookmarksDialog()
    }

    /**
     * Start bookmarks export workflow by showing file creation dialog.
     */
    private fun showExportBookmarksDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            var timeStamp = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dateFormat = SimpleDateFormat("-yyyy-MM-dd-(HH:mm)", Locale.US)
                timeStamp = dateFormat.format(Date())
            }
            // That is a neat feature as it guarantee no file will be overwritten.
            putExtra(Intent.EXTRA_TITLE, "StyxBookmarks$timeStamp.txt")
        }
        bookmarkExportFilePicker.launch(intent)
    }

    val bookmarkExportFilePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {

            // Using content resolver to get an input stream from selected URI
            result.data?.data?.let{ uri ->
                context?.contentResolver?.openOutputStream(uri)?.let { outputStream ->
                    //val mimeType = context?.contentResolver?.getType(uri)

                    bookmarksSortSubscription = bookmarkRepository.getAllBookmarksSorted()
                        .subscribeOn(databaseScheduler)
                        .subscribe { list ->
                            if (!isAdded) {
                                return@subscribe
                            }

                            exportSubscription?.dispose()
                            exportSubscription = BookmarkExporter.exportBookmarksToFile(list, outputStream)
                                .subscribeOn(databaseScheduler)
                                .observeOn(mainScheduler)
                                .subscribeBy(
                                    onComplete = {
                                        activity?.apply {
                                            snackbar("${getString(R.string.bookmark_export_path)} ${uri.fileName}")
                                        }
                                    },
                                    onError = { throwable ->
                                        logger.log(TAG, "onError: exporting bookmarks", throwable)
                                        val activity = activity
                                        if (activity != null && !activity.isFinishing && isAdded) {
                                            Utils.createInformativeDialog(activity as AppCompatActivity, R.string.title_error, R.string.bookmark_export_failure)
                                        } else {
                                            (activity as AppCompatActivity).snackbar(R.string.bookmark_export_failure)
                                        }
                                    }
                                )
                        }
                }
            }
        }
    }

    private fun showImportBookmarksDialog() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // That's needed for some reason, crashes otherwise
            putExtra(
                // List all file types you want the user to be able to select
                Intent.EXTRA_MIME_TYPES, arrayOf(
                    "text/html", // .html
                    "text/plain" // .txt
                )
            )
        }
        bookmarkImportFilePicker.launch(intent)
    }

    val bookmarkImportFilePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Using content resolver to get an input stream from selected URI
            result.data?.data?.let{ uri ->
                context?.contentResolver?.openInputStream(uri).let { inputStream ->
                    val mimeType = context?.contentResolver?.getType(uri)
                    importSubscription?.dispose()
                    importSubscription = Single.just(inputStream)
                        .map {
                            if (mimeType == "text/html") {
                                netscapeBookmarkFormatImporter.importBookmarks(it)
                            } else {
                                legacyBookmarkImporter.importBookmarks(it)
                            }
                        }
                        .flatMap {
                            bookmarkRepository.addBookmarkList(it).andThen(Single.just(it.size))
                        }
                        .subscribeOn(databaseScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onSuccess = { count ->
                                activity?.apply {
                                    snackbar("$count ${getString(R.string.message_import)}")
                                    // Tell browser activity bookmarks have changed
                                    (activity as SettingsActivity).userPreferences.bookmarksChanged = true
                                }
                            },
                            onError = {
                                logger.log(TAG, "onError: importing bookmarks", it)
                                val activity = activity
                                if (activity != null && !activity.isFinishing && isAdded) {
                                    Utils.createInformativeDialog(activity as AppCompatActivity, R.string.title_error, R.string.import_bookmark_error)
                                } else {
                                    (activity as AppCompatActivity).snackbar(R.string.import_bookmark_error)
                                }
                            }
                        )
                }
            }
        }
    }

    private fun importSettings(uri: Uri) {
        val input: InputStream? = requireActivity().contentResolver.openInputStream(uri)

        val bufferSize = 1024
        val buffer = CharArray(bufferSize)
        val out = StringBuilder()
        val `in`: Reader = InputStreamReader(input, "UTF-8")
        while (true) {
            val rsz = `in`.read(buffer, 0, buffer.size)
            if (rsz < 0) break
            out.append(buffer, 0, rsz)
        }

        val content = out.toString()

        val answer = JSONObject(content)
        val keys: JSONArray? = answer.names()
        val userPref = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)
        for (i in 0 until keys!!.length()) {
            val key: String = keys.getString(i)
            val value: String = answer.getString(key)
            with (userPref.edit()) {
                if(value.matches("-?\\d+".toRegex())){
                    putInt(key, value.toInt())
                }
                else if(value == "true" || value == "false"){
                    putBoolean(key, value.toBoolean())
                }
                else{
                    putString(key, value)
                }
                apply()
            }
        }
        activity?.snackbar(R.string.settings_reseted)
    }

    private fun deleteAllBookmarks() {
        showDeleteBookmarksDialog()
    }

    private fun showDeleteBookmarksDialog() {
        activity?.let {
            BrowserDialog.showPositiveNegativeDialog(
                activity = it as AppCompatActivity,
                title = R.string.action_delete,
                message = R.string.delete_all_bookmarks,
                positiveButton = DialogItem(title = R.string.yes) {
                    bookmarkRepository
                        .deleteAllBookmarks()
                        .subscribeOn(databaseScheduler)
                        .subscribe()
                    (activity as AppCompatActivity).snackbar(R.string.bookmark_restore)
                },
                negativeButton = DialogItem(title = R.string.no) {},
                onCancel = {}
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri: Uri? = data?.data
        if(requestCode == EXPORT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if(uri != null){
                exportSettings(uri)
            }
        }
        else if(requestCode == IMPORT_SETTINGS && resultCode == Activity.RESULT_OK) {
            if(uri != null){
                importSettings(uri)
            }
        }
    }

    companion object {

        private const val TAG = "BookmarkSettingsFrag"

        private const val SETTINGS_EXPORT = "export_bookmark"
        private const val SETTINGS_IMPORT = "import_bookmark"
        private const val SETTINGS_DELETE_BOOKMARKS = "delete_bookmarks"
        private const val SETTINGS_SETTINGS_EXPORT = "export_settings"
        private const val SETTINGS_SETTINGS_IMPORT = "import_settings"
        private const val SETTINGS_DELETE_SETTINGS = "clear_settings"

        const val EXPORT_SETTINGS = 0
        const val IMPORT_SETTINGS = 1

    }
}