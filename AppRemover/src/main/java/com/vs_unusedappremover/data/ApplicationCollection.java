package com.vs_unusedappremover.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.vs_unusedappremover.AppEntry;
import com.vs_unusedappremover.MyApplication;
import com.vs_unusedappremover.PackageSize;
import com.vs_unusedappremover.data.DatabaseHelper.AppTable;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;

public class ApplicationCollection {
	
	private static final String TAG = ApplicationCollection.class.getSimpleName();
	private static final long NOTIFY_UPDATED_INTERVAL = 500;
	
	private HashMap<String, AppEntry> previousData = new HashMap<>();
	private final Context context;
	private final HashSet<DataSetObserver> observers = new HashSet<>();
	private final GroupChangeNotifications groupedChangeNotifications = new GroupChangeNotifications(NOTIFY_UPDATED_INTERVAL);
	private final MyApplication app = MyApplication.getInstance();
	private final Object modificationLock = new Object();
	
	public ApplicationCollection(Context context) {
		this.context = context;
		notifyChanged();
	}
	
	public ArrayList<AppEntry> values(Predicate<AppEntry> filter) {
		return values(filter, null);
	}
	
	public ArrayList<AppEntry> values(Predicate<AppEntry> filter, Comparator<AppEntry> order) {		
		synchronized (modificationLock) {
			ArrayList<AppEntry> result = Lists.newArrayList(Collections2.filter(getData().values(), filter));
			if (order != null) {
				Collections.sort(result, order);
			}
			return result;
		}
	}
			
	public void notifyUsed(String appPackage, long when, AppEntry.RanIn ranIn) {
		long now = System.currentTimeMillis();
		synchronized (modificationLock) {
			AppEntry entry = getData().get(appPackage);
			if (entry != null && entry.lastUsedTime < when) {				
				entry.lastUsedTime = when;
                entry.ranIn = ranIn;
				groupedChangeNotifications.add(entry);
				Log.i(TAG, appPackage + " used " + (now - when) / 1000 + "sec ago");
			}			
		}
	}
	
	public void setNotifyAbout(String appPackage, boolean value) {
		synchronized (modificationLock) {
			AppEntry entry = getData().get(appPackage);
			if (entry != null && entry.notifyAbout != value) {
				entry.notifyAbout = value;
				saveToCache(singletonList(entry));
				fireChanged();
			}			
		}
	}
	
	public void addObserver(DataSetObserver o) {
		if (o != null) {
			observers.add(o);
		}
	}
	
	public void removeObserver(DataSetObserver o) {
		if (o != null) {
			observers.remove(o);
		}
	}
	
	public void notifyChanged() {
		final PackageManager pm = context.getPackageManager();

		synchronized (modificationLock) {

			HashMap<String, AppEntry> data = new HashMap<String, AppEntry>();
			for (ApplicationInfo appInfo : pm.getInstalledApplications(0)) {
				if (Applications.isThirdParty(appInfo)) {
				
					AppEntry entry = previousData.get(appInfo.packageName);
					if (entry == null) {					 
						entry = new AppEntry();
						entry.info = appInfo;
					    entry.size = PackageSize.UNKNOWN;
                        entry.ranIn = AppEntry.RanIn.UNKNOWN;
                        entry.label = appInfo.packageName;
					    fillFromDbCache(entry);			    
					}
					data.put(appInfo.packageName, entry);
					app.getBackgroundExecutor().execute(new LoadExpensiveInfo(entry));
				}
			}
			previousData = data;
		}
		
		fireChanged();
	}

	private HashMap<String, AppEntry> getData() {
		synchronized (modificationLock) {
			return previousData;
		}
	}
	
	private void fillFromDbCache(AppEntry entry) {
		DatabaseHelper dbHelper = app.getDbHelper();
		
		Cursor c = new SelectQuery()
			.select(AppTable.TIME_LAST_USED, AppTable.TIME_INSTALLED, 
					AppTable.DOWLOAD_COUNT, AppTable.RATING, 
					AppTable.SIZE, AppTable.NOTIFY, AppTable.RAN_IN)
			.from(AppTable.NAME)
			.where(AppTable.PACKAGE + "=?", entry.info.packageName)
			.execute(dbHelper.getReadableDatabase());
				
		try {
			if (c.moveToFirst()) {
				entry.lastUsedTime = c.getLong(0);
                entry.ranIn = AppEntry.RanIn.byId(c.getInt(6));
				entry.installTime = c.getLong(1);
				entry.downloadCount = c.getString(2);
				entry.rating = c.getFloat(3);
				entry.size = c.getLong(4);
				entry.notifyAbout = (c.getInt(5) != 0);
			}
		} finally {
			c.close();
		}
	}

