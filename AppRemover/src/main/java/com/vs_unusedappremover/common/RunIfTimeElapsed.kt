package com.vs_unusedappremover.common

abstract class RunIfTimeElapsed : Runnable {

    private var lastRunMillis: Long = 0

    fun runIfElapsed(interval: Long) {
        val time = System.currentTimeMillis()
        if (time - lastRunMillis > interval) {
            lastRunMillis = time
            run()
        }
    }
}
