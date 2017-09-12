package com.vs_unusedappremover;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.IOException;

class AppIcon {

    private static final String SCHEME = "appicon";

    static RequestHandler createRequestHandler(final Context context) {
        return new RequestHandler() {
            @Override
            public boolean canHandleRequest(Request data) {
                return data.uri != null && data.uri.getScheme().equals(SCHEME);
            }

            @Override
            public Result load(Request request, int networkPolicy) throws IOException {
                String appPackage = request.uri.getAuthority();
                try {
                    PackageManager packageManager = context.getPackageManager();
                    ApplicationInfo info = packageManager.getApplicationInfo(appPackage, 0);
                    BitmapDrawable d = (BitmapDrawable) info.loadIcon(packageManager);
                    return new Result(d.getBitmap(), Picasso.LoadedFrom.DISK);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
    }

    static Uri buildUrl(String packageName) {
        return Uri.parse(SCHEME + "://" + packageName);
    }
}