	private void saveToCache(Iterable<AppEntry> entries) {		
		Set<String> existingApps = getExistingApps();
		
		SQLiteDatabase db = app.getDbHelper().getWritableDatabase();
		db.beginTransaction();
		try {			
			ContentValues values = new ContentValues();
			
			for (AppEntry entry : entries) {
											 
				values.put(AppTable.TIME_INSTALLED, entry.installTime);
				values.put(AppTable.TIME_LAST_USED, entry.lastUsedTime);
				values.put(AppTable.DOWLOAD_COUNT, entry.downloadCount);
				values.put(AppTable.RATING, entry.rating);
				values.put(AppTable.SIZE, entry.size);
				values.put(AppTable.NOTIFY, entry.notifyAbout);
                values.put(AppTable.RAN_IN, entry.ranIn.id);

                if (existingApps.contains(entry.info.packageName)) {
					db.update(AppTable.NAME, values, AppTable.PACKAGE + "=?", new String[] { entry.info.packageName });
				} else {
					values.put(AppTable.PACKAGE, entry.info.packageName);
					db.insertWithOnConflict(AppTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
				}
				values.clear();
			}
			
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
				
		final DateFormat time = DateFormat.getTimeInstance(DateFormat.SHORT);
		Log.i(TAG, " cached " + Iterables.toString(Iterables.transform(entries, new Function<AppEntry, String>() {
			@Override 
			public String apply(AppEntry e) {
				return e.info.packageName + "=(" +  time.format(new Date(e.lastUsedTime)) + ")";
			}
		})));		
	}
	
	private Set<String> getExistingApps() {
		SQLiteDatabase db = app.getDbHelper().getReadableDatabase();
		Cursor c = new SelectQuery()
			.select(AppTable.PACKAGE)
			.from(AppTable.NAME)
			.execute(db);
		
		HashSet<String> packages = new HashSet<String>(c.getColumnCount());
		try {
			while (c.moveToNext()) {
				packages.add(c.getString(0));
			}
			return packages;
		} finally {
			c.close();
		}			
	}
	
	private void fireChanged() {
		for (DataSetObserver o : observers) {
			o.onChanged();
		}
	}
		
	private class LoadExpensiveInfo implements Runnable {

		private final AppEntry entryToFill;
		private final PackageManager pm = context.getPackageManager();
		
		public LoadExpensiveInfo(AppEntry entry) {
			this.entryToFill = entry;
		}
		
		@Override
		public void run() {			
			File apk = new File(entryToFill.info.sourceDir);
			long size = PackageSize.get(pm, entryToFill.info);
			String packageName = entryToFill.info.packageName;
			String label = apk.exists() ? entryToFill.info.loadLabel(pm).toString() : packageName;
			long installTime = (entryToFill.installTime == 0) ? getFirstInstallTime(packageName) : entryToFill.installTime;
			
			if (size != entryToFill.size ||
				!label.equals(entryToFill.label) ||
				installTime != entryToFill.installTime) 
			{
				synchronized (modificationLock) {
					entryToFill.size = size;
					entryToFill.label = label;
					entryToFill.installTime = installTime;
				}
				groupedChangeNotifications.add(entryToFill);
			}
		}

		private long getFirstInstallTime(String packageName) {
			try {
				return pm.getPackageInfo(packageName, 0).firstInstallTime;
			} catch (NameNotFoundException e) {
				return 0;
			}
		}
	}

	private class GroupChangeNotifications implements Runnable {
		
		private final long updatePeriodMillis;
		private final HashMap<String, AppEntry> changes = new HashMap<String, AppEntry>();
		
		public GroupChangeNotifications(long updatePeriodMillis) {
			this.updatePeriodMillis = updatePeriodMillis;
		}
		
		public synchronized void add(AppEntry entry) {
			boolean wasEmpty = (changes.size() == 0);
			changes.put(entry.info.packageName, entry);
			if (wasEmpty) {
				app.getUiHandler().postDelayed(this, updatePeriodMillis);
			}
		}
		
		public synchronized int flush() {
			final ArrayList<AppEntry> changedList = new ArrayList<AppEntry>(changes.values());
			app.getBackgroundExecutor().execute(new Runnable() {				
				@Override
				public void run() {
					saveToCache(changedList);					
				}
			});			
			int size = changes.size();
			changes.clear();
			return size;
		}
		
		@Override
		public void run() {
			int count = flush();
			Log.i(TAG, "flushed " + count + " entries to cache");
			fireChanged();
		}
	}
}
