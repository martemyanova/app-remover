package com.vs_unusedappremover.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.database.Cursor
import android.database.DataSetObserver
import android.database.sqlite.SQLiteDatabase
import android.util.Log

import com.google.common.base.Function
import com.google.common.base.Predicate
import com.google.common.collect.Collections2
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.MyApplication
import com.vs_unusedappremover.PackageSize
import com.vs_unusedappremover.data.DatabaseHelper.AppTable

import java.io.File
import java.text.DateFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.Date
import java.util.HashMap
import java.util.HashSet

import java.util.Collections.singletonList

class ApplicationCollection(private val context: Context) {

    private var previousData = HashMap<String, AppEntry>()
    private val observers = HashSet<DataSetObserver>()
    private val groupedChangeNotifications = GroupChangeNotifications(NOTIFY_UPDATED_INTERVAL)
    private val app = MyApplication.instance
    private val modificationLock = Any()

    init {
        notifyChanged()
    }

    @JvmOverloads
    fun values(filter: Predicate<AppEntry>, order: Comparator<AppEntry>? = null): ArrayList<AppEntry> {
        synchronized(modificationLock) {
            val result = Lists.newArrayList(Collections2.filter(data.values, filter))
            if (order != null) {
                Collections.sort(result, order)
            }
            return result
        }
    }

    fun notifyUsed(appPackage: String, `when`: Long, ranIn: AppEntry.RanIn) {
        val now = System.currentTimeMillis()
        synchronized(modificationLock) {
            val entry = data[appPackage]
            if (entry != null && entry.lastUsedTime < `when`) {
                entry.lastUsedTime = `when`
                entry.ranIn = ranIn
                groupedChangeNotifications.add(entry)
                Log.i(TAG, appPackage + " used " + (now - `when`) / 1000 + "sec ago")
            }
        }
    }

    fun setNotifyAbout(appPackage: String, value: Boolean) {
        synchronized(modificationLock) {
            val entry = data[appPackage]
            if (entry != null && entry.notifyAbout != value) {
                entry.notifyAbout = value
                saveToCache(listOf<AppEntry>(entry))
                fireChanged()
            }
        }
    }

    fun addObserver(o: DataSetObserver?) {
        if (o != null) {
            observers.add(o)
        }
    }

    fun removeObserver(o: DataSetObserver?) {
        if (o != null) {
            observers.remove(o)
        }
    }

    fun notifyChanged() {
        val pm = context.packageManager

        synchronized(modificationLock) {

            val data = HashMap<String, AppEntry>()
            for (appInfo in pm.getInstalledApplications(0)) {
                if (Applications.isThirdParty(appInfo)) {

                    var entry: AppEntry? = previousData[appInfo.packageName]
                    if (entry == null) {
                        entry = AppEntry()
                        entry.info = appInfo
                        entry.size = PackageSize.UNKNOWN
                        entry.ranIn = AppEntry.RanIn.UNKNOWN
                        entry.label = appInfo.packageName
                        fillFromDbCache(entry)
                    }
                    data.put(appInfo.packageName, entry)
                    app!!.backgroundExecutor.execute(LoadExpensiveInfo(entry))
                }
            }
            previousData = data
        }

        fireChanged()
    }

    private val data: HashMap<String, AppEntry>
        get() {
            synchronized(modificationLock) {
                return previousData
            }
        }

    private fun fillFromDbCache(entry: AppEntry) {
        val dbHelper = app!!.dbHelper

        val c = SelectQuery()
                .select(AppTable.TIME_LAST_USED, AppTable.TIME_INSTALLED,
                        AppTable.DOWLOAD_COUNT, AppTable.RATING,
                        AppTable.SIZE, AppTable.NOTIFY, AppTable.RAN_IN)
                .from(AppTable.NAME)
                .where(AppTable.PACKAGE + "=?", entry.info!!.packageName)
                .execute(dbHelper.readableDatabase)

        try {
            if (c.moveToFirst()) {
                entry.lastUsedTime = c.getLong(0)
                entry.ranIn = AppEntry.RanIn.byId(c.getInt(6))
                entry.installTime = c.getLong(1)
                entry.downloadCount = c.getString(2)
                entry.rating = c.getFloat(3)
                entry.size = c.getLong(4)
                entry.notifyAbout = c.getInt(5) != 0
            }
        } finally {
            c.close()
        }
    }

