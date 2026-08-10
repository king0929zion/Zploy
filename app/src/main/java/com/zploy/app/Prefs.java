package com.zploy.app;

import android.app.LocaleManager;
import android.content.Context;
import android.os.LocaleList;

public final class Prefs {
    private static final String FILE = "zploy";
    public static final String BACKEND_AUTO = "auto";
    public static final String BACKEND_SHIZUKU = "shizuku";
    public static final String BACKEND_ACCESSIBILITY = "accessibility";

    private Prefs() {}

    public static boolean mappingEnabled(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean("mapping", false);
    }

    public static void setMappingEnabled(Context c, boolean v) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean("mapping", v).apply();
    }

    public static float overlayAlpha(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getFloat("overlayAlpha", 0.18f);
    }

    public static void setOverlayAlpha(Context c, float v) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putFloat("overlayAlpha", v).apply();
    }

    public static String backend(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("backend", BACKEND_AUTO);
    }

    public static void setBackend(Context c, String value) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("backend", value).apply();
    }

    public static void setLanguage(Context context, String tag) {
        LocaleManager lm = context.getSystemService(LocaleManager.class);
        if (lm != null) lm.setApplicationLocales(tag.isEmpty() ? LocaleList.getEmptyLocaleList() : LocaleList.forLanguageTags(tag));
    }
}
