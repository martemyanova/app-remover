package com.vs_unusedappremover.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vs_unusedappremover.AppEntry;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = DatabaseHelper.class.getSimpleName();
	public static final String DATABASE_NAME = "apps";
	public static final int DATABASE_VERSION = 4;
	
	public interface AppTable { 
		String NAME = "applications";
		
		String PACKAGE = "package";
		String TIME_LAST_USED = "time_last_used";
        String RAN_IN = "ran_in";
		String TIME_INSTALLED = "time_installed";
		String DOWLOAD_COUNT = "download_count";
		String RATING = "rating";
		String SIZE = "size";
		String NOTIFY = "notify";
				
		String CREATE = "create table " + NAME + " (\n"				 
				+ PACKAGE + " text primary key, \n"
				+ TIME_LAST_USED + " int, \n"
				+ TIME_INSTALLED + " int, \n"
				+ DOWLOAD_COUNT + " text, \n"				
				+ RATING + " real, \n"
				+ SIZE + " int, \n" 
				+ NOTIFY + " int, \n"
                + RAN_IN + " int); ";
	}
		
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		getWritableDatabase(); // to upgrade if needed
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(AppTable.CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		try {
			updateInternal(db, oldVersion);
		} catch (SQLiteDiskIOException e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
	private void updateInternal(SQLiteDatabase db, int oldVersion) {
		switch (oldVersion) {
		case 2: upgradeFrom2to3(db);
        case 3: upgradeFrom3to4(db);
			break;
		default:
			db.execSQL("drop table if exists " + AppTable.NAME + ";");
			onCreate(db);
		}
	}
	
	private void upgradeFrom2to3(SQLiteDatabase db) {
		db.execSQL("ALTER TABLE " + AppTable.NAME + " ADD COLUMN " + AppTable.NOTIFY + " int NOT NULL DEFAULT(1);");
	}

    private void upgradeFrom3to4(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE " + AppTable.NAME + " ADD COLUMN " + AppTable.RAN_IN + " int NOT NULL DEFAULT(" + AppEntry.RanIn.UNKNOWN.id + ");");
    }
}
