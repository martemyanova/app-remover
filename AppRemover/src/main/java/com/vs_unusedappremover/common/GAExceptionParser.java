package com.vs_unusedappremover.common;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.Log;

import java.util.Collection;
import java.util.TreeSet;

import static java.util.Arrays.asList;

public class GAExceptionParser implements ExceptionParser {

    private final TreeSet<String> mIncludedPackages = new TreeSet<>();

    public GAExceptionParser(Context context, String... additionalPackages) {
        setIncludedPackages(context, asList(additionalPackages));
    }

    public void setIncludedPackages(Context context, Collection<String> additionalPackages) {
        // using tree set here, because it sorted in ascending order, which is viable for the
        // underlying loop correctness
        TreeSet<String> packages = getApplicationComponentPackages(context);
        packages.addAll(additionalPackages);

        mIncludedPackages.clear();
        for (String packageName : packages) {
            if (!isSubpackageOfIncluded(packageName)) {
                mIncludedPackages.add(packageName);
            }
        }
    }

    private boolean isSubpackageOfIncluded(String packageName) {
        for (String includedName : mIncludedPackages) {
            if (packageName.startsWith(includedName)) {
                return true;
            }
        }
        return false;
    }

    private static TreeSet<String> getApplicationComponentPackages(Context context) {
        TreeSet<String> packages = new TreeSet<String>();
        if (context != null) {
            try {
                Context appContext = context.getApplicationContext();
                String appPackage = appContext.getPackageName();
                packages.add(appPackage);
                PackageInfo info = appContext.getPackageManager().getPackageInfo(appPackage,
                        PackageManager.GET_ACTIVITIES |
                                PackageManager.GET_RECEIVERS |
                                PackageManager.GET_SERVICES |
                                PackageManager.GET_PROVIDERS);

                ActivityInfo[] androidAppComponents = info.activities;
                if (androidAppComponents != null) {
                    for (ActivityInfo activityInfo : androidAppComponents) {
                        packages.add(activityInfo.packageName);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i("No package found");
            }
        }
        return packages;
    }

    protected Throwable getCause(Throwable t) {
        Throwable result = t;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    protected StackTraceElement getBestStackTraceElement(Throwable t) {
        StackTraceElement[] elements = t.getStackTrace();
        if (elements == null || elements.length == 0) {
            return null;
        }

        for (StackTraceElement e : elements) {
            String className = e.getClassName();
            if (isSubpackageOfIncluded(className)) {
                return e;
            }
        }
        return elements[0];
    }

    protected String getDescription(Throwable cause, StackTraceElement element, String threadName) {
        StringBuilder descriptionBuilder = new StringBuilder();
        descriptionBuilder.append(cause.getClass().getSimpleName());
        if (element != null) {
            descriptionBuilder.append(String.format(" (@%s:%s:%s)",
                    element.getClassName(),
                    element.getMethodName(),
                    element.getLineNumber()));
        }

        // It seems to be not usefull
        /* if (threadName != null) {
            descriptionBuilder.append(String.format(" {%s}", new Object[]{threadName}));
        }*/
        descriptionBuilder.append(String.format(" SDK-%d '%s %s'",
                android.os.Build.VERSION.SDK_INT,
                android.os.Build.MANUFACTURER,
                android.os.Build.MODEL));

        return descriptionBuilder.toString();
    }

    public String getDescription(String threadName, Throwable t) {
        Throwable cause = getCause(t);
        return getDescription(cause, getBestStackTraceElement(cause), threadName);
    }
}
