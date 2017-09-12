package com.vs_unusedappremover;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

public class RequestPermissionDialog extends AppCompatDialogFragment {

    private static final String TAG = "RequestPermissionDialog";

    public static boolean showIfNeeded(FragmentActivity parent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!hasQueryUsageStatsPermission()) {
                RequestPermissionDialog f = new RequestPermissionDialog();
                f.show(parent.getSupportFragmentManager(), "grant permission");
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getContext());
        }

        Dialog d = builder
                .setMessage(R.string.request_permission)
                .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        queryUsageStatsPermissionIfNeeded(getActivity());
                    }
                })
                .setNegativeButton(R.string.deny, null)
                .setCancelable(false)
                .create();

        Window window = d.getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.gravity = Gravity.BOTTOM;
        attributes.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(attributes);

        return d;
    }

    private static boolean hasQueryUsageStatsPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            try {
                MyApplication context = MyApplication.getInstance();
                @SuppressWarnings("ResourceType")
                AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
                int mode = aom.checkOp(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
                return mode == AppOpsManager.MODE_ALLOWED;
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to check permission: " + e);
            }
        }
        return false;
    }

    private static void queryUsageStatsPermissionIfNeeded(Activity parent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !hasQueryUsageStatsPermission()) {
            try {
                parent.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return;
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to open " + Settings.ACTION_USAGE_ACCESS_SETTINGS);
            }

            try {
                parent.startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                return;
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to open " + Settings.ACTION_SECURITY_SETTINGS);
            }

            Log.w(TAG, "Failed to open any activity to request permissions");
        }
    }
}
