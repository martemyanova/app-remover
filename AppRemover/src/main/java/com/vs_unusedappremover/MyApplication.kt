package com.vs_unusedappremover

import android.app.Application
import android.content.Context
import android.content.Intent
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

    val dbHelper: DatabaseHelper by lazy {
        DatabaseHelper(this)
    }

    val backgroundExecutor: ExecutorService
    lateinit var uiHandler: Handler
        private set
    lateinit var applications: ApplicationCollection
        private set
    lateinit private var picasso: Picasso

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
        if (prefs.getLong(INSTALL_TIME, 0) == 0L) {
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
            return if (installTime != 0L) installTime else now
        } catch (e: NameNotFoundException) {
            return now
        }

    }

    companion object {

        val TAG: String = MyApplication::class.java.simpleName
        private val INSTALL_TIME = "install time"

        lateinit var instance: MyApplication
            private set

        fun startMonitoringService(context: Context) {
            context.startService(Intent(context, MonitorApplicationsService::class.java))
        }
    }
}
