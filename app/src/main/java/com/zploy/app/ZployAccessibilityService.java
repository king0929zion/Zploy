package com.zploy.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;

public final class ZployAccessibilityService extends AccessibilityService {
    private static volatile ZployAccessibilityService instance;
    public static ZployAccessibilityService getInstance() { return instance; }

    private MappingEngine engine;
    private OverlayController overlay;
    private MappingStore store;
    private volatile String foregroundPackage = "";

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        ShizukuBridge.get().init(this);
        store = new MappingStore(this);
        engine = new MappingEngine(this);
        overlay = new OverlayController(this);
        applyMappingState(Prefs.mappingEnabled(this));
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.isEmpty() || pkg.equals(foregroundPackage)) return;
        foregroundPackage = pkg;
        if (overlay != null) overlay.onForegroundPackageChanged(pkg);
        setJoystickCapture(Prefs.mappingEnabled(this) && isTargetGameForeground());
    }

    @Override public void onInterrupt() {}

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (!ControllerStore.isControllerSource(event.getSource())) return false;
        ControllerStore.get().updateKey(event);

        if (overlay != null && overlay.isEditing() && event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            overlay.onControllerKeyPressed(event.getKeyCode());
            return true;
        }

        if (!Prefs.mappingEnabled(this) || !isTargetGameForeground()) return false;
        MappingItem item = store == null ? null : store.findByKey(event.getKeyCode());
        if (item == null) return false;

        String backend = Prefs.backend(this);
        if (Prefs.BACKEND_ACCESSIBILITY.equals(backend) || !ShizukuBridge.get().isReady()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) accessibilityCompat(item);
        }
        return true;
    }

    @Override public void onMotionEvent(MotionEvent event) {
        if (!ControllerStore.isControllerSource(event.getSource())) return;
        ControllerState before = ControllerStore.get().snapshot();
        ControllerStore.get().updateMotion(event);
        ControllerState after = ControllerStore.get().snapshot();

        if (overlay != null && overlay.isEditing()) {
            int[] axisButtons = {
                    KeyEvent.KEYCODE_BUTTON_L2, KeyEvent.KEYCODE_BUTTON_R2,
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT
            };
            for (int key : axisButtons) {
                if (!before.pressed(key) && after.pressed(key)) {
                    overlay.onControllerKeyPressed(key);
                    break;
                }
            }
        }
    }

    public void applyMappingState(boolean enabled) {
        Prefs.setMappingEnabled(this, enabled);
        if (enabled) {
            if (engine != null) engine.start();
            if (overlay != null) overlay.setEnabled(true);
        } else {
            if (engine != null) engine.stop();
            if (overlay != null) overlay.setEnabled(false);
        }
        setJoystickCapture(enabled && isTargetGameForeground());
    }

    public void openEditor() { if (overlay != null) overlay.openEditor(); }
    public void refreshOverlay() { if (overlay != null) overlay.refresh(); }
    public boolean isOverlayEditing() { return overlay != null && overlay.isEditing(); }
    public boolean isTargetGameForeground() { return overlay != null && overlay.isTargetGameForeground(); }
    public String foregroundPackage() { return foregroundPackage; }

    private void setJoystickCapture(boolean capture) {
        try {
            AccessibilityServiceInfo info = getServiceInfo();
            if (info == null) return;
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            info.setMotionEventSources(capture ? InputDevice.SOURCE_JOYSTICK : 0);
            setServiceInfo(info);
        } catch (Throwable ignored) {}
    }

    private void accessibilityCompat(MappingItem item) {
        if (item.type != MappingType.TAP && item.type != MappingType.HOLD) return;
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        Path p = new Path();
        p.moveTo(item.x * w, item.y * h);
        long duration = item.type == MappingType.HOLD ? 650L : 45L;
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(p, 0, duration);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    @Override public void onDestroy() {
        if (engine != null) engine.destroy();
        if (overlay != null) overlay.destroy();
        instance = null;
        super.onDestroy();
    }
}
