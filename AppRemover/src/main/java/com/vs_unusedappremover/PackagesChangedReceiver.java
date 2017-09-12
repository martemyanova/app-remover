package com.vs_unusedappremover;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PackagesChangedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(MyApplication.TAG, "received " + intent.getAction());
		MyApplication.getInstance().getApplications().notifyChanged();
	}
}
