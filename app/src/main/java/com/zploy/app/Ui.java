package com.zploy.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(243, 243, 243);
    public static final int WHITE = Color.WHITE;
    public static final int BLACK = Color.rgb(17, 17, 17);
    public static final int GRAY = Color.rgb(119, 119, 119);
    public static final int LIGHT = Color.rgb(232, 232, 232);

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    public static TextView button(Context c, String value, boolean dark) {
        TextView t = text(c, value, 14, dark ? WHITE : BLACK, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(dark ? BLACK : WHITE, 18, c));
        t.setClickable(true);
        t.setFocusable(true);
        t.setPadding(dp(c, 16), 0, dp(c, 16), 0);
        t.setMinHeight(dp(c, 52));
        return t;
    }

    public static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static void margin(View v, int l, int t, int r, int b) {
        ViewGroup.LayoutParams raw = v.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) raw;
            p.setMargins(l, t, r, b);
            v.setLayoutParams(p);
        }
    }
}
