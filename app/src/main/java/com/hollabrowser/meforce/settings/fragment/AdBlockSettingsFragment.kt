package com.hollabrowser.meforce.settings.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hollabrowser.meforce.R
import com.hollabrowser.meforce.adblock.AbpBlocker
import com.hollabrowser.meforce.adblock.AbpListUpdater
import com.hollabrowser.meforce.adblock.AbpUpdateMode
import com.hollabrowser.meforce.adblock.BloomFilterAdBlocker
import com.hollabrowser.meforce.adblock.repository.abp.AbpDao
import com.hollabrowser.meforce.adblock.repository.abp.AbpEntity
import com.hollabrowser.meforce.adblock.source.HostsSourceType
import com.hollabrowser.meforce.adblock.source.selectedHostsSource
import com.hollabrowser.meforce.adblock.source.toPreferenceIndex
import com.hollabrowser.meforce.constant.Schemes
import com.hollabrowser.meforce.di.DiskScheduler
import com.hollabrowser.meforce.di.MainScheduler
import com.hollabrowser.meforce.di.injector
//import com.hollabrowser.meforce.dialog.BrowserDialog
//import com.hollabrowser.meforce.dialog.DialogItem
import com.hollabrowser.meforce.extensions.drawable
import com.hollabrowser.meforce.extensions.resizeAndShow
import com.hollabrowser.meforce.extensions.snackbar
import com.hollabrowser.meforce.extensions.withSingleChoiceItems
import com.hollabrowser.meforce.preference.UserPreferences
import io.reactivex.Maybe
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.*
import javax.inject.Inject

/**
 * Settings for the ad block mechanic.
 */
class AdBlockSettingsFragment : AbstractSettingsFragment() {

    @Inject internal lateinit var userPreferences: UserPreferences
    @Inject @field:MainScheduler internal lateinit var mainScheduler: Scheduler
    @Inject @field:DiskScheduler internal lateinit var diskScheduler: Scheduler
    @Inject internal lateinit var bloomFilterAdBlocker: BloomFilterAdBlocker
    @Inject internal lateinit var abpListUpdater: AbpListUpdater
    @Inject internal lateinit var abpBlocker: AbpBlocker

    private var recentSummaryUpdater: SummaryUpdater? = null
    private val compositeDisposable = CompositeDisposable()
    private var forceRefreshHostsPreference: Preference? = null

    private lateinit var abpDao: AbpDao
    private val entitiyPrefs = mutableMapOf<Int, Preference>()

    // if blocklist changed, they need to be reloaded, but this should happen only once
    //  if reloadLists is true, list reload will be launched onDestroy
    private var reloadLists = false

    // updater is launched in background, and lists should not be reloaded while updater is running
    //  int since multiple lists could be updated at the same time
    private var updatesRunning = 0

    override fun providePreferencesXmlResource(): Int = R.xml.preference_ad_block

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        injector.inject(this)

        switchPreference(
                preference = SETTINGS_BLOCKMINING,
                isChecked = userPreferences.blockMiningEnabled,
                onCheckChange = { userPreferences.blockMiningEnabled = it }
        )

        switchPreference(
            preference = "cb_block_ads",
            isChecked = userPreferences.adBlockEnabled,
            onCheckChange = { userPreferences.adBlockEnabled = it }
        )

        if (context != null) {
            abpDao = AbpDao(requireContext())

            clickableDynamicPreference(
                preference = getString(R.string.pref_key_blocklist_auto_update),
                summary = userPreferences.blockListAutoUpdate.toDisplayString(),
                onClick = { summaryUpdater ->
                    MaterialAlertDialogBuilder(activity as AppCompatActivity).apply {
                        setTitle(R.string.ad_block_update_mode)
                        val values = AbpUpdateMode.values().map { Pair(it, it.toDisplayString()) }
                        withSingleChoiceItems(values, userPreferences.blockListAutoUpdate) {
                            userPreferences.blockListAutoUpdate = it
                            summaryUpdater.updateSummary(it.toDisplayString())
                        }
                        setPositiveButton(getString(R.string.action_ok), null)
                        setNeutralButton(getString(R.string.ad_block_update_now)) {_,_ ->
                            updateEntity(null)
                        }
                    }.resizeAndShow()
                }
            )

            // "new list" button
            val newList = Preference(context)
            newList.title = getString(R.string.ad_block_create_blocklist)
            newList.icon = requireContext().drawable(R.drawable.ic_add_oval)
            newList.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                // show only blocklist from url, change to dialog once list from file is working
                showBlockist(AbpEntity(url = ""))
                /*val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setNegativeButton(getString(R.string.ad_block_from_file)) { _,_ -> showBlockist(AbpEntity(url = getString(R.string.ad_block_file))) }
                    .setPositiveButton(getString(R.string.ad_block_from_address)) { _,_ -> showBlockist(AbpEntity(url = "")) }
                    .setNeutralButton(getString(R.string.action_cancel), null)
                    .setTitle(getString(R.string.ad_block_create_blocklist))
                    .create()
                dialog.show()*/
                true
            }
            this.preferenceScreen.addPreference(newList)

