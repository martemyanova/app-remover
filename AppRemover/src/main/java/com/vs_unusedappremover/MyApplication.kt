package com.vs_unusedappremover

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Handler

import com.squareup.picasso.Picasso
import com.vs_unusedappremover.common.GA
import com.vs_unusedappremover.data.ApplicationCollection
import com.vs_unusedappremover.data.DatabaseHelper

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MyApplication : Application() {

    private var database: DatabaseHelper? = null
    private val backgroundExecutor: ThreadPoolExecutor
    var uiHandler: Handler? = null
        private set
    var applications: ApplicationCollection? = null
        private set
    private var picasso: Picasso? = null

    init {
        backgroundExecutor = ThreadPoolExecutor(2, 16, 1, TimeUnit.SECONDS, LinkedBlockingQueue())
        backgroundExecutor.rejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        uiHandler = Handler()
        GA.init(null, this)
        applications = ApplicationCollection(this)
        picasso = Picasso.Builder(this)
                .addRequestHandler(AppIcon.createRequestHandler(this))
                .build()
        startMonitoringService(this)
        checkInstallTimePresent()
    }

    val dbHelper: DatabaseHelper
        @Synchronized get() {
            if (database == null) {
                database = DatabaseHelper(this)
            }
            return database
        }

    fun getBackgroundExecutor(): ExecutorService {
        return backgroundExecutor
    }

    val installTime: Long
        get() {
            val prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE)
            return prefs.getLong(INSTALL_TIME, 0)
        }

    fun picasso(): Picasso? {
        return picasso
    }

    private fun checkInstallTimePresent() {
        val prefs = getSharedPreferences(TAG, Context.MODE_PRIVATE)
        if (prefs.getLong(INSTALL_TIME, 0) == 0) {
            prefs.edit()
                    .putLong(INSTALL_TIME, calculateInstallTime())
                    .apply()
        }
    }

    private fun calculateInstallTime(): Long {
        val now = System.currentTimeMillis()
        val pm = packageManager
        try {
            val info = pm.getApplicationInfo(packageName, 0)
            val apk = File(info.sourceDir)
            val installTime = apk.lastModified()
            return if (installTime != 0) installTime else now
        } catch (e: NameNotFoundException) {
            return now
        }

    }

    companion object {

        val TAG = MyApplication::class.java!!.getSimpleName()
        private val INSTALL_TIME = "install time"

        var instance: MyApplication? = null
            private set

        fun startMonitoringService(context: Context) {
            context.startService(Intent(context, MonitorApplicationsService::class.java))
        }
    }
}
