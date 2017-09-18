package com.vs_unusedappremover.common

interface MillisecondsIn {
    companion object {
        val SECOND: Long = 1000
        val MINUTE = 60 * SECOND
        val HOUR = 60 * MINUTE
        val DAY = 24 * HOUR
        val WEEK = 7 * DAY
    }
}
