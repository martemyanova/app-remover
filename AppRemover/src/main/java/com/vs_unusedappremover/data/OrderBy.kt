package com.vs_unusedappremover.data

import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.R
import java.util.*

enum class OrderBy(val shortTextResId: Int, val fullTextResId: Int) {

    TIME_UNUSED(R.string.order_by_time_unused_action_bar,
            R.string.order_by_time_unused_menu) {

        override val comparator: Comparator<AppEntry>
            get() = AppEntry.byTimeUnused
    },

    NAME(R.string.order_by_name_action_bar, R.string.order_by_name_menu) {

        override val comparator: Comparator<AppEntry>
            get() = AppEntry.byLabel
    },

    SIZE(R.string.order_by_size_action_bar, R.string.order_by_size_menu) {

        override val comparator
            get() = AppEntry.bySize
    };

    abstract val comparator: Comparator<AppEntry>
}
