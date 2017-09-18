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

class SortActionProvider(private val context: Context) : ActionProvider(context) {

    interface OnSortSelectedListener {
        fun onSortSelected(source: SortActionProvider, order: OrderBy)
    }

    private val adapter: SortActionAdapter
    private var listener: OnSortSelectedListener? = null
    private var order = OrderBy.TIME_UNUSED

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

    fun setOrder(order: OrderBy?) {
        assert(order != null)
        this.order = order
    }

    fun getOrder(): OrderBy {
        return order
    }

    fun setOnSortSelectedListener(l: OnSortSelectedListener) {
        this.listener = l
    }

    private val onItemClick = OnItemClickListener { parent, view, position, id ->
        order = adapter.getItem(position)
        if (listener != null) {
            listener!!.onSortSelected(this@SortActionProvider, order)
        }
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



