package com.vs_unusedappremover.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;

public class SelectQuery {	
	
	private Object from;
	private String[] columns;
	private String selection;
	private String[] selectArgs;
	private String orderBy;
		
	public SelectQuery select(String...columns) {
		this.columns = columns;  
		return this;
	}
	
	public SelectQuery from(String table) {
		this.from = table;
		return this;
	}
	
	public SelectQuery from(Uri uri) {
		this.from = uri;
		return this;
	}
	
	public SelectQuery where(String condition, Object...args) {
		this.selection = condition;
		selectArgs = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			selectArgs[i] = (args[i] != null) ? args[i].toString() : null;
		}
		return this;
	}
	
	public SelectQuery orderByAscending(String column) {
		orderBy = column + " asc";
		return this;
	}
	
	public SelectQuery orderByDescending(String column) {
		orderBy = column + " desc";
		return this;
	}
		
	public Cursor execute(SQLiteDatabase db) {			
		Cursor c = db.query((String)from, columns, selection, selectArgs, null, null, orderBy);
		if (c == null) throw new SQLiteException("null cursor");
		return c;
	}
	
	public Cursor execute(ContentResolver resolver) {
		Cursor c = resolver.query((Uri)from, columns, selection, selectArgs, orderBy);
		if (c == null) throw new SQLiteException("null cursor");
		return c;
	}
}
