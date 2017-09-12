package com.vs_unusedappremover;

import android.app.ActivityManager;
import android.app.ActivityManager.RecentTaskInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Iterables;
import com.vs_unusedappremover.common.HandlerTimer;
import com.vs_unusedappremover.common.MillisecondsIn;
import com.vs_unusedappremover.data.Applications.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class MonitorApplicationsService extends Service  {
	
	private static final String TAG = MonitorApplicationsService.class.getSimpleName();
	private static final long GET_FOREGROUND_APP_INTERVAL = 5 * MillisecondsIn.SECOND;
	private static final long GET_RECENT_APPS_INTERVAL = 30 * MillisecondsIn.SECOND;
		
	private MyApplication app;
	private LinkedList<String> prevRecentTasks = new LinkedList<String>();
	private HandlerTimer recentAppsTimer;
	private HandlerTimer runningAppTimer;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		app = MyApplication.getInstance();				
					
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(screenTurnedOnReceiver, filter);
								
		Handler mainThreadHandler = new Handler();
		recentAppsTimer = new HandlerTimer(mainThreadHandler, GET_RECENT_APPS_INTERVAL) {			
			@Override
			protected void onTick() {
				updateRecentUsedApps();
			}
		};		
		
		runningAppTimer = new HandlerTimer(mainThreadHandler, GET_FOREGROUND_APP_INTERVAL) {
			@Override
			protected void onTick() {
				updateRunningApps();				
			}
		};
		
		PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
		onScreenStateChanged(pm.isScreenOn());
		
		Log.i(TAG, "service created");
	}
	
	@Override
	public void onDestroy() {		
		super.onDestroy();		
		unregisterReceiver(screenTurnedOnReceiver);		
		Log.i(TAG, "service destroyed");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Is not designed to bind to");
	}
	
	private void onScreenStateChanged(boolean isScreenOn) {
		//recentAppsTimer.setEnabled(isScreenOn);
		runningAppTimer.setEnabled(isScreenOn);
	}
		
	private LinkedList<String> getRecentTasks() {
		ActivityManager activityManager = (ActivityManager)app.getSystemService(ACTIVITY_SERVICE);
		LinkedList<String> recentTasks = new LinkedList<String>();
		
		for (RecentTaskInfo task : activityManager.getRecentTasks(50, ActivityManager.RECENT_WITH_EXCLUDED)) {
			ComponentName component = task.baseIntent.getComponent();
			if (component != null) {
				recentTasks.add(component.getPackageName());
			}
		}
		return recentTasks;
	}
	
	private List<String> selectRecentUsed(LinkedList<String> recentTasks) {
						
		ArrayList<String> used = new ArrayList<String>();
		for (String recent : recentTasks) {			
			if (!recent.equals(prevRecentTasks.peek())) {
				used.add(recent);				
			}
			prevRecentTasks.remove(recent);
		}
		prevRecentTasks = recentTasks;
		return used;
	}
	
	private void showNotificationIfNeeded() {		
		if (UnusedAppsNotification.needToShow(this)) {			
			Collection<AppEntry> unused = app.getApplications().values(Filter.UNUSED.create());			
			if (unused.size() > 0) {				
				long packageSizes = 0;
				for (AppEntry app : unused) {
					packageSizes += app.size;
				}				
				UnusedAppsNotification.show(this, unused.size(), packageSizes);
			}
		}
	}
	
	private void updateRecentUsedApps() {
		Iterable<String> recentUsed = selectRecentUsed(getRecentTasks());
		Log.i(TAG, "resent used: " + Iterables.toString(recentUsed));
		long time = System.currentTimeMillis();
		for (String appPackage : recentUsed) {
			app.getApplications().notifyUsed(appPackage, time, AppEntry.RanIn.FOREGROUND);
		}				
	}

	private void updateRunningApps() {
		app.getBackgroundExecutor().execute(new Runnable() {			
			@Override
			public void run() {
                updateForegroundApps();
                updateRunningServices();
			}
		});		
	}

    private void updateForegroundApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @SuppressWarnings("ResourceType")
            UsageStatsManager m = (UsageStatsManager) app.getSystemService("usagestats");
            List<UsageStats> usageStats = m.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis());
            if (usageStats != null) {
                for (UsageStats s : usageStats) {
                    app.getApplications().notifyUsed(s.getPackageName(), s.getLastTimeUsed(), AppEntry.RanIn.FOREGROUND);
                }
            }
        } else {
            ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            for (RunningTaskInfo info : am.getRunningTasks(100)) {
                if (info.topActivity != null) {
                    String taskName = info.topActivity.getPackageName();
                    app.getApplications().notifyUsed(taskName, System.currentTimeMillis(), AppEntry.RanIn.FOREGROUND);
                    break;
                }
            }
        }
    }

    private void updateRunningServices() {
		ActivityManager am = (ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningServiceInfo> infos = am.getRunningServices(500);
		for (RunningServiceInfo info : infos) {			
			
			String appPackage = getPackageFromProcess(info.process);
			long time = fromSysTimeToClock(info.lastActivityTime); 			
			app.getApplications().notifyUsed(appPackage, time, AppEntry.RanIn.BACKGROUND);
		}
	}
	
	private String getPackageFromProcess(String process) {
		String appPackage = process;
		int index = appPackage.indexOf(':');
		if (index > 0) {
			appPackage = appPackage.substring(0, index);
		}
		return appPackage;
	}
	
	private long fromSysTimeToClock(long system) {
		return System.currentTimeMillis() - SystemClock.uptimeMillis() + system;
	}

	private final BroadcastReceiver screenTurnedOnReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "screenOnOffReceiver: " + intent.getAction());
			
			String action = intent.getAction();
			boolean isScreenOn = Intent.ACTION_SCREEN_ON.equals(action);
			onScreenStateChanged(isScreenOn);
			showNotificationIfNeeded();
		}
	};
}
