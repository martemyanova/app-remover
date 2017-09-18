package com.vs_unusedappremover.data

import android.content.res.Resources
import android.util.Log

import com.seppius.i18n.plurals.PluralResources
import com.vs_unusedappremover.MyApplication

class Plural(private val res: Resources) {
    private var pluralRes: PluralResources? = null

    init {
        try {
            this.pluralRes = PluralResources(res)
        } catch (e: Exception) {
            Log.e(MyApplication.TAG, "unable to create plural resources", e)
            this.pluralRes = null
        }

    }

    fun format(pluralResId: Int, quantity: Int, vararg args: Any): String {
        return if (pluralRes != null) {
            pluralRes!!.getQuantityString(pluralResId, quantity, *args)
        } else {
            res.getQuantityString(pluralResId, quantity, *args)
        }
    }
}
