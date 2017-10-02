package com.vs_unusedappremover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PackagesChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(MyApplication.TAG, "received " + intent.action)
        MyApplication.instance.applications.notifyChanged()
    }
}
