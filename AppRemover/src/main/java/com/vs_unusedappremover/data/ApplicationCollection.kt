package com.vs_unusedappremover.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager.NameNotFoundException
import android.database.DataSetObserver
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.MyApplication
import com.vs_unusedappremover.PackageSize
import com.vs_unusedappremover.data.DatabaseHelper.AppTable
import java.io.File
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class ApplicationCollection(private val context: Context) {

    private var previousData = HashMap<String, AppEntry>()
    private val observers = HashSet<DataSetObserver>()
    private val groupedChangeNotifications = GroupChangeNotifications(NOTIFY_UPDATED_INTERVAL)
    private val app = MyApplication.instance
    private val modificationLock = Any()
    private val data: HashMap<String, AppEntry>
        get() {
            synchronized(modificationLock) {
                return previousData
            }
        }

    init {
        notifyChanged()
    }

    @JvmOverloads
    fun values(order: Comparator<AppEntry>? = null, filter: (AppEntry) -> Boolean): ArrayList<AppEntry> {
        synchronized(modificationLock) {
            val result = data.values.filter(filter)
            if (order != null) {
                Collections.sort(result, order)
            }
            return ArrayList(result)
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
                if (appInfo.isThirdParty()) {
                    var entry = previousData[appInfo.packageName]
                    if (entry == null) {
                        entry = AppEntry(info = appInfo,
                                label = appInfo.packageName,
                                size = PackageSize.UNKNOWN,
                                ranIn = AppEntry.RanIn.UNKNOWN)
                        fillFromDbCache(entry)
                    }
                    data.put(entry.label, entry)
                    app.backgroundExecutor.execute(LoadExpensiveInfo(entry))
                }
            }
            previousData = data
        }

        fireChanged()
    }

    private fun ApplicationInfo.isThirdParty(): Boolean {
        if (this.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) return false
        return this.flags and ApplicationInfo.FLAG_SYSTEM == 0
    }

    private fun fillFromDbCache(entry: AppEntry) {
        SelectQuery()
                .select(AppTable.TIME_LAST_USED, AppTable.TIME_INSTALLED,
                        AppTable.DOWNLOAD_COUNT, AppTable.RATING,
                        AppTable.SIZE, AppTable.NOTIFY, AppTable.RAN_IN)
                .from(AppTable.NAME)
                .where(AppTable.PACKAGE + "=?", entry.info.packageName)
                .execute(app.dbHelper.readableDatabase).use {
            if (it.moveToFirst()) {
                entry.apply {
                    lastUsedTime = it.getLong(0)
                    ranIn = AppEntry.RanIn.byId(it.getInt(6))
                    installTime = it.getLong(1)
                    downloadCount = it.getString(2)
                    rating = it.getFloat(3)
                    size = it.getLong(4)
                    notifyAbout = it.getInt(5) != 0
                }
            }
        }
    }

    private fun saveToCache(entries: Iterable<AppEntry>) {
        val existingApps = existingApps

        app.dbHelper.writableDatabase.inTransaction {
            val values = ContentValues()
            for (entry in entries) {
                values.apply {
                    put(AppTable.TIME_INSTALLED, entry.installTime)
                    put(AppTable.TIME_LAST_USED, entry.lastUsedTime)
                    put(AppTable.DOWNLOAD_COUNT, entry.downloadCount)
                    put(AppTable.RATING, entry.rating)
                    put(AppTable.SIZE, entry.size)
                    put(AppTable.NOTIFY, entry.notifyAbout)
                    put(AppTable.RAN_IN, entry.ranIn.id)

                    if (existingApps.contains(entry.info.packageName)) {
                        update(AppTable.NAME, this, AppTable.PACKAGE + "=?", arrayOf(entry.info.packageName))
                    } else {
                        put(AppTable.PACKAGE, entry.info.packageName)
                        insertWithOnConflict(AppTable.NAME, null, this, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                    clear()
                }
            }
        }

        val time = DateFormat.getTimeInstance(DateFormat.SHORT)

        Log.i(TAG, " cached " + entries.joinToString(prefix = "[", postfix = "]") { it ->
            it.info.packageName + "=(" + time.format(Date(it.lastUsedTime)) + ")"
        })
    }

    private inline fun SQLiteDatabase.inTransaction(func: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            func()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }

    private val existingApps: Set<String>
        get() {
            SelectQuery()
                    .select(AppTable.PACKAGE)
                    .from(AppTable.NAME)
                    .execute(app.dbHelper.readableDatabase)
                    .use {
                        val packages = HashSet<String>(it.columnCount)
                        while (it.moveToNext()) {
                            packages.add(it.getString(0))
                        }
                        return packages
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
            val apk = File(entryToFill.info.sourceDir)
            val size = PackageSize.get(pm, entryToFill.info)
            val packageName = entryToFill.info.packageName
            val label = if (apk.exists()) entryToFill.info.loadLabel(pm).toString() else packageName
            val installTime = if (entryToFill.installTime == 0L) getFirstInstallTime(packageName) else entryToFill.installTime

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
            changes.put(entry.info.packageName, entry)
            if (wasEmpty) {
                app.uiHandler.postDelayed(this, updatePeriodMillis)
            }
        }

        @Synchronized
        fun flush(): Int {
            val changedList = ArrayList(changes.values)
            app.backgroundExecutor.execute { saveToCache(changedList) }
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

        private val TAG = ApplicationCollection::class.java.simpleName
        private val NOTIFY_UPDATED_INTERVAL: Long = 500
    }
}