            // list of blocklists/entities
            for (entity in abpDao.getAll()) {
                val entityPref = Preference(context)
                entityPref.title = entity.title
                if (!entity.url.startsWith(Schemes.Holla) && entity.lastLocalUpdate > 0)
                    entityPref.summary = getString(R.string.ad_block_last_update, DateFormat.getDateInstance().format(Date(entity.lastLocalUpdate)))
                entityPref.icon = requireContext().drawable(R.drawable.ic_import_export_oval)
                entityPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    showBlockist(entity)
                    true
                }
                entitiyPrefs[entity.entityId] = entityPref
                this.preferenceScreen.addPreference(entitiyPrefs[entity.entityId])
            }
        }
    }

    // update entity and adjust displayed last update time
    private fun updateEntity(abpEntity: AbpEntity?) {
        GlobalScope.launch(Dispatchers.IO) {
            ++updatesRunning
            val updated = if (abpEntity == null) abpListUpdater.updateAll(true) else abpListUpdater.updateAbpEntity(abpEntity)
            if (updated) {
                // remove lists now
                //  when it's done later there might be cases where old joint list is still used
                abpBlocker.removeJointLists()
                reloadLists = true

                // update the "last updated" times
                activity?.runOnUiThread {
                    for (entity in abpDao.getAll())
                        if (!entity.url.startsWith(Schemes.Holla) && entity.lastLocalUpdate > 0)
                            entitiyPrefs[entity.entityId]?.summary = resources.getString(
                                R.string.ad_block_last_update,
                                DateFormat.getDateInstance().format(Date(entity.lastLocalUpdate))
                            )
                }
            }
            --updatesRunning
        }
    }

    private fun showBlockist(entity: AbpEntity) {
        val builder = MaterialAlertDialogBuilder(requireContext())
        var dialog: AlertDialog? = null
        builder.setTitle(getString(R.string.ad_block_edit_blocklist))
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL

        // edit field for blocklist title
        val title = EditText(context)
        title.inputType = InputType.TYPE_CLASS_TEXT
        title.setText(entity.title)
        title.hint = getString(R.string.hint_title)
        title.addTextChangedListener {
            entity.title = it.toString()
            updateButton(dialog?.getButton(AlertDialog.BUTTON_POSITIVE), entity.url, entity.title)
        }
        linearLayout.addView(title)

        // field for choosing file or url
        when {
            entity.url.startsWith(Schemes.Holla) -> {
                val text = TextView(context)
                text.text = getString(R.string.ad_block_internal_list)
                linearLayout.addView(text)
            }
            entity.url.startsWith(getString(R.string.ad_block_file)) -> {
                val updateButton = Button(context)
                updateButton.text = getString(R.string.ad_block_update_list)
                updateButton.setOnClickListener {
                }
                linearLayout.addView(updateButton)
            }
            entity.url.toHttpUrlOrNull() != null || entity.url == "" -> {
                val url = EditText(context)
                url.inputType = InputType.TYPE_TEXT_VARIATION_URI
                url.setText(entity.url)
                url.hint = getString(R.string.ad_block_address)
                url.addTextChangedListener {
                    entity.url = it.toString()
                    updateButton(dialog?.getButton(AlertDialog.BUTTON_POSITIVE), entity.url, entity.title)
                }
                linearLayout.addView(url)
            }
        }

        // enabled switch
        val enabled = SwitchCompat(requireContext())
        enabled.text = getString(R.string.ad_block_enable)
        enabled.isChecked = entity.enabled
        linearLayout.addView(enabled)

        // arbitrary numbers that look ok on my phone -> ok for other phones?
        linearLayout.setPadding(30,10,30,10)
        builder.setView(linearLayout)

        // delete button
        // don't show for internal list or when creating a new entity
        if (entity.entityId != 0 && !entity.url.startsWith(Schemes.Holla)) {
        builder.setNeutralButton(getString(R.string.action_delete)) { _, _ ->
            abpDao.delete(entity)
            dialog?.dismiss()
            preferenceScreen.removePreference(entitiyPrefs[entity.entityId])
            }
        }
        builder.setNegativeButton(getString(R.string.action_cancel), null)
        builder.setPositiveButton(getString(R.string.action_ok)) { _,_ ->

            val wasEnabled = entity.enabled
            entity.enabled = enabled.isChecked

            entity.title = title.text.toString()
            val newId = abpDao.update(entity) // new id if new entity was added, otherwise newId == entity.entityId

            // set new id for newly added list
            if (entity.entityId == 0)
                entity.entityId = newId

            // check for update (necessary to have correct id!)
            if (entity.url.startsWith("http") && enabled.isChecked && !wasEnabled)
                updateEntity(entity)
            else if (enabled.isChecked != wasEnabled)
                reloadLists = true

            if (entitiyPrefs[newId] == null) { // not in entityPrefs if new
                val pref = Preference(context)
                entity.entityId = newId
                pref.title = entity.title
                if (!entity.url.startsWith(Schemes.Holla) && entity.lastLocalUpdate > 0)
                    pref.summary = getString(R.string.ad_block_last_update, DateFormat.getDateInstance().format(Date(entity.lastLocalUpdate)))
                pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    showBlockist(entity)
                    true
                }
                entitiyPrefs[newId] = pref
                preferenceScreen.addPreference(entitiyPrefs[newId])
            } else
                entitiyPrefs[entity.entityId]?.title = entity.title

        }
        dialog = builder.create()
        dialog.show()
        updateButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), entity.url, entity.title)
    }

    // disable ok button if url or title not valid
    private fun updateButton(button: Button?, url: String, title: String?) {
        if (title?.contains("§§") == true || title.isNullOrBlank()) {
            button?.text = resources.getText(R.string.ad_block_invalid_title)
            button?.isEnabled = false
            return
        }
        if ((url.toHttpUrlOrNull() == null || url.contains("§§")) && !url.startsWith(Schemes.Holla) && !url.startsWith("file")) {
            button?.text = resources.getText(R.string.ad_block_invalid_url)
            button?.isEnabled = false
            return
        }
        button?.text = resources.getString(R.string.action_ok)
        button?.isEnabled = true
    }

    private fun AbpUpdateMode.toDisplayString(): String = getString(when (this) {
        AbpUpdateMode.NONE -> R.string.ad_block_update_off
        AbpUpdateMode.WIFI_ONLY -> R.string.ad_block_update_wifi
        AbpUpdateMode.ALWAYS -> R.string.ad_block_update_on
    })

    private fun updateRefreshHostsEnabledStatus() {
        forceRefreshHostsPreference?.isEnabled = isRefreshHostsEnabled()
    }

    private fun isRefreshHostsEnabled() = userPreferences.selectedHostsSource() is HostsSourceType.Remote

    override fun onDestroy() {
        super.onDestroy()
        // reload lists after updates are done
        if (reloadLists || updatesRunning > 0) {
            GlobalScope.launch(Dispatchers.Default) {
                while (updatesRunning > 0)
                    delay(200)
                if (reloadLists)
                    abpBlocker.loadLists()
            }
        }
        compositeDisposable.clear()
    }

    private fun HostsSourceType.toSummary(): String = when (this) {
        HostsSourceType.Default -> getString(R.string.block_source_default)
        is HostsSourceType.Local -> getString(R.string.block_source_local_description, file.path)
        is HostsSourceType.Remote -> getString(R.string.block_source_remote_description, httpUrl)
    }

    //private fun showHostsSourceChooser(summaryUpdater: SummaryUpdater) {
    //    BrowserDialog.showListChoices(
    //            activity as AppCompatActivity,
    //        R.string.block_ad_source,
    //        DialogItem(
    //            title = R.string.block_source_default,
    //            isConditionMet = userPreferences.selectedHostsSource() == HostsSourceType.Default,
    //            onClick = {
    //                userPreferences.hostsSource = HostsSourceType.Default.toPreferenceIndex()
    //                summaryUpdater.updateSummary(userPreferences.selectedHostsSource().toSummary())
    //                updateForNewHostsSource()
    //            }
    //        ),
    //        DialogItem(
    //            title = R.string.block_source_local,
    //            isConditionMet = userPreferences.selectedHostsSource() is HostsSourceType.Local,
    //            onClick = {
    //                showFileChooser(summaryUpdater)
    //            }
    //        ),
    //        DialogItem(
    //            title = R.string.block_source_remote,
    //            isConditionMet = userPreferences.selectedHostsSource() is HostsSourceType.Remote,
    //            onClick = {
    //                showUrlChooser(summaryUpdater)
    //            }
    //        )
    //    )
    //}

    //@Suppress("DEPRECATION")
    //private fun showFileChooser(summaryUpdater: SummaryUpdater) {
    //    this.recentSummaryUpdater = summaryUpdater
    //    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    //        addCategory(Intent.CATEGORY_OPENABLE)
    //        type = TEXT_MIME_TYPE
    //    }

    //    startActivityForResult(intent, FILE_REQUEST_CODE)
    //}

    //private fun showUrlChooser(summaryUpdater: SummaryUpdater) {
    //    BrowserDialog.showEditText(
    //            activity as AppCompatActivity,
    //        title = R.string.block_source_remote,
    //        hint = R.string.hint_url,
    //        currentText = userPreferences.hostsRemoteFile,
    //        action = R.string.action_ok,
    //        textInputListener = {
    //            val url = it.toHttpUrlOrNull()
    //                ?: return@showEditText run { (activity as AppCompatActivity).snackbar(R.string.problem_download) }
    //            userPreferences.hostsSource = HostsSourceType.Remote(url).toPreferenceIndex()
    //            userPreferences.hostsRemoteFile = it
    //            summaryUpdater.updateSummary(it)
    //            updateForNewHostsSource()
    //        }
    //    )
    //}

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                data?.data?.also { uri ->
                    compositeDisposable += readTextFromUri(uri)
                        .subscribeOn(diskScheduler)
                        .observeOn(mainScheduler)
                        .subscribeBy(
                            onComplete = { (activity as AppCompatActivity).snackbar(R.string.action_message_canceled) },
                            onSuccess = { file ->
                                userPreferences.hostsSource = HostsSourceType.Local(file).toPreferenceIndex()
                                userPreferences.hostsLocalFile = file.path
                                recentSummaryUpdater?.updateSummary(userPreferences.selectedHostsSource().toSummary())
                                updateForNewHostsSource()
                            }
                        )
                }
            } else {
                (activity as AppCompatActivity).snackbar(R.string.action_message_canceled)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateForNewHostsSource() {
        bloomFilterAdBlocker.populateAdBlockerFromDataSource(forceRefresh = true)
        updateRefreshHostsEnabledStatus()
    }

    private fun readTextFromUri(uri: Uri): Maybe<File> = Maybe.create {
        val externalFilesDir = activity?.getExternalFilesDir("")
            ?: return@create it.onComplete()
        val inputStream = activity?.contentResolver?.openInputStream(uri)
            ?: return@create it.onComplete()

        try {
            val outputFile = File(externalFilesDir, AD_HOSTS_FILE)

            val input = inputStream.source()
            val output = outputFile.sink().buffer()
            output.writeAll(input)
            return@create it.onSuccess(outputFile)
        } catch (exception: IOException) {
            return@create it.onComplete()
        }
    }

    companion object {
        private const val FILE_REQUEST_CODE = 100
        private const val AD_HOSTS_FILE = "local_hosts.txt"
        //private const val TEXT_MIME_TYPE = "text/*"
        private const val SETTINGS_BLOCKMINING = "block_mining_sites"
    }
}
