package net.londatiga.android

import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.net.Uri

/**
 * Action item, displayed as menu with icon and text.
 *
 * @author Lorensius. W. L. T <lorenz></lorenz>@londatiga.net>
 *
 * Contributors:
 * - Kevin Peck <kevinwpeck></kevinwpeck>@gmail.com>
 */
class ActionItem {
    /**
     * Get action icon
     * @return  [Drawable] action icon
     */
    /**
     * Set action icon
     *
     * @param icon [Drawable] action icon
     */
    var icon: Drawable? = null
    val iconUri: Uri
    /**
     * Get thumb image
     *
     * @return Thumb image
     */
    /**
     * Set thumb
     *
     * @param thumb Thumb image
     */
    var thumb: Bitmap? = null
    /**
     * Get action title
     *
     * @return action title
     */
    /**
     * Set action title
     *
     * @param title action title
     */
    var title: String? = null
    /**
     * @return  Our action id
     */
    /**
     * Set action id
     *
     * @param actionId  Action id for this action
     */
    var actionId = -1
    /**
     * Check if item is selected
     *
     * @return true or false
     */
    /**
     * Set selected flag;
     *
     * @param selected Flag to indicate the item is selected
     */
    var isSelected: Boolean = false
    /**
     * @return  true if button is sticky, menu stays visible after press
     */
    /**
     * Set sticky status of button
     *
     * @param sticky  true for sticky, pop up sends event but does not disappear
     */
    var isSticky: Boolean = false

    constructor(actionId: Int, title: String, iconUri: Uri) {
        this.actionId = actionId
        this.title = title
        this.iconUri = iconUri
    }

    /**
     * Constructor
     *
     * @param actionId  Action id for case statements
     * @param title     Title
     * @param icon      Icon to use
     */
    @JvmOverloads constructor(actionId: Int, title: String?, icon: Drawable = null) {
        this.title = title
        this.icon = icon
        this.actionId = actionId
    }

    /**
     * Constructor
     *
     * @param icon [Drawable] action icon
     */
    constructor(icon: Drawable) : this(-1, null, icon) {}

    /**
     * Constructor
     *
     * @param actionId  Action ID of item
     * @param icon      [Drawable] action icon
     */
    constructor(actionId: Int, icon: Drawable) : this(actionId, null, icon) {}
}
/**
 * Constructor
 *
 * @param actionId  Action id of the item
 * @param title     Text to show for the item
 */