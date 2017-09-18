package com.vs_unusedappremover

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageStatsObserver
import android.content.pm.PackageManager
import android.content.pm.PackageStats
import android.os.RemoteException

import com.vs_unusedappremover.common.AsyncResult

object PackageSize {
    val UNKNOWN: Long = -1

    operator fun get(pm: PackageManager, app: ApplicationInfo): Long {
        try {
            val ps = getPackageStats(pm, app)
            return if (ps != null) getFromStats(ps) else UNKNOWN
        } catch (e: InvocationTargetException) {
            return getFromFile(app)
        } catch (e: NoSuchFieldError) {
            return getFromFile(app)
        }

    }

    @SuppressLint("NewApi")
    private fun getFromStats(ps: PackageStats): Long {
        var size = ps.codeSize + ps.dataSize + ps.cacheSize

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            size += ps.externalCodeSize + ps.externalObbSize
            +ps.externalDataSize + ps.externalMediaSize
            +ps.externalCacheSize
        }
        return size
    }

    private fun getFromFile(app: ApplicationInfo): Long {
        try {
            val f = File(app.sourceDir)
            return if (f.exists()) f.length() else UNKNOWN
        } catch (e: SecurityException) {
            return UNKNOWN
        }

    }

    @Throws(InvocationTargetException::class)
    private fun getPackageStats(pm: PackageManager, app: ApplicationInfo): PackageStats? {
        val result = AsyncResult<PackageStats>()

        try {
            val getPackageSizeInfo = pm.javaClass.getMethod("getPackageSizeInfo", String::class.java, IPackageStatsObserver::class.java)

            getPackageSizeInfo.invoke(pm, app.packageName,
                    object : IPackageStatsObserver.Stub() {
                        @Throws(RemoteException::class)
                        override fun onGetStatsCompleted(pStats: PackageStats, succeeded: Boolean) {
                            result.set(pStats)
                        }
                    })
        } catch (e: Exception) {
            throw InvocationTargetException(e)
        }

        return result.get()
    }
}
