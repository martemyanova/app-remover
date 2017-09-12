package com.vs_unusedappremover;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.vs_unusedappremover.common.GA;

public class RateThisAppDialog extends DialogFragment {

	private static final String TAG =RateThisAppDialog.class.getSimpleName();
	private static final int SHOW_INTERVAL = 12;
	private static final String REQUEST_COUNT = "num requests";
	private static final String IS_RATED = "is rated";
	
	public static void showIfNeeded(FragmentActivity context) {
		SharedPreferences prefs = getPreferences(context);
		
		int requestCount = prefs.getInt(REQUEST_COUNT, 0) + 1;		
		prefs.edit().putInt(REQUEST_COUNT, requestCount).commit();
		
		boolean isRated = prefs.getBoolean(IS_RATED, false);
		boolean isTimeToShow = (requestCount % SHOW_INTERVAL == 0);
		Intent intent = getPlayMarketIntent();
		
		if (!isRated && isTimeToShow /*&& isFromPlayMarket()*/ && intent != null) {
			new RateThisAppDialog().show(context.getSupportFragmentManager(), TAG);			
		}
	}
	
	private static boolean isFromPlayMarket() {
		MyApplication app = MyApplication.getInstance();
		PackageManager pm = app.getPackageManager();
		String installerName = pm.getInstallerPackageName(app.getPackageName());
		return "com.android.vending".equals(installerName);
	}
	
	private static Intent getPlayMarketIntent() {
		MyApplication app = MyApplication.getInstance();
		
		Uri uri = Uri.parse("market://details?id=" + app.getPackageName());
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				
		PackageManager pm = app.getPackageManager();
		if (pm.queryIntentActivities(intent, 0).size() == 0) {
			return null;
		}
		return intent;
	}
	
	private static SharedPreferences getPreferences(Context context) {
		return context.getSharedPreferences("rate app", Context.MODE_PRIVATE);
	}
		
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Activity context = getActivity();
				
		return new AlertDialog.Builder(context)
			.setMessage(R.string.dialog_rate_this_message)
			.setCancelable(true)			
			.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onYesClicked();
				}
			})
			.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					onNoClicked();
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {				
				@Override
				public void onCancel(DialogInterface dialog) {
					onNoClicked();					
				}
			})
			.create();
	}
	
	private void onYesClicked() {
		getActivity().startActivity(getPlayMarketIntent());		
		
		SharedPreferences prefs = getPreferences(getActivity());
		prefs.edit().putBoolean(IS_RATED, true).commit();
		GA.event("MainActivity", "Rate application");
	}
	
	private void onNoClicked() {
		GA.event("MainActivity", "Rejected rate application");
	}
}
