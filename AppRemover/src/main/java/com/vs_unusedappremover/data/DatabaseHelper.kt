package com.vs_unusedappremover.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

import com.vs_unusedappremover.AppEntry

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    interface AppTable {
        companion object {
            val NAME = "applications"

            val PACKAGE = "package"
            val TIME_LAST_USED = "time_last_used"
            val RAN_IN = "ran_in"
            val TIME_INSTALLED = "time_installed"
            val DOWNLOAD_COUNT = "download_count"
            val RATING = "rating"
            val SIZE = "size"
            val NOTIFY = "notify"

            val CREATE = "create table $NAME (\n" +
                "$PACKAGE text primary key, \n" +
                "$TIME_LAST_USED int, \n" +
                "$TIME_INSTALLED int, \n" +
                "$DOWNLOAD_COUNT text, \n" +
                "$RATING real, \n" +
                "$SIZE int, \n" +
                "$NOTIFY int, \n" +
                "$RAN_IN int); "
        }
    }

    init {
        writableDatabase // to upgrade if needed
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(AppTable.CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            updateInternal(db, oldVersion)
        } catch (e: SQLiteDiskIOException) {
            Log.e(TAG, e.message)
        }

    }

    private fun updateInternal(db: SQLiteDatabase, oldVersion: Int) {
        when (oldVersion) {
            2 -> {
                upgradeFrom2to3(db)
                upgradeFrom3to4(db)
            }
            3 -> upgradeFrom3to4(db)
            else -> {
                db.execSQL("drop table if exists ${AppTable.NAME};")
                onCreate(db)
            }
        }
    }

    private fun upgradeFrom2to3(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE ${AppTable.NAME} ADD COLUMN ${AppTable.NOTIFY} int NOT NULL DEFAULT(1);")
    }

    private fun upgradeFrom3to4(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE ${AppTable.NAME} ADD COLUMN ${AppTable.RAN_IN} int NOT NULL DEFAULT(" + AppEntry.RanIn.UNKNOWN.id + ");")
    }

    companion object {

        private val TAG = DatabaseHelper::class.java.simpleName
        val DATABASE_NAME = "apps"
        val DATABASE_VERSION = 4
    }
}
