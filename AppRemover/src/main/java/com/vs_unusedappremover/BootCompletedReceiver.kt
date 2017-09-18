package com.vs_unusedappremover

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        MyApplication.startMonitoringService(context)
    }
}
