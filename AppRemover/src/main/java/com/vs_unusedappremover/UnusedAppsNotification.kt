package com.vs_unusedappremover

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.support.v4.app.NotificationCompat
import android.text.format.Formatter
import android.util.Log
import com.vs_unusedappremover.common.MillisecondsIn
import com.vs_unusedappremover.data.Plural

object UnusedAppsNotification {

    private val TAG = UnusedAppsNotification::class.java.simpleName

    private val PREFERENCES = UnusedAppsNotification::class.java.name
    private val SETTING_LAST_SHOWN = "last shown"
    private val SHOW_INTERVAL_MILLIS = MillisecondsIn.HOUR * 36

    fun show(context: Context, unusedCount: Int, unusedSize: Long) {
        val notificationIntent = MainActivity.showUnusedFromNotificationIntent(context)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val intent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val res = context.resources

        val apps = Plural(res).format(R.plurals.application_count, unusedCount)

        var title = format(res, R.string.notification_unused_header, apps)
        val first = Character.toUpperCase(title[0])
        title = first + title.substring(1)

        val text: String
        if (unusedSize != 0L) {
            val size = Formatter.formatFileSize(context, unusedSize)
            text = format(res, R.string.notification_unused_apps_consuming, unusedCount, apps, size)
        } else {
            text = format(res, R.string.notification_unused_apps, unusedCount, apps)
        }

        val notification = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, notification)
        notifyShown(context)

        Log.i(TAG, "notification is shown")
    }

    fun needToShow(context: Context): Boolean {
        val settings = context.getSharedPreferences(PREFERENCES, 0)
        val lastShown = settings.getLong(SETTING_LAST_SHOWN, 0)
        return System.currentTimeMillis() - lastShown >= SHOW_INTERVAL_MILLIS
    }

    fun notifyShown(context: Context) {
        val settings = context.getSharedPreferences(PREFERENCES, 0)
        settings.edit()
                .putLong(SETTING_LAST_SHOWN, System.currentTimeMillis())
                .apply()
    }

    private fun format(res: Resources, formatResId: Int, vararg args: Any): String {
        val format = res.getString(formatResId)
        return String.format(format, *args)
    }
}
