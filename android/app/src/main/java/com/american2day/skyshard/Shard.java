package com.american2day.skyshard;

import android.graphics.drawable.Drawable;

/** One cloned instance of an installed app, living inside the SkyShard space. */
public class Shard {
    public final String packageName;
    public final String label;
    public final Drawable icon;
    public final boolean frozen;

    public Shard(String packageName, String label, Drawable icon, boolean frozen) {
        this.packageName = packageName;
        this.label = label;
        this.icon = icon;
        this.frozen = frozen;
    }
}
