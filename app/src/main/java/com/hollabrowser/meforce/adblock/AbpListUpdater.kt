/*
 * Copyright (C) 2017-2021 Hazuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hollabrowser.meforce.adblock

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.hollabrowser.meforce.adblock.filter.abp.*
import com.hollabrowser.meforce.adblock.filter.unified.FILTER_DIR
import com.hollabrowser.meforce.adblock.filter.unified.UnifiedFilter
import com.hollabrowser.meforce.adblock.filter.unified.element.ElementFilter
import com.hollabrowser.meforce.adblock.filter.unified.io.ElementWriter
import com.hollabrowser.meforce.adblock.filter.unified.io.FilterWriter
import com.hollabrowser.meforce.adblock.repository.abp.AbpDao
import com.hollabrowser.meforce.adblock.repository.abp.AbpEntity
import com.hollabrowser.meforce.adblock.util.hash.computeMD5
import com.hollabrowser.meforce.preference.UserPreferences
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject

// this is a slightly modified part of jp.hazuki.yuzubrowser.adblock.service/AbpUpdateService.kt
@Suppress("BlockingMethodInNonBlockingContext")
class AbpListUpdater @Inject constructor(val context: Context) {

    //@Inject internal lateinit var okHttpClient: OkHttpClient
    val okHttpClient = OkHttpClient() // any problems if not injecting?

    @Inject internal lateinit var userPreferences: UserPreferences

    val abpDao = AbpDao(context)

    fun updateAll(forceUpdate: Boolean): Boolean {
        var result = false
        runBlocking {

            var nextUpdateTime = Long.MAX_VALUE
            val now = System.currentTimeMillis()
            abpDao.getAll().forEach {
                if (forceUpdate || (it.isNeedUpdate() && it.enabled)) {
                    val localResult = updateInternal(it, forceUpdate)
                    if (localResult && it.expires > 0) {
                        val nextTime = it.expires * AN_HOUR + now
                        if (nextTime < nextUpdateTime) nextUpdateTime = nextTime
                    }
                    result = result or localResult
                }
            }
        }
        return result
    }

    fun removeFiles(entity: AbpEntity) {
        val dir = getFilterDir()
        val writer = FilterWriter()
        writer.write(dir.getAbpBlackListFile(entity), listOf())
        writer.write(dir.getAbpWhiteListFile(entity), listOf())
        writer.write(dir.getAbpWhitePageListFile(entity), listOf())

        val elementWriter = ElementWriter()
        elementWriter.write(dir.getAbpElementListFile(entity), listOf())
    }

    fun updateAbpEntity(entity: AbpEntity) = runBlocking {
        updateInternal(entity)
    }

    private fun updateInternal(entity: AbpEntity, forceUpdate: Boolean = false): Boolean {
        return when {
            entity.url == "styx://easylist" -> updateAssets(entity)
            entity.url.startsWith("http") -> updateHttp(entity, forceUpdate)
            entity.url.startsWith("file") -> updateFile(entity)
            else -> false
        }
    }

    private fun getFilterDir() = context.getDir(FILTER_DIR, Context.MODE_PRIVATE)

    private fun updateHttp(entity: AbpEntity, forceUpdate: Boolean): Boolean {
        // don't update if auto-update settings don't allow
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (!forceUpdate
            && ((userPreferences.blockListAutoUpdate == AbpUpdateMode.WIFI_ONLY && cm.isActiveNetworkMetered)
                    || userPreferences.blockListAutoUpdate == AbpUpdateMode.NONE))
            return false

        val request = try {
            Request.Builder()
                .url(entity.url)
                .get()
        } catch (e: IllegalArgumentException) {
            return false
        }

        if (!forceUpdate) {
            entity.lastModified?.let {
                val dir = getFilterDir()

                if (dir.getAbpBlackListFile(entity).exists() ||
                    dir.getAbpWhiteListFile(entity).exists() ||
                    dir.getAbpWhitePageListFile(entity).exists())
                    request.addHeader("If-Modified-Since", it)
            }
        }

        val call = okHttpClient.newCall(request.build())
        try {
            val response = call.execute()

            if (response.code == 304) {
                entity.lastLocalUpdate = System.currentTimeMillis()
                abpDao.update(entity)
                return false
            }
            response.body?.run {
                val charset = contentType()?.charset() ?: Charsets.UTF_8
                source().inputStream().bufferedReader(charset).use { reader ->
                    if (decode(reader, charset, entity)) {
                        entity.lastLocalUpdate = System.currentTimeMillis()
                        entity.lastModified = response.header("Last-Modified")
                        abpDao.update(entity)
                        return true
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    private fun updateFile(entity: AbpEntity): Boolean {
        val path = Uri.parse(entity.url).path ?: return false
        val file = File(path)
        if (file.lastModified() < entity.lastLocalUpdate) return false

        try {
            file.inputStream().bufferedReader().use { reader ->
                return decode(reader, Charsets.UTF_8, entity)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    private fun updateAssets(entity: AbpEntity): Boolean {
        val dir = getFilterDir()

        // changed to not update if any file exists, as the list does not need to have all kinds of filters
        if (dir.getAbpBlackListFile(entity).exists() ||
            dir.getAbpWhiteListFile(entity).exists() ||
            dir.getAbpWhitePageListFile(entity).exists()) return false

        // lastModified is only used for HTTP and file
        // can't get file date for assets, so assume that size changes when blocklist changes
        // and (ab)use lastModified to store checksum, so update is triggered when file is changed
        // TODO: maybe only check if app version changed
        val checksum = context.assets.open(ASSETS_BLOCKLIST).computeMD5()
        if (checksum == entity.lastModified)
            return false

        entity.lastModified = checksum // checksum set now, but written to entity only at the end of decode -> should be safe
        context.assets.open(ASSETS_BLOCKLIST).bufferedReader().use {
            return decode(it, Charsets.UTF_8, entity)
        }
    }

    private fun decode(reader: BufferedReader, charset: Charset, entity: AbpEntity): Boolean {
        val decoder = AbpFilterDecoder()
        if (!decoder.checkHeader(reader, charset)) return false

        val set = decoder.decode(reader, entity.url)

        val info = set.filterInfo
        if (entity.title == null) // only update title if there is none
            entity.title = info.title
        entity.expires = info.expires ?: -1
        entity.homePage = info.homePage
        entity.version = info.version
        entity.lastUpdate = info.lastUpdate
        entity.lastLocalUpdate = System.currentTimeMillis()
        val dir = getFilterDir()

        val writer = FilterWriter()
        writer.write(dir.getAbpBlackListFile(entity), set.blackList)
        writer.write(dir.getAbpWhiteListFile(entity), set.whiteList)
        writer.write(dir.getAbpWhitePageListFile(entity), set.elementDisableFilter)

        val elementWriter = ElementWriter()
        elementWriter.write(dir.getAbpElementListFile(entity), set.elementList)

        abpDao.update(entity)
        return true
    }

    private fun FilterWriter.write(file: File, list: List<UnifiedFilter>) {
        if (list.isNotEmpty()) {
            try {
                file.outputStream().buffered().use {
                    write(it, list)
                }
            } catch (e: IOException) {
            }
        } else {
            if (file.exists()) file.delete()
        }
    }

    private fun ElementWriter.write(file: File, list: List<ElementFilter>) {
        if (list.isNotEmpty()) {
            try {
                file.outputStream().buffered().use {
                    write(it, list)
                }
            } catch (e: IOException) {
            }
        } else {
            if (file.exists()) file.delete()
        }
    }

    companion object {
        private const val AN_HOUR = 60 * 60 * 1000
        const val ASSETS_BLOCKLIST = "easylist.txt"
    }

}
