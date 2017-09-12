package com.vs_unusedappremover;

import android.content.pm.ApplicationInfo;

public class AppEntry {

    public static enum RanIn {
        UNKNOWN(0),
        BACKGROUND(1),
        FOREGROUND(2);

        public final int id;

        public static RanIn byId(int id) {
            for (RanIn v : values()) {
                if (v.id == id) return v;
            }
            throw new IllegalArgumentException("No item with such id");
        }

        private RanIn(int id) {
            this.id = id;
        }
    }

    public String label;
    public long size;
    public ApplicationInfo info;
    public long lastUsedTime;
    public RanIn ranIn;
    public long installTime;
    public float rating;
    public String downloadCount;
    public boolean notifyAbout = true;
}
