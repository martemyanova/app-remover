package com.vs_unusedappremover

import android.app.ActivityManager
import android.app.ActivityManager.RecentTaskInfo
import android.app.ActivityManager.RunningServiceInfo
import android.app.ActivityManager.RunningTaskInfo
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log

import com.google.common.collect.Iterables
import com.vs_unusedappremover.common.HandlerTimer
import com.vs_unusedappremover.common.MillisecondsIn
import com.vs_unusedappremover.data.Applications.Filter

import java.util.ArrayList
import java.util.LinkedList

class MonitorApplicationsService : Service() {

    private var app: MyApplication? = null
    private var prevRecentTasks = LinkedList<String>()
    private var recentAppsTimer: HandlerTimer? = null
    private var runningAppTimer: HandlerTimer? = null

    override fun onCreate() {
        super.onCreate()

        app = MyApplication.instance

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenTurnedOnReceiver, filter)

        val mainThreadHandler = Handler()
        recentAppsTimer = object : HandlerTimer(mainThreadHandler, GET_RECENT_APPS_INTERVAL) {
            override fun onTick() {
                updateRecentUsedApps()
            }
        }

        runningAppTimer = object : HandlerTimer(mainThreadHandler, GET_FOREGROUND_APP_INTERVAL) {
            override fun onTick() {
                updateRunningApps()
            }
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        onScreenStateChanged(pm.isScreenOn)

        Log.i(TAG, "service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenTurnedOnReceiver)
        Log.i(TAG, "service destroyed")
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Is not designed to bind to")
    }

    private fun onScreenStateChanged(isScreenOn: Boolean) {
        //recentAppsTimer.setEnabled(isScreenOn);
        runningAppTimer!!.setEnabled(isScreenOn)
    }

    private val recentTasks: LinkedList<String>
        get() {
            val activityManager = app!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val recentTasks = LinkedList<String>()

            for (task in activityManager.getRecentTasks(50, ActivityManager.RECENT_WITH_EXCLUDED)) {
                val component = task.baseIntent.component
                if (component != null) {
                    recentTasks.add(component.packageName)
                }
            }
            return recentTasks
        }

    private fun selectRecentUsed(recentTasks: LinkedList<String>): List<String> {

        val used = ArrayList<String>()
        for (recent in recentTasks) {
            if (recent != prevRecentTasks.peek()) {
                used.add(recent)
            }
            prevRecentTasks.remove(recent)
        }
        prevRecentTasks = recentTasks
        return used
    }

    private fun showNotificationIfNeeded() {
        if (UnusedAppsNotification.needToShow(this)) {
            val unused = app!!.applications!!.values(Filter.UNUSED.create())
            if (unused.size > 0) {
                var packageSizes: Long = 0
                for (app in unused) {
                    packageSizes += app.size
                }
                UnusedAppsNotification.show(this, unused.size, packageSizes)
            }
        }
    }

    private fun updateRecentUsedApps() {
        val recentUsed = selectRecentUsed(recentTasks)
        Log.i(TAG, "resent used: " + Iterables.toString(recentUsed))
        val time = System.currentTimeMillis()
        for (appPackage in recentUsed) {
            app!!.applications!!.notifyUsed(appPackage, time, AppEntry.RanIn.FOREGROUND)
        }
    }

    private fun updateRunningApps() {
        app!!.backgroundExecutor.execute {
            updateForegroundApps()
            updateRunningServices()
        }
    }

    private fun updateForegroundApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val m = app!!.getSystemService("usagestats") as UsageStatsManager
            val usageStats = m.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis())
            if (usageStats != null) {
                for (s in usageStats) {
                    app!!.applications!!.notifyUsed(s.packageName, s.lastTimeUsed, AppEntry.RanIn.FOREGROUND)
                }
            }
        } else {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (info in am.getRunningTasks(100)) {
                if (info.topActivity != null) {
                    val taskName = info.topActivity.packageName
                    app!!.applications!!.notifyUsed(taskName, System.currentTimeMillis(), AppEntry.RanIn.FOREGROUND)
                    break
                }
            }
        }
    }

    private fun updateRunningServices() {
        val am = app!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val infos = am.getRunningServices(500)
        for (info in infos) {

            val appPackage = getPackageFromProcess(info.process)
            val time = fromSysTimeToClock(info.lastActivityTime)
            app!!.applications!!.notifyUsed(appPackage, time, AppEntry.RanIn.BACKGROUND)
        }
    }

    private fun getPackageFromProcess(process: String): String {
        var appPackage = process
        val index = appPackage.indexOf(':')
        if (index > 0) {
            appPackage = appPackage.substring(0, index)
        }
        return appPackage
    }

    private fun fromSysTimeToClock(system: Long): Long {
        return System.currentTimeMillis() - SystemClock.uptimeMillis() + system
    }

    private val screenTurnedOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "screenOnOffReceiver: " + intent.action)

            val action = intent.action
            val isScreenOn = Intent.ACTION_SCREEN_ON == action
            onScreenStateChanged(isScreenOn)
            showNotificationIfNeeded()
        }
    }

    companion object {

        private val TAG = MonitorApplicationsService::class.java!!.getSimpleName()
        private val GET_FOREGROUND_APP_INTERVAL = 5 * MillisecondsIn.SECOND
        private val GET_RECENT_APPS_INTERVAL = 30 * MillisecondsIn.SECOND
    }
}