    private fun saveToCache(entries: Iterable<AppEntry>) {
        val existingApps = existingApps

        val db = app!!.dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues()

            for (entry in entries) {

                values.put(AppTable.TIME_INSTALLED, entry.installTime)
                values.put(AppTable.TIME_LAST_USED, entry.lastUsedTime)
                values.put(AppTable.DOWLOAD_COUNT, entry.downloadCount)
                values.put(AppTable.RATING, entry.rating)
                values.put(AppTable.SIZE, entry.size)
                values.put(AppTable.NOTIFY, entry.notifyAbout)
                values.put(AppTable.RAN_IN, entry.ranIn!!.id)

                if (existingApps.contains(entry.info!!.packageName)) {
                    db.update(AppTable.NAME, values, AppTable.PACKAGE + "=?", arrayOf(entry.info!!.packageName))
                } else {
                    values.put(AppTable.PACKAGE, entry.info!!.packageName)
                    db.insertWithOnConflict(AppTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                values.clear()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        val time = DateFormat.getTimeInstance(DateFormat.SHORT)
        Log.i(TAG, " cached " + Iterables.toString(Iterables.transform(entries) { e -> e!!.info!!.packageName + "=(" + time.format(Date(e.lastUsedTime)) + ")" }))
    }

    private val existingApps: Set<String>
        get() {
            val db = app!!.dbHelper.readableDatabase
            val c = SelectQuery()
                    .select(AppTable.PACKAGE)
                    .from(AppTable.NAME)
                    .execute(db)

            val packages = HashSet<String>(c.columnCount)
            try {
                while (c.moveToNext()) {
                    packages.add(c.getString(0))
                }
                return packages
            } finally {
                c.close()
            }
        }

    private fun fireChanged() {
        for (o in observers) {
            o.onChanged()
        }
    }

    private inner class LoadExpensiveInfo(private val entryToFill: AppEntry) : Runnable {
        private val pm = context.packageManager

        override fun run() {
            val apk = File(entryToFill.info!!.sourceDir)
            val size = PackageSize.get(pm, entryToFill.info)
            val packageName = entryToFill.info!!.packageName
            val label = if (apk.exists()) entryToFill.info!!.loadLabel(pm).toString() else packageName
            val installTime = if (entryToFill.installTime == 0) getFirstInstallTime(packageName) else entryToFill.installTime

            if (size != entryToFill.size ||
                    label != entryToFill.label ||
                    installTime != entryToFill.installTime) {
                synchronized(modificationLock) {
                    entryToFill.size = size
                    entryToFill.label = label
                    entryToFill.installTime = installTime
                }
                groupedChangeNotifications.add(entryToFill)
            }
        }

        private fun getFirstInstallTime(packageName: String): Long {
            try {
                return pm.getPackageInfo(packageName, 0).firstInstallTime
            } catch (e: NameNotFoundException) {
                return 0
            }

        }
    }

    private inner class GroupChangeNotifications(private val updatePeriodMillis: Long) : Runnable {
        private val changes = HashMap<String, AppEntry>()

        @Synchronized
        fun add(entry: AppEntry) {
            val wasEmpty = changes.size == 0
            changes.put(entry.info!!.packageName, entry)
            if (wasEmpty) {
                app!!.uiHandler!!.postDelayed(this, updatePeriodMillis)
            }
        }

        @Synchronized
        fun flush(): Int {
            val changedList = ArrayList(changes.values)
            app!!.backgroundExecutor.execute { saveToCache(changedList) }
            val size = changes.size
            changes.clear()
            return size
        }

        override fun run() {
            val count = flush()
            Log.i(TAG, "flushed $count entries to cache")
            fireChanged()
        }
    }

    companion object {

        private val TAG = ApplicationCollection::class.java!!.getSimpleName()
        private val NOTIFY_UPDATED_INTERVAL: Long = 500
    }
}