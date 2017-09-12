package com.vs_unusedappremover;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;

import com.squareup.picasso.Picasso;
import com.vs_unusedappremover.common.GA;
import com.vs_unusedappremover.data.ApplicationCollection;
import com.vs_unusedappremover.data.DatabaseHelper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyApplication extends Application {
	
	public static final String TAG = MyApplication.class.getSimpleName();
	private static final String INSTALL_TIME = "install time";
	
	private static MyApplication instance;
	
	private DatabaseHelper database;
	private final ThreadPoolExecutor backgroundExecutor;
	private Handler uiHandler;
	private ApplicationCollection applications;
	private Picasso picasso;

	public static MyApplication getInstance() {
		return instance;
	}
	
	public static void startMonitoringService(Context context) {
		context.startService(new Intent(context, MonitorApplicationsService.class));
	}
	
	public MyApplication() {
		backgroundExecutor = new ThreadPoolExecutor(2, 16, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		backgroundExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
	}
		
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		uiHandler = new Handler();
        GA.init(null, this);
		applications = new ApplicationCollection(this);
		picasso = new Picasso.Builder(this)
				.addRequestHandler(AppIcon.createRequestHandler(this))
				.build();
		startMonitoringService(this);
		checkInstallTimePresent();
	}
	
	public synchronized DatabaseHelper getDbHelper() {
		if (database == null) {
			database = new DatabaseHelper(this);
		}
		return database;
	}
	
	public ExecutorService getBackgroundExecutor() {
		return backgroundExecutor;
	}
	
	public ApplicationCollection getApplications() {
		return applications;
	}
	
	public Handler getUiHandler() {
		return uiHandler;
	}
	
	public long getInstallTime() {
		SharedPreferences prefs = getSharedPreferences(TAG, MODE_PRIVATE);
		return prefs.getLong(INSTALL_TIME, 0);
	}

	public Picasso picasso() {
		return picasso;
	}
	
	private void checkInstallTimePresent() {
		SharedPreferences prefs = getSharedPreferences(TAG, MODE_PRIVATE);
		if (prefs.getLong(INSTALL_TIME, 0) == 0) {
			prefs.edit()
				.putLong(INSTALL_TIME, calculateInstallTime())
				.apply();
		}
	}	
	
	private long calculateInstallTime() {
		long now = System.currentTimeMillis();
		PackageManager pm = getPackageManager();
		try {
			ApplicationInfo info = pm.getApplicationInfo(getPackageName(), 0);
			File apk = new File(info.sourceDir);
			long installTime = apk.lastModified();
			return installTime != 0 ? installTime : now;
		} catch (NameNotFoundException e) {
			return now;
		}
	}
}
