package com.zploy.app;

import android.view.KeyEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ControllerState {
    private static final float DIGITAL_TRIGGER_THRESHOLD = 0.55f;
    private static final float DIGITAL_HAT_THRESHOLD = 0.50f;

    public final String deviceName;
    public final int deviceId;
    public final float lx, ly, rx, ry, lt, rt, hatX, hatY;
    public final Set<Integer> pressedKeys;
    public final int lastKeyCode;

    public ControllerState(String deviceName, int deviceId,
                           float lx, float ly, float rx, float ry,
                           float lt, float rt, float hatX, float hatY,
                           Set<Integer> pressedKeys, int lastKeyCode) {
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.lx = lx;
        this.ly = ly;
        this.rx = rx;
        this.ry = ry;
        this.lt = lt;
        this.rt = rt;
        this.hatX = hatX;
        this.hatY = hatY;
        this.pressedKeys = Collections.unmodifiableSet(new HashSet<>(pressedKeys));
        this.lastKeyCode = lastKeyCode;
    }

    /**
     * Returns a digital button state. Some Android controller modes expose the
     * triggers and D-pad only as axes, so Zploy synthesizes their digital state
     * here. That keeps mappings stable across GameSir HID modes.
     */
    public boolean pressed(int keyCode) {
        if (pressedKeys.contains(keyCode)) return true;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_L2:
                return lt >= DIGITAL_TRIGGER_THRESHOLD;
            case KeyEvent.KEYCODE_BUTTON_R2:
                return rt >= DIGITAL_TRIGGER_THRESHOLD;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return hatX <= -DIGITAL_HAT_THRESHOLD;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return hatX >= DIGITAL_HAT_THRESHOLD;
            case KeyEvent.KEYCODE_DPAD_UP:
                return hatY <= -DIGITAL_HAT_THRESHOLD;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return hatY >= DIGITAL_HAT_THRESHOLD;
            default:
                return false;
        }
    }
}
