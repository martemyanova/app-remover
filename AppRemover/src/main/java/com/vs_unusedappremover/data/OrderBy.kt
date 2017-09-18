package com.vs_unusedappremover.data

import java.text.Collator
import java.util.Comparator

import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.R

enum class OrderBy private constructor(val shortTextResId: Int, val fullTextResId: Int) : Comparator<AppEntry> {

    TIME_UNUSED(R.string.order_by_time_unused_action_bar,
            R.string.order_by_time_unused_menu) {
        override fun compare(e1: AppEntry, e2: AppEntry): Int {
            val time1 = Math.max(e1.installTime, e1.lastUsedTime)
            val time2 = Math.max(e2.installTime, e2.lastUsedTime)
            if (time1 < time2) return -1
            return if (time1 > time2) 1 else NAME.compare(e1, e2)
        }
    },

    NAME(R.string.order_by_name_action_bar,
            R.string.order_by_name_menu) {
        override fun compare(e1: AppEntry, e2: AppEntry): Int {
            return Collator.getInstance().compare(e1.label, e2.label)
        }
    },

    SIZE(R.string.order_by_size_action_bar,
            R.string.order_by_size_menu) {
        override fun compare(e1: AppEntry, e2: AppEntry): Int {
            if (e1.size < e2.size) return 1
            return if (e1.size > e2.size) -1 else 0
        }
    }
}
