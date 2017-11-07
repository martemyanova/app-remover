package com.vs_unusedappremover.data

import com.vs_unusedappremover.AppEntry
import com.vs_unusedappremover.R
import kotlin.Comparator

enum class OrderBy(val shortText: Int, val fullText: Int, val comparator: Comparator<AppEntry>) {

    TIME_UNUSED(
            shortText = R.string.order_by_time_unused_action_bar,
            fullText = R.string.order_by_time_unused_menu,
            comparator = AppEntry.byTimeUnused),

    NAME(
            shortText = R.string.order_by_name_action_bar,
            fullText = R.string.order_by_name_menu,
            comparator = AppEntry.byLabel),

    SIZE(
            shortText = R.string.order_by_size_action_bar,
            fullText = R.string.order_by_size_menu,
            comparator = AppEntry.bySize);

}
