package com.vs_unusedappremover.data;

import android.content.pm.ApplicationInfo;

import com.google.common.base.Predicate;
import com.vs_unusedappremover.AppEntry;
import com.vs_unusedappremover.MyApplication;
import com.vs_unusedappremover.common.MillisecondsIn;

public class Applications {
			
	public static final long UNUSED_INTERVAL_MILLIS = MillisecondsIn.DAY * 7;
	
	public static enum Filter {
		DOWNLOADED {
			@Override
			public Predicate<AppEntry> create() {
				final String myPackage = MyApplication.getInstance().getPackageName();
				
				return new Predicate<AppEntry>() {
					@Override
					public boolean apply(AppEntry entry) {
						return !myPackage.equals(entry.info.packageName);
					}
				};
			}			
		},
		
		UNUSED {
			@Override
			public Predicate<AppEntry> create() {
				MyApplication app = MyApplication.getInstance();	
				final long thisInstallTime = app.getInstallTime();
				final String myPackage = app.getPackageName();
				
				return new Predicate<AppEntry>() {
					@Override
					public boolean apply(AppEntry entry) {
						return isUnused(entry, thisInstallTime) && !myPackage.equals(entry.info.packageName);
					}
				};
			}			
		};
		
		public abstract Predicate<AppEntry> create();
	}
			
	public static boolean isThirdParty(ApplicationInfo app) {
		if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) return false;
		if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) return false;
		return true;
	}
	
	private static boolean isUnused(AppEntry app, long thisAppInstallDate) {
		if (!app.notifyAbout) return false;
		long timeUsed = Math.max(app.lastUsedTime, app.installTime);
		timeUsed = Math.max(timeUsed, thisAppInstallDate);
		return System.currentTimeMillis() - timeUsed > UNUSED_INTERVAL_MILLIS;
	}	
}
