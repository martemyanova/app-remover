package com.vs_unusedappremover;

import com.seppius.i18n.plurals.PluralResources;
import com.vs_unusedappremover.R;
import com.vs_unusedappremover.common.MillisecondsIn;
import com.vs_unusedappremover.data.Plural;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;

public class UnusedAppsNotification {

	private static final String TAG = UnusedAppsNotification.class.getSimpleName();
	
	private static final String PREFERENCES = UnusedAppsNotification.class.getName();
	private static final String SETTING_LAST_SHOWN = "last shown";
	private static final long SHOW_INTERVAL_MILLIS = MillisecondsIn.HOUR * 36;
	
	public static void show(Context context, int unusedCount, long unusedSize) {
		Intent notificationIntent = MainActivity.showUnusedFromNotificationIntent(context);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
		PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);		
		Resources res = context.getResources();		
		
		String apps = new Plural(res).format(R.plurals.application_count, unusedCount);
				
		String title = format(res, R.string.notification_unused_header, apps);
		char first = Character.toUpperCase(title.charAt(0));
		title = first + title.substring(1);
		
		String text;
		if (unusedSize != 0) {
			String size = Formatter.formatFileSize(context, unusedSize);
			text = format(res, R.string.notification_unused_apps_consuming, unusedCount, apps, size);
		} else {
			text = format(res, R.string.notification_unused_apps, unusedCount, apps);
		}
		 		
		Notification notification = new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(title)
			.setContentText(text)
			.setContentIntent(intent)
			.setAutoCancel(true)
			.build();
		
		NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(0, notification);
		notifyShown(context);
		
		Log.i(TAG, "notification is shown");
	}
	
	public static boolean needToShow(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCES, 0);
		long lastShown = settings.getLong(SETTING_LAST_SHOWN, 0);
		return System.currentTimeMillis() - lastShown >= SHOW_INTERVAL_MILLIS;
	}

	public static void notifyShown(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFERENCES, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putLong(SETTING_LAST_SHOWN, System.currentTimeMillis());
	    editor.commit();
	}
	
	private static String format(Resources res, int formatResId, Object... args) {
		String format = res.getString(formatResId);
		return String.format(format, args);
	}
}
