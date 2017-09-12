package com.vs_unusedappremover;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.RemoteException;

import com.vs_unusedappremover.common.AsyncResult;

public class PackageSize {
	public static final long UNKNOWN = -1;
		
	public static long get(PackageManager pm, ApplicationInfo app) {
		try {			
			PackageStats ps = getPackageStats(pm, app);
			if (ps != null) return getFromStats(ps);
			return UNKNOWN;
		} catch (InvocationTargetException e) {
			return getFromFile(app);
		} catch (NoSuchFieldError e) {
			return getFromFile(app);
		}
	}
		
	@SuppressLint("NewApi")
	private static long getFromStats(PackageStats ps) {
		long size = ps.codeSize + ps.dataSize + ps.cacheSize;
				
		if (android.os.Build.VERSION.SDK_INT >= 11) {		
			size += ps.externalCodeSize + ps.externalObbSize
					+ ps.externalDataSize + ps.externalMediaSize
					+ ps.externalCacheSize;
		}
        return size;
	}
	
	private static long getFromFile(ApplicationInfo app) {
		try {
			File f = new File(app.sourceDir);		
			if (f.exists()) return f.length();
			return UNKNOWN;		
		} catch (SecurityException e) {
			return UNKNOWN;
		}		
	}
	
	private static PackageStats getPackageStats(PackageManager pm, ApplicationInfo app) throws InvocationTargetException {
		final AsyncResult<PackageStats> result = new AsyncResult<PackageStats>();
			
		try {
			Method getPackageSizeInfo = pm.getClass().getMethod("getPackageSizeInfo", String.class, IPackageStatsObserver.class);
		
			getPackageSizeInfo.invoke(pm, app.packageName,
			    new IPackageStatsObserver.Stub() {
			        @Override
			        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) throws RemoteException {
			        	result.set(pStats);			        	
			        }
			    });
		} catch (Exception e) {
			throw new InvocationTargetException(e);
		}
		return result.get();
	}
}
