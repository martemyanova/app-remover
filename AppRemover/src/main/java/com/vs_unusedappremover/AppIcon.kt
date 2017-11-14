package com.vs_unusedappremover

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.util.Log

import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

import java.io.IOException

internal object AppIcon {

    val SCHEME = "appicon"
    private val TAG = AppIcon.javaClass.simpleName

    fun createRequestHandler(context: Context): RequestHandler {
        return object : RequestHandler() {
            override fun canHandleRequest(data: Request): Boolean {
                return data.uri != null && data.uri.scheme == SCHEME
            }

            @Throws(IOException::class)
            override fun load(request: Request, networkPolicy: Int): RequestHandler.Result {
                val appPackage = request.uri.authority
                val packageManager = context.packageManager
                val bitmap = packageManager.getAppIcon(appPackage)
                return RequestHandler.Result(bitmap, Picasso.LoadedFrom.DISK)

            }
        }
    }

    fun PackageManager.getAppIcon(packageName: String): Bitmap {

        try {
            val drawable = this.getApplicationIcon(packageName)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
                val layerDrawable = LayerDrawable(arrayOf(drawable.background, drawable.foreground))

                val resBitmap = Bitmap.createBitmap(
                        layerDrawable.intrinsicWidth,
                        layerDrawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resBitmap)
                layerDrawable.setBounds(0, 0, canvas.width, canvas.height)
                layerDrawable.draw(canvas)

                resBitmap
            }
            else {
                (drawable as BitmapDrawable).bitmap
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message)
            throw IOException(e)
        }
    }
}
