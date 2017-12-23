package com.vs_unusedappremover

import android.content.pm.ApplicationInfo
import android.net.Uri


data class AppEntry (var info: ApplicationInfo,
                var label: String,
                var size: Long = PackageSize.UNKNOWN,
                var installTime: Long = 0,
                var lastUsedTime: Long = 0,
                var ranIn: RanIn = RanIn.UNKNOWN,
                var rating: Float = 0F,
                var notifyAbout: Boolean = true,
                var downloadCount: String? = null) {

    enum class RanIn constructor(val id: Int) {
        UNKNOWN(0),
        BACKGROUND(1),
        FOREGROUND(2);


        companion object {

            fun byId(id: Int): RanIn {
                values()
                        .filter { it.id == id }
                        .forEach { return it }
                throw IllegalArgumentException("No item with such id")
            }
        }
    }

    fun buildUrl(): Uri {
        val packageName: String = info.packageName
        return Uri.parse(AppIcon.SCHEME + "://" + packageName)
    }

    companion object {
        val byLabel = compareBy(AppEntry::label)
        val bySize = compareByDescending(AppEntry::size)
        val byTimeUnused = Comparator<AppEntry> { e1, e2 ->
            val time1 = Math.max(e1.installTime, e1.lastUsedTime)
            val time2 = Math.max(e2.installTime, e2.lastUsedTime)
            if (time1 == time2) 0 else if (time1 < time2) -1 else 1
        }.then(byLabel)
    }
}
