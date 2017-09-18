package com.vs_unusedappremover.data

import android.content.pm.ApplicationInfo

import com.google.common.base.Predicate
import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.MyApplication
import com.vs_unusedappremover.common.MillisecondsIn

object Applications {

    val UNUSED_INTERVAL_MILLIS = MillisecondsIn.DAY * 7

    enum class Filter {
        DOWNLOADED {
            override fun create(): Predicate<AppEntry> {
                val myPackage = MyApplication.instance!!.packageName

                return Predicate { entry -> myPackage != entry!!.info!!.packageName }
            }
        },

        UNUSED {
            override fun create(): Predicate<AppEntry> {
                val app = MyApplication.instance
                val thisInstallTime = app!!.installTime
                val myPackage = app.packageName

                return Predicate { entry -> isUnused(entry, thisInstallTime) && myPackage != entry!!.info!!.packageName }
            }
        };

        abstract fun create(): Predicate<AppEntry>
    }

    fun isThirdParty(app: ApplicationInfo): Boolean {
        if (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0) return false
        return if (app.flags and ApplicationInfo.FLAG_SYSTEM != 0) false else true
    }

    private fun isUnused(app: AppEntry?, thisAppInstallDate: Long): Boolean {
        if (!app!!.notifyAbout) return false
        var timeUsed = Math.max(app.lastUsedTime, app.installTime)
        timeUsed = Math.max(timeUsed, thisAppInstallDate)
        return System.currentTimeMillis() - timeUsed > UNUSED_INTERVAL_MILLIS
    }
}
