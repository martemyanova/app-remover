package com.vs_unusedappremover.data

import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.MyApplication
import com.vs_unusedappremover.common.MillisecondsIn

object Applications {

    private val UNUSED_INTERVAL_MILLIS = MillisecondsIn.DAY * 7

    enum class Filter(val create: (entry: AppEntry?) -> Boolean) {
        DOWNLOADED({ entry ->
            val myPackage = MyApplication.instance.packageName
            entry != null && myPackage != entry.info.packageName
        }),

        UNUSED({ entry ->
            val myPackage = MyApplication.instance.packageName
            entry != null && entry.isUnused() && myPackage != entry.info.packageName
        });
    }

    private fun AppEntry.isUnused(): Boolean {
        if (!this.notifyAbout) return false

        var timeUsed = Math.max(this.lastUsedTime, this.installTime)
        timeUsed = Math.max(timeUsed, MyApplication.instance.installTime)

        return System.currentTimeMillis() - timeUsed > UNUSED_INTERVAL_MILLIS
    }
}
