package com.vs_unusedappremover.common

class AsyncResult<T> {

    private val waitMonitor = java.lang.Object()
    private var result: T? = null
    private var hasResult: Boolean = false

    fun set(result: T) {
        synchronized(waitMonitor) {
            this.result = result
            hasResult = true
            waitMonitor.notify()
        }
    }

    fun get(): T? {
        synchronized(waitMonitor) {
            while (!hasResult) {
                try {
                    waitMonitor.wait()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }

            }
            return result
        }
    }
}
