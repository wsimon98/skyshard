package com.american2day.skyshard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lists every user-installed app that has a launcher entry. */
public class AppRepo {

    public static List<AppInfo> listLaunchableApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolves = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        List<AppInfo> apps = new ArrayList<>();

        for (ResolveInfo r : resolves) {
            String pkg = r.activityInfo.packageName;
            if (seen.contains(pkg)) continue;
            seen.add(pkg);
            if (pkg.equals(ctx.getPackageName())) continue;
            try {
                CharSequence label = r.loadLabel(pm);
                apps.add(new AppInfo(
                        pkg,
                        label == null ? pkg : label.toString(),
                        r.loadIcon(pm)));
            } catch (Exception ignored) {
            }
        }
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return apps;
    }
}
