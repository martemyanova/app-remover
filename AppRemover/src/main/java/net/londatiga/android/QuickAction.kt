package net.londatiga.android

import android.content.Context

import android.graphics.Rect
import android.graphics.drawable.Drawable

import android.net.Uri
import android.util.DisplayMetrics
import android.widget.ImageView
import android.widget.TextView
import android.widget.ScrollView
import android.widget.RelativeLayout
import android.widget.PopupWindow.OnDismissListener

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup
import java.util.ArrayList

import com.vs_unusedappremover.MyApplication
import com.vs_unusedappremover.R

/**
 * QuickAction dialog, shows action list as icon and text like the one in Gallery3D app. Currently supports vertical
 * and horizontal layout.
 *
 * @author Lorensius W. L. T <lorenz></lorenz>@londatiga.net>
 *
 * Contributors:
 * - Kevin Peck <kevinwpeck></kevinwpeck>@gmail.com>
 */
class QuickAction
/**
 * Constructor allowing orientation override
 *
 * @param context    Context
 * @param orientation Layout orientation, can be vartical or horizontal
 */
@JvmOverloads constructor(context: Context, private val mOrientation: Int = VERTICAL) : PopupWindows(context), OnDismissListener {

    private var mRootView: View? = null
    private var mArrowUp: ImageView? = null
    private var mArrowDown: ImageView? = null
    private val mInflater: LayoutInflater
    private var mTrack: ViewGroup? = null
    private var mScroller: ScrollView? = null
    private var mItemClickListener: OnActionItemClickListener? = null
    private var mDismissListener: OnDismissListener? = null

    private val actionItems = ArrayList<ActionItem>()

    private var mDidAction: Boolean = false

    private var mChildPos: Int = 0
    private var mInsertPos: Int = 0
    private var mAnimStyle: Int = 0
    private var rootWidth = 0

    init {

        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        if (mOrientation == HORIZONTAL) {
            setRootViewId(R.layout.popup_horizontal)
        } else {
            setRootViewId(R.layout.popup_vertical)
        }

        mAnimStyle = ANIM_AUTO
        mChildPos = 0
    }

    /**
     * Get action item at an index
     *
     * @param index  Index of item (position from callback)
     *
     * @return  Action Item at the position
     */
    fun getActionItem(index: Int): ActionItem {
        return actionItems[index]
    }

    /**
     * Set root view.
     *
     * @param id Layout resource id
     */
    fun setRootViewId(id: Int) {
        mRootView = mInflater.inflate(id, null) as ViewGroup
        mTrack = mRootView!!.findViewById(R.id.tracks)

        mArrowDown = mRootView!!.findViewById(R.id.arrow_down)
        mArrowUp = mRootView!!.findViewById(R.id.arrow_up)

        mScroller = mRootView!!.findViewById(R.id.scroller)

        //This was previously defined on show() method, moved here to prevent force close that occured
        //when tapping fastly on a view to show quickaction dialog.
        //Thanx to zammbi (github.com/zammbi)
        mRootView!!.setLayoutParams(LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        setContentView(mRootView)
    }

    /**
     * Set animation style
     *
     * @param mAnimStyle animation style, default is set to ANIM_AUTO
     */
    fun setAnimStyle(mAnimStyle: Int) {
        this.mAnimStyle = mAnimStyle
    }

    /**
     * Set listener for action item clicked.
     *
     * @param listener Listener
     */
    fun setOnActionItemClickListener(listener: OnActionItemClickListener) {
        mItemClickListener = listener
    }

    /**
     * Add action item
     *
     * @param action  [ActionItem]
     */
    fun addActionItem(action: ActionItem) {
        actionItems.add(action)

        val title = action.title
        val icon = action.icon

        val container: View

        if (mOrientation == HORIZONTAL) {
            container = mInflater.inflate(R.layout.action_item_horizontal, null)
        } else {
            container = mInflater.inflate(R.layout.action_item_vertical, null)
        }

        val img = container.findViewById(R.id.iv_icon) as ImageView
        val text = container.findViewById(R.id.tv_title) as TextView

        val iconUri = action.iconUri
        if (icon != null) {
            img.setImageDrawable(icon)
        } else if (iconUri != null) {
            val metrics = img.resources.displayMetrics
            val size = (ICON_SIZE_DP * metrics.density).toInt()
            MyApplication.instance!!.picasso()!!.load(iconUri).resize(size, size).into(img)
        } else {
            img.visibility = View.GONE
        }

        if (title != null) {
            text.text = title
        } else {
            text.visibility = View.GONE
        }

        val pos = mChildPos
        val actionId = action.actionId

        container.setOnClickListener {
            if (mItemClickListener != null) {
                mItemClickListener!!.onItemClick(this@QuickAction, pos, actionId)
            }

            if (!getActionItem(pos).isSticky) {
                mDidAction = true

                dismiss()
            }
        }

        container.isFocusable = true
        container.isClickable = true

        if (mOrientation == HORIZONTAL && mChildPos != 0) {
            val separator = mInflater.inflate(R.layout.horiz_separator, null)

            val params = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)

            separator.layoutParams = params
            separator.setPadding(5, 0, 5, 0)

            mTrack!!.addView(separator, mInsertPos)

            mInsertPos++
        }

        mTrack!!.addView(container, mInsertPos)

        mChildPos++
        mInsertPos++
    }

    /**
     * Show quickaction popup. Popup is automatically positioned, on top or bottom of anchor view.
     *
     */
    fun show(anchor: View) {
        preShow()

        var xPos: Int
        val yPos: Int
        val arrowPos: Int

        mDidAction = false

        val location = IntArray(2)

        anchor.getLocationOnScreen(location)

        val anchorRect = Rect(location[0], location[1], location[0] + anchor.width, location[1] + anchor.height)

        // mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
        // LayoutParams.WRAP_CONTENT));

        mRootView!!.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        val rootHeight = mRootView!!.getMeasuredHeight()

        rootWidth = mRootView!!.getMeasuredWidth()

        val screenWidth = mWindowManager.defaultDisplay.width
        val screenHeight = mWindowManager.defaultDisplay.height

        // automatically get X coord of popup (top left)
        if (anchorRect.left + rootWidth > screenWidth) {
            xPos = anchorRect.left - (rootWidth - anchor.width)
            xPos = if (xPos < 0) 0 else xPos

            arrowPos = anchorRect.centerX() - xPos

        } else {
            if (anchor.width > rootWidth) {
                xPos = anchorRect.centerX() - rootWidth / 2
            } else {
                xPos = anchorRect.left
            }

            arrowPos = anchorRect.centerX() - xPos
        }

        val dyTop = anchorRect.top
        val dyBottom = screenHeight - anchorRect.bottom

        val onTop = if (dyTop > dyBottom) true else false

        if (onTop) {
            if (rootHeight > dyTop) {
                yPos = 15
                val l = mScroller!!.layoutParams
                l.height = dyTop - anchor.height
            } else {
                yPos = anchorRect.top - rootHeight
            }
        } else {
            yPos = anchorRect.bottom

            if (rootHeight > dyBottom) {
                val l = mScroller!!.layoutParams
                l.height = dyBottom
            }
        }

        showArrow(if (onTop) R.id.arrow_down else R.id.arrow_up, arrowPos)

        setAnimationStyle(screenWidth, anchorRect.centerX(), onTop)

        mWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, xPos, yPos)
    }

    /**
     * Set animation style
     *
     * @param screenWidth screen width
     * @param requestedX distance from left edge
     * @param onTop flag to indicate where the popup should be displayed. Set TRUE if displayed on top of anchor view
     * and vice versa
     */
    private fun setAnimationStyle(screenWidth: Int, requestedX: Int, onTop: Boolean) {
        val arrowPos = requestedX - mArrowUp!!.measuredWidth / 2

        when (mAnimStyle) {
            ANIM_GROW_FROM_LEFT -> mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Left else R.style.Animations_PopDownMenu_Left

            ANIM_GROW_FROM_RIGHT -> mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Right else R.style.Animations_PopDownMenu_Right

            ANIM_GROW_FROM_CENTER -> mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Center else R.style.Animations_PopDownMenu_Center

            ANIM_REFLECT -> mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Reflect else R.style.Animations_PopDownMenu_Reflect

            ANIM_AUTO -> if (arrowPos <= screenWidth / 4) {
                mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Left else R.style.Animations_PopDownMenu_Left
            } else if (arrowPos > screenWidth / 4 && arrowPos < 3 * (screenWidth / 4)) {
                mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Center else R.style.Animations_PopDownMenu_Center
            } else {
                mWindow.animationStyle = if (onTop) R.style.Animations_PopUpMenu_Right else R.style.Animations_PopDownMenu_Right
            }
        }
    }

    /**
     * Show arrow
     *
     * @param whichArrow arrow type resource id
     * @param requestedX distance from left screen
     */
    private fun showArrow(whichArrow: Int, requestedX: Int) {
        val showArrow = if (whichArrow == R.id.arrow_up) mArrowUp else mArrowDown
        val hideArrow = if (whichArrow == R.id.arrow_up) mArrowDown else mArrowUp

        val arrowWidth = mArrowUp!!.measuredWidth

        showArrow!!.visibility = View.VISIBLE

        val param = showArrow.layoutParams as ViewGroup.MarginLayoutParams

        param.leftMargin = requestedX - arrowWidth / 2

        hideArrow!!.visibility = View.INVISIBLE
    }

    /**
     * Set listener for window dismissed. This listener will only be fired if the quicakction dialog is dismissed
     * by clicking outside the dialog or clicking on sticky item.
     */
    fun setOnDismissListener(listener: QuickAction.OnDismissListener) {
        setOnDismissListener(this)

        mDismissListener = listener
    }

    override fun onDismiss() {
        if (!mDidAction && mDismissListener != null) {
            mDismissListener!!.onDismiss()
        }
    }

    /**
     * Listener for item click
     *
     */
    interface OnActionItemClickListener {
        fun onItemClick(source: QuickAction, pos: Int, actionId: Int)
    }

    /**
     * Listener for window dismiss
     *
     */
    interface OnDismissListener {
        fun onDismiss()
    }

    companion object {

        val ICON_SIZE_DP = 48f

        val HORIZONTAL = 0
        val VERTICAL = 1

        val ANIM_GROW_FROM_LEFT = 1
        val ANIM_GROW_FROM_RIGHT = 2
        val ANIM_GROW_FROM_CENTER = 3
        val ANIM_REFLECT = 4
        val ANIM_AUTO = 5
    }
}
/**
 * Constructor for default vertical layout
 *
 * @param context  Context
 */