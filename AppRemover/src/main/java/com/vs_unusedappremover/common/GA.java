package com.vs_unusedappremover.common;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;

import static java.lang.Thread.UncaughtExceptionHandler;

public class GA {

    private static final String TAG = "GA";
    private static EasyTracker sTracker;
    private static ExceptionParser exceptionParser;

    public static void init(String resourcesPackage, final Context context, final String... additionalPackages) {
        EasyTracker.setResourcePackageName(resourcesPackage);
        sTracker = EasyTracker.getInstance(context);
        exceptionParser = new GAExceptionParser(context, additionalPackages);

        final UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread thread, Throwable th) {
                reportException(thread, th, true);
                defaultHandler.uncaughtException(thread, th);
            }
        });
    }

    public static void setCustomDimension(int index, String value) {
        sTracker.set(Fields.customDimension(index), value);
        Log.i(TAG, "set dimension[" + index + "]=" + value);
    }

    public static void onActivityStart(Activity sender) {
        sTracker.activityStart(sender);
    }

    public static void onActivityStop(Activity sender) {
        sTracker.activityStop(sender);
    }

    public static void onFragmentShown(String tag) {
        sTracker.set(Fields.SCREEN_NAME, tag);
        sTracker.send(MapBuilder
                .createAppView()
                .build());
    }

    public static void event(String category, String action) {
        eventInternal(category, action, null, null);
    }

    public static void event(String category, String action, String description) {
        eventInternal(category, action, description, null);
    }

    public static void event(String category, String action, long value) {
        eventInternal(category, action, null, value);
    }

    public static void event(String category, String action, boolean value) {
        eventInternal(category, action, null, value ? 1L : 0L);
    }

    public static void timing(String category, long timeInMilliseconds, String name) {
        timing(category, timeInMilliseconds, name, null);
    }

    public static void timing(String category, long timeInMilliseconds, String name, String label) {
        sTracker.send(MapBuilder.createTiming(category, timeInMilliseconds, name, label).build());
        Log.i(TAG, "timing " + category + " time=\"" + timeInMilliseconds + "\" name=\"" + name + "\" label=" + label);
    }

    public static void reportException(Throwable th) {
        reportException(Thread.currentThread(), th, false);
    }

    public static void reportException(Thread thread, Throwable th, boolean fatal) {
        String description = exceptionParser.getDescription(thread.getName(), th);
        sTracker.send(MapBuilder
                .createException(description, false)
                .build());
    }

    private static void eventInternal(String category, String action, String description, Long value) {
        sTracker.send(MapBuilder.createEvent(category, action, description, value).build());
        Log.i(TAG, "event " + category + " action=\"" + action + "\" desc=\"" + description + "\" value=" + value);
    }

    private GA() { }
}