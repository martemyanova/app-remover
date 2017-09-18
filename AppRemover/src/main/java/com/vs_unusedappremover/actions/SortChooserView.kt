package com.vs_unusedappremover.actions

import android.content.Context
import android.content.res.Resources
import android.support.v7.widget.ListPopupWindow
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter
import android.widget.PopupWindow
import android.widget.TextView

import com.vs_unusedappremover.R
import com.vs_unusedappremover.actions.SortActionProvider.SortActionAdapter
import com.vs_unusedappremover.common.GA

internal class SortChooserView @JvmOverloads constructor(private val mContext: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ViewGroup(mContext, attrs, defStyle) {

    private var mAdapter: SortActionAdapter? = null
    private val mButton: View
    private val mTextView: TextView
    private var itemListener: AdapterView.OnItemClickListener? = null

    private val mListPopupMaxWidth: Int
    private var mProvider: SortActionProvider? = null

    private val mOnGlobalLayoutListener = OnGlobalLayoutListener {
        if (isShowingPopup) {
            if (!isShown) {
                listPopupWindow.dismiss()
            } else {
                listPopupWindow.show()
                if (mProvider != null) {
                    mProvider!!.subUiVisibilityChanged(true)
                }
            }
        }
    }

    private var mListPopupWindow: ListPopupWindow? = null

    private var mIsAttachedToWindow: Boolean = false

    init {

        val inflater = LayoutInflater.from(mContext)
        inflater.inflate(R.layout.chooser_view, this, true)

        mButton = findViewById(R.id.expand_activities_button)
        mButton.setOnClickListener(onButtonClick)

        mTextView = findViewById(R.id.text) as TextView

        val resources = mContext.resources
        var size = 0
        try {
            size = resources.getDimensionPixelSize(android.support.v7.appcompat.R.dimen.abc_dialog_fixed_width_major)
        } catch (e: Resources.NotFoundException) {
            GA.reportException(e)
        }

        mListPopupMaxWidth = Math.max(resources.displayMetrics.widthPixels / 2, size)
    }

    fun setAdapter(adapter: SortActionAdapter) {
        this.mAdapter = adapter
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemListener = listener
    }

    fun setProvider(provider: SortActionProvider) {
        mProvider = provider
        setTextResource(mProvider!!.order.shortTextResId)
        this.requestLayout()
    }

    fun showPopup(): Boolean {
        if (isShowingPopup || !mIsAttachedToWindow) {
            return false
        }

        showPopupUnchecked()
        return true
    }

    private fun showPopupUnchecked() {

        viewTreeObserver.addOnGlobalLayoutListener(mOnGlobalLayoutListener)

        val popupWindow = listPopupWindow
        if (!popupWindow.isShowing) {
            val contentWidth = Math.min(measureContentWidth(mAdapter), mListPopupMaxWidth)
            popupWindow.setContentWidth(contentWidth)
            popupWindow.show()
            if (mProvider != null) {
                mProvider!!.subUiVisibilityChanged(true)
            }
            popupWindow.listView.contentDescription = mContext.getString(R.string.menu_sort)
        }

        setTextResource(R.string.order_by)
    }

    fun dismissPopup(): Boolean {
        if (isShowingPopup) {
            listPopupWindow.dismiss()
            val viewTreeObserver = viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeGlobalOnLayoutListener(mOnGlobalLayoutListener)
            }
        }
        return true
    }

    val isShowingPopup: Boolean
        get() = listPopupWindow.isShowing

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mIsAttachedToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        val viewTreeObserver = viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeGlobalOnLayoutListener(mOnGlobalLayoutListener)
        }
        mIsAttachedToWindow = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = mButton
        // If the default action is not visible we want to be as tall as the
        // ActionBar so if this widget is used in the latter it will look as
        // a normal action button.
        measureChild(child, widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(child.measuredWidth, child.measuredHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        mButton.layout(0, 0, right - left, bottom - top)
        if (listPopupWindow.isShowing) {
            showPopupUnchecked()
        } else {
            dismissPopup()
        }
    }

    private val listPopupWindow: ListPopupWindow
        get() {
            if (mListPopupWindow == null) {
                mListPopupWindow = ListPopupWindow(context)
                mListPopupWindow!!.setAdapter(mAdapter)
                mListPopupWindow!!.anchorView = this@SortChooserView
                mListPopupWindow!!.isModal = true
                mListPopupWindow!!.setOnItemClickListener(onItemClick)
                mListPopupWindow!!.setOnDismissListener(onPopupDismissed)
            }
            return mListPopupWindow
        }

    private fun measureContentWidth(adapter: BaseAdapter?): Int {

        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val count = adapter!!.count

        var contentWidth = 0
        var itemView: View? = null
        for (i in 0..count - 1) {
            itemView = adapter.getView(i, itemView, null)
            itemView!!.measure(widthMeasureSpec, heightMeasureSpec)
            contentWidth = Math.max(contentWidth, itemView.measuredWidth)
        }
        return contentWidth
    }

    private fun setTextResource(resId: Int) {
        mTextView.setText(resId)
        requestLayout()
    }

    private val onItemClick = OnItemClickListener { parent, view, position, id ->
        if (itemListener != null) {
            itemListener!!.onItemClick(parent, view, position, id)
        }
        dismissPopup()
    }

    private val onButtonClick = OnClickListener { showPopupUnchecked() }

    private val onPopupDismissed = PopupWindow.OnDismissListener {
        setTextResource(mProvider!!.order.shortTextResId)
        if (mProvider != null) {
            mProvider!!.subUiVisibilityChanged(false)
        }
    }
}
