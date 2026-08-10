package com.zploy.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lists launchable applications that can be bound to a Zploy mapping profile. */
public final class GameCatalog {
    public static final class AppEntry {
        public final String packageName;
        public final String label;
        public final Drawable icon;

        AppEntry(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private GameCatalog() {}

    public static List<AppEntry> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        List<AppEntry> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(context.getPackageName()) || !seen.add(pkg)) continue;
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            CharSequence label = ai.loadLabel(pm);
            out.add(new AppEntry(pkg, label == null ? pkg : label.toString(), ai.loadIcon(pm)));
        }
        out.sort(Comparator.comparing(a -> a.label.toLowerCase()));
        return out;
    }

    public static boolean launch(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launch == null) return false;
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return true;
    }
}
