package com.vs_unusedappremover

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

import com.vs_unusedappremover.common.MillisecondsIn
import com.vs_unusedappremover.data.Plural

import java.util.ArrayList

class ApplicationsAdapter(private val context: Context) : BaseAdapter() {
    private val elapsedTime: ElapsedTimeFormatter
    private val unknownUsageTime: UnknownUsageTimeFormatter
    private val items = ArrayList<AppEntry>()

    init {
        this.elapsedTime = ElapsedTimeFormatter(context)
        this.unknownUsageTime = UnknownUsageTimeFormatter(context)
    }

    fun setApplications(applications: Collection<AppEntry>) {
        items.clear()
        items.addAll(applications)
        notifyDataSetChanged()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(at: Int): AppEntry {
        return items[at]
    }

    override fun getItemId(at: Int): Long {
        return at.toLong()
    }

    override fun getView(index: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val holder = ApplicationViewHolder.createOrReuse(context, convertView)
        convertView = holder.rootView
        val context = convertView.context

        val entry = getItem(index)
        holder.appName.text = entry.label

        MyApplication.instance!!.picasso()!!.load(AppIcon.buildUrl(entry.info!!.packageName)).into(holder.appIcon)

        holder.doNotNotify.visibility = if (entry.notifyAbout) View.INVISIBLE else View.VISIBLE

        val sizeStr = if (entry.size != PackageSize.UNKNOWN) Formatter.formatFileSize(context, entry.size) else null
        if (sizeStr != null) {
            holder.appSize.text = sizeStr
        } else {
            holder.appSize.setText(R.string.app_size_unknown)
        }

        val lastUsedText: String
        val unusedTime = entry.lastUsedTime - System.currentTimeMillis()
        if (entry.lastUsedTime != 0 || unusedTime < MillisecondsIn.DAY * 365 * 3) {
            lastUsedText = elapsedTime.format(entry.lastUsedTime, entry.ranIn)
        } else {
            lastUsedText = unknownUsageTime.format(entry.installTime)
        }
        holder.lastUsed.text = lastUsedText

        return convertView
    }
}

internal class ElapsedTimeFormatter(context: Context) {

    private val res: Resources
    private val pluralRes: Plural

    init {
        this.res = context.resources
        this.pluralRes = Plural(res)
    }

    fun format(lastUsed: Long, ranIn: AppEntry.RanIn?): String {
        if (lastUsed == 0) return res.getString(R.string.havent_seen_app_running)

        val now = System.currentTimeMillis()
        val diff = now - lastUsed

        val isToday = DateUtils.isToday(lastUsed)

        if (diff > MillisecondsIn.DAY || !isToday) {
            var count = (diff / MillisecondsIn.DAY).toInt()
            count = Math.max(1, count)
            return lastUsed(ranIn, R.plurals.day_count, count)
        }

        if (diff > 12 * MillisecondsIn.HOUR) {
            when (ranIn) {
                AppEntry.RanIn.BACKGROUND -> return res.getString(R.string.ran_in_background_today)
                AppEntry.RanIn.FOREGROUND,
                else -> return res.getString(R.string.used_today)
            }
        }

        if (diff > MillisecondsIn.HOUR) {
            val count = (diff / MillisecondsIn.HOUR).toInt()
            return lastUsed(ranIn, R.plurals.hour_count, count)
        }

        if (diff > 5 * MillisecondsIn.MINUTE) {
            val count = (diff / MillisecondsIn.MINUTE).toInt()
            return lastUsed(ranIn, R.plurals.minute_count, count)
        }

        when (ranIn) {
            AppEntry.RanIn.BACKGROUND -> return res.getString(R.string.ran_in_background_just)
            AppEntry.RanIn.FOREGROUND,
            else -> return res.getString(R.string.used_just)
        }
    }

    private fun lastUsed(ranIn: AppEntry.RanIn?, unitsPluralId: Int, quantity: Int): String {
        val ranInFormat: String
        when (ranIn) {
            AppEntry.RanIn.BACKGROUND -> ranInFormat = res.getString(R.string.ran_in_background_X_ago)
            AppEntry.RanIn.FOREGROUND,
            else -> ranInFormat = res.getString(R.string.used_by_user_X_ago)
        }
        val timeLeft = pluralRes.format(unitsPluralId, quantity, quantity)
        return String.format(ranInFormat, timeLeft)
    }
}

internal class UnknownUsageTimeFormatter(context: Context) {
    private val removerInstallTime = MyApplication.instance!!.installTime
    private val res: Resources
    private val pluralRes: Plural

    init {
        this.res = context.resources
        this.pluralRes = Plural(res)
    }

    fun format(installTime: Long): String {
        val timeInstalled = Math.max(installTime, removerInstallTime)
        val timeLeft = System.currentTimeMillis() - timeInstalled

        val days = (timeLeft / MillisecondsIn.DAY).toInt()
        if (days > 0) {
            val daysString = pluralRes.format(R.plurals.day_count, days, days)
            val format = res.getString(R.string.unused_at_least_X)
            return String.format(format, daysString)
        }
        return res.getString(R.string.havent_seen_app_running)
    }
}

internal class ApplicationViewHolder private constructor(context: Context) {

    val rootView: View
    val appName: TextView
    val appIcon: ImageView
    val appSize: TextView
    val lastUsed: TextView
    val doNotNotify: ImageView

    init {
        rootView = View.inflate(context, R.layout.list_item_application, null)
        appName = rootView.findViewById(R.id.app_name) as TextView
        appIcon = rootView.findViewById(R.id.app_icon) as ImageView
        appSize = rootView.findViewById(R.id.app_size) as TextView
        lastUsed = rootView.findViewById(R.id.app_last_used) as TextView
        doNotNotify = rootView.findViewById(R.id.app_dont_notify) as ImageView
        val drawable = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_do_not_disturb_black_24dp))
        DrawableCompat.setTint(drawable, 0x80000000.toInt())
        doNotNotify.setImageDrawable(drawable)
        rootView.tag = this
    }

    companion object {

        fun createOrReuse(context: Context, convertView: View?): ApplicationViewHolder {
            return if (convertView != null) {
                getFromView(convertView)
            } else ApplicationViewHolder(context)
        }

        fun getFromView(v: View): ApplicationViewHolder {
            return v.tag as ApplicationViewHolder
        }
    }
}
