package com.vs_unusedappremover

import android.app.Activity
import android.app.AppOpsManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.util.Log
import android.view.Gravity
import android.view.WindowManager

class RequestPermissionDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert)
        } else {
            AlertDialog.Builder(context)
        }

        val d = builder
                .setMessage(R.string.request_permission)
                .setPositiveButton(R.string.allow) { _, _ -> queryUsageStatsPermissionIfNeeded(activity) }
                .setNegativeButton(R.string.deny, null)
                .setCancelable(false)
                .create()

        val window = d.window
        val attributes = window.attributes
        attributes.gravity = Gravity.BOTTOM
        attributes.flags = attributes.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
        window.attributes = attributes

        return d
    }

    companion object {

        private val TAG = "RequestPermissionDialog"

        fun showIfNeeded(parent: FragmentActivity): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!hasQueryUsageStatsPermission()) {
                    val f = RequestPermissionDialog()
                    f.show(parent.supportFragmentManager, "grant permission")
                    return true
                }
            }
            return false
        }

        private fun hasQueryUsageStatsPermission(): Boolean {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                try {
                    val context = MyApplication.instance
                    val aom = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    val mode = aom.checkOp(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                    return mode == AppOpsManager.MODE_ALLOWED
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to check permission: " + e)
                }

            }
            return false
        }

        private fun queryUsageStatsPermissionIfNeeded(parent: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !hasQueryUsageStatsPermission()) {
                try {
                    parent.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    return
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to open " + Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }

                try {
                    parent.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    return
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Unable to open " + Settings.ACTION_SECURITY_SETTINGS)
                }

                Log.w(TAG, "Failed to open any activity to request permissions")
            }
        }
    }
}
