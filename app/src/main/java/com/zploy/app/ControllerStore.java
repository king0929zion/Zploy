package com.zploy.app;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ControllerStore {
    public interface Listener { void onControllerState(ControllerState state); }

    private static final ControllerStore INSTANCE = new ControllerStore();
    public static ControllerStore get() { return INSTANCE; }

    private final Set<Integer> pressed = new HashSet<>();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private String deviceName = "";
    private int deviceId = -1;
    private float lx, ly, rx, ry, lt, rt, hatX, hatY;
    private int lastKeyCode = KeyEvent.KEYCODE_UNKNOWN;

    private ControllerStore() {}

    public void addListener(Listener listener) { listeners.addIfAbsent(listener); }
    public void removeListener(Listener listener) { listeners.remove(listener); }

    public synchronized ControllerState snapshot() {
        return new ControllerState(deviceName, deviceId, lx, ly, rx, ry, lt, rt, hatX, hatY, pressed, lastKeyCode);
    }

    public void updateKey(KeyEvent event) {
        InputDevice d = event.getDevice();
        synchronized (this) {
            if (d != null) {
                deviceName = d.getName();
                deviceId = d.getId();
            }
            lastKeyCode = event.getKeyCode();
            if (event.getAction() == KeyEvent.ACTION_DOWN) pressed.add(event.getKeyCode());
            if (event.getAction() == KeyEvent.ACTION_UP) pressed.remove(event.getKeyCode());
        }
        notifyListeners();
    }

    public void updateMotion(MotionEvent event) {
        InputDevice d = event.getDevice();
        synchronized (this) {
            if (d != null) {
                deviceName = d.getName();
                deviceId = d.getId();
            }
            lx = axis(event, MotionEvent.AXIS_X);
            ly = axis(event, MotionEvent.AXIS_Y);

            float z = axis(event, MotionEvent.AXIS_Z);
            float rz = axis(event, MotionEvent.AXIS_RZ);
            float rxCandidate = axis(event, MotionEvent.AXIS_RX);
            float ryCandidate = axis(event, MotionEvent.AXIS_RY);
            if (hasAxis(event, MotionEvent.AXIS_Z) || hasAxis(event, MotionEvent.AXIS_RZ)) {
                rx = z;
                ry = rz;
            } else {
                rx = rxCandidate;
                ry = ryCandidate;
            }

            lt = firstAvailableAxis(event, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE);
            rt = firstAvailableAxis(event, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS);
            hatX = axis(event, MotionEvent.AXIS_HAT_X);
            hatY = axis(event, MotionEvent.AXIS_HAT_Y);
        }
        notifyListeners();
    }

    public synchronized void setDevice(InputDevice d) {
        if (d == null) return;
        deviceName = d.getName();
        deviceId = d.getId();
        notifyListeners();
    }

    private static boolean hasAxis(MotionEvent e, int axis) {
        InputDevice d = e.getDevice();
        return d != null && d.getMotionRange(axis, e.getSource()) != null;
    }

    private static float axis(MotionEvent e, int axis) {
        try { return e.getAxisValue(axis); } catch (Exception ignored) { return 0f; }
    }

    private static float firstAvailableAxis(MotionEvent e, int a, int b) {
        if (hasAxis(e, a)) return axis(e, a);
        return axis(e, b);
    }

    private void notifyListeners() {
        ControllerState s = snapshot();
        for (Listener l : listeners) l.onControllerState(s);
    }

    public static boolean isControllerSource(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
    }
}
