package com.vs_unusedappremover

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.vs_unusedappremover.common.GA

class RateThisAppDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder: AlertDialog.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog_Alert)
        } else {
            AlertDialog.Builder(activity)
        }

        return builder
                .setMessage(R.string.dialog_rate_this_message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes) { _, _ -> onYesClicked() }
                .setNegativeButton(android.R.string.no) { _, _ -> onNoClicked() }
                .setOnCancelListener { onNoClicked() }
                .create()
    }

    private fun onYesClicked() {
        activity.startActivity(playMarketIntent)

        val prefs = getPreferences(activity)
        prefs.edit().putBoolean(IS_RATED, true).apply()
        GA.event("MainActivity", "Rate application")
    }

    private fun onNoClicked() {
        GA.event("MainActivity", "Rejected rate application")
    }

    companion object {

        private val TAG = RateThisAppDialog::class.java.simpleName
        private val SHOW_INTERVAL = 12
        private val REQUEST_COUNT = "num requests"
        private val IS_RATED = "is rated"

        fun showIfNeeded(context: FragmentActivity) {
            val prefs = getPreferences(context)

            val requestCount = prefs.getInt(REQUEST_COUNT, 0) + 1
            prefs.edit().putInt(REQUEST_COUNT, requestCount).apply()

            val isRated = prefs.getBoolean(IS_RATED, false)
            val isTimeToShow = requestCount % SHOW_INTERVAL == 0
            val intent = playMarketIntent

            if (!isRated && isTimeToShow /*&& isFromPlayMarket()*/ && intent != null) {
                RateThisAppDialog().show(context.supportFragmentManager, TAG)
            }
        }

        private val isFromPlayMarket: Boolean
            get() {
                val app = MyApplication.instance
                val pm = app.packageManager
                val installerName = pm.getInstallerPackageName(app.packageName)
                return "com.android.vending" == installerName
            }

        private val playMarketIntent: Intent?
            get() {
                val app = MyApplication.instance

                val uri = Uri.parse("market://details?id=${app.packageName}")
                val intent = Intent(Intent.ACTION_VIEW, uri)

                val pm = app.packageManager
                return if (pm.queryIntentActivities(intent, 0).size == 0) null else intent
            }

        private fun getPreferences(context: Context): SharedPreferences =
                context.getSharedPreferences("rate app", Context.MODE_PRIVATE)
    }
}
