package com.vs_unusedappremover

import android.content.pm.ApplicationInfo

class AppEntry {

    enum class RanIn private constructor(val id: Int) {
        UNKNOWN(0),
        BACKGROUND(1),
        FOREGROUND(2);


        companion object {

            fun byId(id: Int): RanIn {
                for (v in values()) {
                    if (v.id == id) return v
                }
                throw IllegalArgumentException("No item with such id")
            }
        }
    }

    var label: String? = null
    var size: Long = 0
    var info: ApplicationInfo? = null
    var lastUsedTime: Long = 0
    var ranIn: RanIn? = null
    var installTime: Long = 0
    var rating: Float = 0.toFloat()
    var downloadCount: String? = null
    var notifyAbout = true
}
