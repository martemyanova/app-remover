package com.vs_unusedappremover.common

import android.app.Activity
import android.content.Context
import android.util.Log

import com.google.analytics.tracking.android.EasyTracker
import com.google.analytics.tracking.android.ExceptionParser
import com.google.analytics.tracking.android.Fields
import com.google.analytics.tracking.android.MapBuilder

import java.lang.Thread.UncaughtExceptionHandler

object GA {

    private val TAG = "GA"
    private var sTracker: EasyTracker? = null
    private var exceptionParser: ExceptionParser? = null

    fun init(resourcesPackage: String?, context: Context, vararg additionalPackages: String) {
        EasyTracker.setResourcePackageName(resourcesPackage)
        sTracker = EasyTracker.getInstance(context)
        exceptionParser = GAExceptionParser(context, *additionalPackages)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, th ->
            reportException(thread, th, true)
            defaultHandler.uncaughtException(thread, th)
        }
    }

    fun setCustomDimension(index: Int, value: String) {
        sTracker!!.set(Fields.customDimension(index), value)
        Log.i(TAG, "set dimension[$index]=$value")
    }

    fun onActivityStart(sender: Activity) {
        sTracker!!.activityStart(sender)
    }

    fun onActivityStop(sender: Activity) {
        sTracker!!.activityStop(sender)
    }

    fun onFragmentShown(tag: String) {
        sTracker!!.set(Fields.SCREEN_NAME, tag)
        sTracker!!.send(MapBuilder
                .createAppView()
                .build())
    }

    fun event(category: String, action: String) {
        eventInternal(category, action, null, null)
    }

    fun event(category: String, action: String, description: String) {
        eventInternal(category, action, description, null)
    }

    fun event(category: String, action: String, value: Long) {
        eventInternal(category, action, null, value)
    }

    fun event(category: String, action: String, value: Boolean) {
        eventInternal(category, action, null, if (value) 1L else 0L)
    }

    @JvmOverloads
    fun timing(category: String, timeInMilliseconds: Long, name: String, label: String? = null) {
        sTracker!!.send(MapBuilder.createTiming(category, timeInMilliseconds, name, label).build())
        Log.i(TAG, "timing $category time=\"$timeInMilliseconds\" name=\"$name\" label=$label")
    }

    fun reportException(th: Throwable) {
        reportException(Thread.currentThread(), th, false)
    }

    fun reportException(thread: Thread, th: Throwable, fatal: Boolean) {
        val description = exceptionParser!!.getDescription(thread.name, th)
        sTracker!!.send(MapBuilder
                .createException(description, false)
                .build())
    }

    private fun eventInternal(category: String, action: String, description: String?, value: Long?) {
        sTracker!!.send(MapBuilder.createEvent(category, action, description, value).build())
        Log.i(TAG, "event $category action=\"$action\" desc=\"$description\" value=$value")
    }
}