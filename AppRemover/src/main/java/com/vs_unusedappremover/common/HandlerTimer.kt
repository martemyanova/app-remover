package com.vs_unusedappremover.common

import android.os.Handler

abstract class HandlerTimer(private val handler: Handler, private val intervalMillis: Long) {
    private var isEnabled: Boolean = false

    fun setEnabled(value: Boolean) {
        isEnabled = value
        if (isEnabled) {
            timerRunnable.run()
        }
    }

    protected abstract fun onTick()

    private val timerRunnable = object : Runnable {
        override fun run() {
            onTick()
            if (isEnabled) {
                handler.removeCallbacks(this)
                handler.postDelayed(this, intervalMillis)
            }
        }
    }
}

