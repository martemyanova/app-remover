package com.vs_unusedappremover.data;

import android.content.res.Resources;
import android.util.Log;

import com.seppius.i18n.plurals.PluralResources;
import com.vs_unusedappremover.MyApplication;

public class Plural {
	
	private final Resources res;
	private PluralResources pluralRes;
	
	public Plural(Resources resources) {
		this.res = resources;
		try {
			this.pluralRes = new PluralResources(res);
		} catch (Exception e) {
			Log.e(MyApplication.TAG, "unable to create plural resources", e);
			this.pluralRes = null;
		}
	}
	
	public String format(int pluralResId, int quantity, Object...args) {
		if (pluralRes != null) {
			return pluralRes.getQuantityString(pluralResId, quantity, args);
		} else {
			return res.getQuantityString(pluralResId, quantity, args);
		}
	}
}
