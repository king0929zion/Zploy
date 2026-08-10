package com.zploy.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/** Minimal front-view visualization tuned for the GameSir Nova 2 Lite layout. */
public final class ControllerView extends View implements ControllerStore.Listener {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ControllerState state = ControllerStore.get().snapshot();
    private final Map<String, Float> points = new HashMap<>();

    public ControllerView(Context context) {
        super(context);
        setMinimumHeight(Ui.dp(context, 260));
        ControllerStore.get().addListener(this);
    }

    @Override protected void onDetachedFromWindow() {
        ControllerStore.get().removeListener(this);
        super.onDetachedFromWindow();
    }

    @Override public void onControllerState(ControllerState state) {
        this.state = state;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(232, 232, 232));
        Path body = new Path();
        body.moveTo(w * .12f, h * .24f);
        body.quadTo(w * .05f, h * .34f, w * .10f, h * .72f);
        body.quadTo(w * .15f, h * .95f, w * .33f, h * .76f);
        body.quadTo(cx, h * .82f, w * .67f, h * .76f);
        body.quadTo(w * .85f, h * .95f, w * .90f, h * .72f);
        body.quadTo(w * .95f, h * .34f, w * .88f, h * .24f);
        body.quadTo(cx, h * .06f, w * .12f, h * .24f);
        c.drawPath(body, paint);

        stick(c, w*.27f, h*.32f, Math.min(w,h)*.105f, state.lx, state.ly, "L");
        dpad(c, w*.29f, h*.60f, Math.min(w,h)*.105f);
        stick(c, w*.55f, h*.61f, Math.min(w,h)*.105f, state.rx, state.ry, "R");

        button(c, w*.78f, h*.45f, "X", pressed(android.view.KeyEvent.KEYCODE_BUTTON_X));
        button(c, w*.85f, h*.37f, "Y", pressed(android.view.KeyEvent.KEYCODE_BUTTON_Y));
        button(c, w*.85f, h*.54f, "B", pressed(android.view.KeyEvent.KEYCODE_BUTTON_B));
        button(c, w*.78f, h*.61f, "A", pressed(android.view.KeyEvent.KEYCODE_BUTTON_A));

        smallButton(c, w*.48f, h*.42f, "M", pressed(android.view.KeyEvent.KEYCODE_BUTTON_SELECT));
        smallButton(c, w*.59f, h*.43f, "○", pressed(android.view.KeyEvent.KEYCODE_BUTTON_MODE));
        smallButton(c, w*.68f, h*.40f, "≡", pressed(android.view.KeyEvent.KEYCODE_BUTTON_START));

        triggerBar(c, w*.13f, h*.13f, w*.28f, state.lt, "LT");
        triggerBar(c, w*.72f, h*.13f, w*.87f, state.rt, "RT");

        paint.setColor(Color.rgb(90,90,90));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(Ui.dp(getContext(), 11));
        c.drawText("GAMESIR", cx, h*.28f, paint);
    }

    private boolean pressed(int code) { return state.pressed(code); }

    private void stick(Canvas c, float x, float y, float r, float ax, float ay, String label) {
        paint.setColor(Color.rgb(210,210,210)); c.drawCircle(x,y,r,paint);
        paint.setColor(Color.rgb(30,30,30)); c.drawCircle(x + ax*r*.46f, y + ay*r*.46f, r*.54f, paint);
        paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(r*.28f);
        c.drawText(label, x + ax*r*.46f, y + ay*r*.46f + r*.1f, paint);
    }

    private void dpad(Canvas c, float x, float y, float r) {
        paint.setColor(Color.rgb(150,150,150));
        float s = r*.55f;
        c.drawRoundRect(x-s*.45f,y-r,x+s*.45f,y+r, s*.25f,s*.25f,paint);
        c.drawRoundRect(x-r,y-s*.45f,x+r,y+s*.45f, s*.25f,s*.25f,paint);
        paint.setColor(Color.rgb(70,70,70));
        if (state.hatY < -.5f || pressed(android.view.KeyEvent.KEYCODE_DPAD_UP)) c.drawCircle(x,y-r*.62f,r*.16f,paint);
        if (state.hatY > .5f || pressed(android.view.KeyEvent.KEYCODE_DPAD_DOWN)) c.drawCircle(x,y+r*.62f,r*.16f,paint);
        if (state.hatX < -.5f || pressed(android.view.KeyEvent.KEYCODE_DPAD_LEFT)) c.drawCircle(x-r*.62f,y,r*.16f,paint);
        if (state.hatX > .5f || pressed(android.view.KeyEvent.KEYCODE_DPAD_RIGHT)) c.drawCircle(x+r*.62f,y,r*.16f,paint);
    }

    private void button(Canvas c, float x, float y, String text, boolean down) {
        float r = Ui.dp(getContext(), 19);
        paint.setColor(down ? Color.rgb(17,17,17) : Color.rgb(205,205,205)); c.drawCircle(x,y,r,paint);
        paint.setColor(down ? Color.WHITE : Color.rgb(45,45,45)); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(Ui.dp(getContext(),14));
        c.drawText(text,x,y+Ui.dp(getContext(),5),paint);
    }

    private void smallButton(Canvas c, float x, float y, String text, boolean down) {
        float rw=Ui.dp(getContext(),19), rh=Ui.dp(getContext(),11);
        paint.setColor(down ? Color.rgb(17,17,17) : Color.rgb(205,205,205)); c.drawRoundRect(x-rw,y-rh,x+rw,y+rh,rh,rh,paint);
        paint.setColor(down ? Color.WHITE : Color.rgb(80,80,80)); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(Ui.dp(getContext(),10));
        c.drawText(text,x,y+Ui.dp(getContext(),4),paint);
    }

    private void triggerBar(Canvas c, float l, float y, float r, float value, String label) {
        float top=y-Ui.dp(getContext(),7), bottom=y+Ui.dp(getContext(),7);
        paint.setColor(Color.rgb(215,215,215)); c.drawRoundRect(l,top,r,bottom,Ui.dp(getContext(),7),Ui.dp(getContext(),7),paint);
        paint.setColor(Color.rgb(35,35,35)); c.drawRoundRect(l,top,l+(r-l)*MappingMath.clamp(value,0f,1f),bottom,Ui.dp(getContext(),7),Ui.dp(getContext(),7),paint);
        paint.setColor(Color.rgb(80,80,80)); paint.setTextSize(Ui.dp(getContext(),9)); paint.setTextAlign(Paint.Align.CENTER); c.drawText(label,(l+r)/2,y-Ui.dp(getContext(),12),paint);
    }
}
