package com.zploy.app;

import android.content.Context;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

/**
 * Runs inside Shizuku UserService as shell/root identity.
 * The UserService process is exempt from non-SDK API restrictions, so the
 * Android framework input manager can be used without an extra ADB connection.
 */
public final class InputInjectorService extends IInputInjector.Stub {
    private Object inputManagerGlobal;
    private Method injectInputEvent;
    private Method setDisplayId;

    public InputInjectorService() {
        initReflection();
    }

    public InputInjectorService(Context ignored) {
        initReflection();
    }

    private void initReflection() {
        try {
            Class<?> global = Class.forName("android.hardware.input.InputManagerGlobal");
            Method getInstance = global.getDeclaredMethod("getInstance");
            getInstance.setAccessible(true);
            inputManagerGlobal = getInstance.invoke(null);
            injectInputEvent = global.getDeclaredMethod("injectInputEvent", InputEvent.class, int.class);
            injectInputEvent.setAccessible(true);
        } catch (Throwable first) {
            // Older framework fallback. Android 16 normally uses InputManagerGlobal.
            try {
                Class<?> im = Class.forName("android.hardware.input.InputManager");
                Method getInstance = im.getDeclaredMethod("getInstance");
                getInstance.setAccessible(true);
                inputManagerGlobal = getInstance.invoke(null);
                injectInputEvent = im.getDeclaredMethod("injectInputEvent", InputEvent.class, int.class);
                injectInputEvent.setAccessible(true);
            } catch (Throwable ignored) {
                inputManagerGlobal = null;
                injectInputEvent = null;
            }
        }

        try {
            setDisplayId = InputEvent.class.getDeclaredMethod("setDisplayId", int.class);
            setDisplayId.setAccessible(true);
        } catch (Throwable ignored) {
            setDisplayId = null;
        }
    }

    @Override
    public boolean ping() {
        return inputManagerGlobal != null && injectInputEvent != null;
    }

    @Override
    public boolean injectMotion(int action, long downTime, long eventTime,
                                int[] pointerIds, float[] xs, float[] ys) {
        if (!ping() || pointerIds == null || xs == null || ys == null) return false;
        if (pointerIds.length == 0 || pointerIds.length != xs.length || xs.length != ys.length) return false;

        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerIds.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerIds.length];

        for (int i = 0; i < pointerIds.length; i++) {
            MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
            pp.id = pointerIds[i];
            pp.toolType = MotionEvent.TOOL_TYPE_FINGER;
            properties[i] = pp;

            MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
            pc.x = xs[i];
            pc.y = ys[i];
            pc.pressure = 1f;
            pc.size = 1f;
            coords[i] = pc;
        }

        if (eventTime <= 0) eventTime = SystemClock.uptimeMillis();
        if (downTime <= 0) downTime = eventTime;

        MotionEvent event = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                pointerIds.length,
                properties,
                coords,
                0,
                0,
                1f,
                1f,
                0,
                0,
                InputDevice.SOURCE_TOUCHSCREEN,
                0
        );
        try {
            if (setDisplayId != null) setDisplayId.invoke(event, 0);
            Object result = injectInputEvent.invoke(inputManagerGlobal, event, 0); // ASYNC
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable e) {
            return false;
        } finally {
            event.recycle();
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
