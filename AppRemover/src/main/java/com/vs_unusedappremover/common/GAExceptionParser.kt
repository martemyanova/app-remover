package com.vs_unusedappremover.common

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.analytics.tracking.android.ExceptionParser
import com.google.analytics.tracking.android.Log
import java.util.TreeSet

import java.util.Arrays.asList

class GAExceptionParser(context: Context, vararg additionalPackages: String) : ExceptionParser {

    private val mIncludedPackages = TreeSet<String>()

    init {
        setIncludedPackages(context, asList(*additionalPackages))
    }

    fun setIncludedPackages(context: Context, additionalPackages: Collection<String>) {
        // using tree set here, because it sorted in ascending order, which is viable for the
        // underlying loop correctness
        val packages = getApplicationComponentPackages(context)
        packages.addAll(additionalPackages)

        mIncludedPackages.clear()
        for (packageName in packages) {
            if (!isSubpackageOfIncluded(packageName)) {
                mIncludedPackages.add(packageName)
            }
        }
    }

    private fun isSubpackageOfIncluded(packageName: String): Boolean {
        for (includedName in mIncludedPackages) {
            if (packageName.startsWith(includedName)) {
                return true
            }
        }
        return false
    }

    private fun getApplicationComponentPackages(context: Context?): TreeSet<String> {
        val packages = TreeSet<String>()
        if (context != null) {
            try {
                val appContext = context.applicationContext
                val appPackage = appContext.packageName
                packages.add(appPackage)
                val info = appContext.packageManager.getPackageInfo(appPackage,
                        PackageManager.GET_ACTIVITIES or
                                PackageManager.GET_RECEIVERS or
                                PackageManager.GET_SERVICES or
                                PackageManager.GET_PROVIDERS)

                val androidAppComponents = info.activities
                if (androidAppComponents != null) {
                    for (activityInfo in androidAppComponents) {
                        packages.add(activityInfo.packageName)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.i("No package found")
            }

        }
        return packages
    }

    protected fun getCause(t: Throwable): Throwable {
        var result = t
        while (result.cause != null) {
            result = result.cause
        }
        return result
    }

    protected fun getBestStackTraceElement(t: Throwable): StackTraceElement? {
        val elements = t.stackTrace
        if (elements == null || elements.size == 0) {
            return null
        }

        for (e in elements) {
            val className = e.className
            if (isSubpackageOfIncluded(className)) {
                return e
            }
        }
        return elements[0]
    }

    protected fun getDescription(cause: Throwable, element: StackTraceElement?, threadName: String): String {
        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append(cause.javaClass.getSimpleName())
        if (element != null) {
            descriptionBuilder.append(String.format(" (@%s:%s:%s)",
                    element.className,
                    element.methodName,
                    element.lineNumber))
        }

        // It seems to be not usefull
        /* if (threadName != null) {
            descriptionBuilder.append(String.format(" {%s}", new Object[]{threadName}));
        }*/
        descriptionBuilder.append(String.format(" SDK-%d '%s %s'",
                android.os.Build.VERSION.SDK_INT,
                android.os.Build.MANUFACTURER,
                android.os.Build.MODEL))

        return descriptionBuilder.toString()
    }

    override fun getDescription(threadName: String, t: Throwable): String {
        val cause = getCause(t)
        return getDescription(cause, getBestStackTraceElement(cause), threadName)
    }
}
