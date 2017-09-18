package com.vs_unusedappremover

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri

import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

import java.io.IOException

internal object AppIcon {

    private val SCHEME = "appicon"

    fun createRequestHandler(context: Context): RequestHandler {
        return object : RequestHandler() {
            override fun canHandleRequest(data: Request): Boolean {
                return data.uri != null && data.uri.scheme == SCHEME
            }

            @Throws(IOException::class)
            override fun load(request: Request, networkPolicy: Int): RequestHandler.Result {
                val appPackage = request.uri.authority
                try {
                    val packageManager = context.packageManager
                    val info = packageManager.getApplicationInfo(appPackage, 0)
                    val d = info.loadIcon(packageManager) as BitmapDrawable
                    return RequestHandler.Result(d.bitmap, Picasso.LoadedFrom.DISK)
                } catch (e: Exception) {
                    throw IOException(e)
                }

            }
        }
    }

    fun buildUrl(packageName: String): Uri {
        return Uri.parse(SCHEME + "://" + packageName)
    }
}
