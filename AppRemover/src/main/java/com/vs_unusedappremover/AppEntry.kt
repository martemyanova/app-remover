package com.vs_unusedappremover

import android.content.pm.ApplicationInfo

class AppEntry (var info: ApplicationInfo,
                var label: String,
                var size: Long = PackageSize.UNKNOWN,
                var lastUsedTime: Long = 0,
                var ranIn: RanIn = RanIn.UNKNOWN,
                var installTime: Long = 0,
                var rating: Float = 0F,
                var notifyAbout: Boolean = true) {

    enum class RanIn constructor(val id: Int) {
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

    var downloadCount: String? = null
}
