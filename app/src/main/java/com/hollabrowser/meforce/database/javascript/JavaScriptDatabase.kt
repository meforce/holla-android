package com.hollabrowser.meforce.database.javascript

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.WorkerThread
import com.hollabrowser.meforce.database.databaseDelegate
import com.hollabrowser.meforce.extensions.useMap
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The disk backed download database. See [JavaScriptRepository] for function documentation.
 */
@Singleton
@WorkerThread
class JavaScriptDatabase @Inject constructor(
        application: Application
) : SQLiteOpenHelper(application, DATABASE_NAME, null, DATABASE_VERSION), JavaScriptRepository {

    private val database: SQLiteDatabase by databaseDelegate()

    data class JavaScriptEntry(
            val name: String,
            val namespace: String?,
            val version: String?,
            val author: String?,
            val include: String?,
            val exclude: String?,
            val time: String?,
            val permissions: String?,
            val code: String,
            val requirements: String?
    )


    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val createJavaScriptTable = "CREATE TABLE $TABLE_JAVASCRIPT(" +
                " $KEY_ID INTEGER PRIMARY KEY," +
                " $KEY_NAME TEXT," +
                " $KEY_NAMESPACE TEXT," +
                " $KEY_AUTHOR TEXT," +
                " $KEY_VERSION TEXT," +
                " $KEY_INCLUDE TEXT," +
                " $KEY_EXCLUDE TEXT," +
                " $KEY_TIME TEXT," +
                " $KEY_PERMISSIONS TEXT," +
                " $KEY_CODE TEXT," +
                " $KEY_REQUIREMENTS TEXT" +
                ")"
        db.execSQL(createJavaScriptTable)
    }

    private val databasealter3 = ("ALTER TABLE "
            + TABLE_JAVASCRIPT) + " ADD COLUMN " + KEY_REQUIREMENTS + " string;"

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL(databasealter3)
        }
    }

    override fun deleteJavaScript(): Completable = Completable.fromAction {
        database.run {
            delete(TABLE_JAVASCRIPT, null, null)
            close()
        }
    }

    override fun deleteJavaScriptEntry(name: String): Completable = Completable.fromAction {
        database.delete(TABLE_JAVASCRIPT, "$KEY_NAME = ?", arrayOf(name))
    }


    @SuppressLint("Recycle")
    override fun findJavaScriptEntriesContaining(query: String): Single<List<JavaScriptEntry>> =
            Single.fromCallable {
                val search = "%$query%"

                return@fromCallable database.query(
                        TABLE_JAVASCRIPT,
                        null,
                        "$KEY_NAME LIKE ?",
                        arrayOf(search, search),
                        null,
                        null,
                        "$KEY_ID DESC",
                        "5"
                ).useMap { it.bindToJavaScriptEntry() }
            }

    @SuppressLint("Recycle")
    override fun lastHundredVisitedJavaScriptEntries(): Single<List<JavaScriptEntry>> =
            Single.fromCallable {
                database.query(
                        TABLE_JAVASCRIPT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "$KEY_ID DESC",
                        "100"
                ).useMap { it.bindToJavaScriptEntry() }
            }

    override fun addJavaScriptIfNotExists(entry: JavaScriptEntry): Single<Boolean> = Single.fromCallable {
        database.query(
                TABLE_JAVASCRIPT,
                null,
                "${KEY_NAME}=?",
                arrayOf(entry.name),
                null,
                null,
                "1"
        ).use {
            if (it.moveToFirst()) {
                return@fromCallable false
            }
        }

        val id = database.insert(TABLE_JAVASCRIPT, null, entry.toContentValues())

        return@fromCallable id != -1L
    }

    @SuppressLint("Recycle")
    fun getAllJavaScriptEntries(): List<JavaScriptEntry> {
        return database.query(
                TABLE_JAVASCRIPT,
                null,
                null,
                null,
                null,
                null,
                "$KEY_ID DESC"
        ).useMap { it.bindToJavaScriptEntry() }
    }

    fun getJavaScriptEntriesCount(): Long = DatabaseUtils.queryNumEntries(database, TABLE_JAVASCRIPT)

    private fun JavaScriptEntry.toContentValues() = ContentValues().apply {
        put(KEY_NAME, name)
        put(KEY_NAMESPACE, namespace)
        put(KEY_AUTHOR, author)
        put(KEY_VERSION, version)
        put(KEY_INCLUDE, include)
        put(KEY_EXCLUDE, exclude)
        put(KEY_TIME, time)
        put(KEY_PERMISSIONS, permissions)
        put(KEY_CODE, code)
        put(KEY_REQUIREMENTS, requirements)
    }

    private fun Cursor.bindToJavaScriptEntry() = JavaScriptEntry(
            name = getString(1),
            namespace = getString(2),
            author = getString(3),
            version = getString(4),
            include = getString(5),
            exclude = getString(6),
            time = getString(7),
            permissions = getString(8),
            code = getString(9),
            requirements = getString(10)
    )

    companion object {

        // Database version
        private const val DATABASE_VERSION = 3

        // Database name
        private const val DATABASE_NAME = "javascriptManager"

        // JavaScriptEntry table name
        private const val TABLE_JAVASCRIPT = "javascript"

        // JavaScriptEntry table columns names
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_NAMESPACE = "namespace"
        private const val KEY_AUTHOR = "author"
        private const val KEY_VERSION = "version"
        private const val KEY_INCLUDE = "include"
        private const val KEY_EXCLUDE = "exclude"
        private const val KEY_TIME = "time"
        private const val KEY_PERMISSIONS = "permissions"
        private const val KEY_REQUIREMENTS = "requirements"
        private const val KEY_CODE = "code"

    }
}
