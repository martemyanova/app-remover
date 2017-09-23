package com.vs_unusedappremover.actions

import android.content.Context
import android.support.v4.view.ActionProvider
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.TextView

import com.vs_unusedappremover.R
import com.vs_unusedappremover.data.OrderBy


typealias OnSortSelectedListener = (source: SortActionProvider, order: OrderBy) -> Unit

class SortActionProvider(context: Context) : ActionProvider(context) {

    private val adapter: SortActionAdapter
    private var listener: OnSortSelectedListener? = null
    var order = OrderBy.TIME_UNUSED
        set(value) {
            field = value
        }

    init {
        this.adapter = SortActionAdapter(context)
    }

    override fun onCreateActionView(): View {
        val activityChooserView = SortChooserView(context)

        activityChooserView.setProvider(this)

        activityChooserView.setAdapter(adapter)
        activityChooserView.setOnItemClickListener(onItemClick)

        return activityChooserView
    }

    fun setOnSortSelectedListener(l: OnSortSelectedListener) {
        this.listener = l
    }

    private val onItemClick = OnItemClickListener { _, _, position, _ ->
        order = adapter.getItem(position)
        listener?.invoke(this@SortActionProvider, order)
    }

    class SortActionAdapter(private val context: Context) : BaseAdapter() {

        override fun getCount(): Int {
            return OrderBy.values().size
        }

        override fun getItem(position: Int): OrderBy {
            return OrderBy.values()[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null || convertView.id != R.id.list_item) {
                convertView = LayoutInflater.from(context).inflate(R.layout.chooser_list_item, parent, false)
            }
            val order = getItem(position)

            val titleView = convertView!!.findViewById(R.id.title) as TextView
            titleView.setText(order.fullTextResId)

            return convertView
        }
    }
}



