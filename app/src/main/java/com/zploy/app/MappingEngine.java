package com.zploy.app;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 60 Hz game-scoped mapping loop. Shizuku is preferred for raw multi-touch injection. */
public final class MappingEngine {
    private final Context context;
    private final MappingStore store;
    private final TouchSynthesizer touch = new TouchSynthesizer();
    private final HandlerThread thread = new HandlerThread("ZployMapping");
    private final Handler handler;
    private volatile boolean running;
    private Set<Integer> previousKeys = new HashSet<>();
    private final Map<String, Long> tapUntil = new HashMap<>();
    private final Map<String, TouchPoint> cameraPositions = new HashMap<>();
    private final Set<String> cameraReset = new HashSet<>();

    public MappingEngine(Context context) {
        this.context = context.getApplicationContext();
        this.store = new MappingStore(context);
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    public void start() { if (!running) { running = true; handler.post(loop); } }
    public void stop() { running = false; handler.removeCallbacksAndMessages(null); reset(); }
    public void destroy() { stop(); thread.quitSafely(); }

    private void reset() {
        touch.cancelAll();
        previousKeys = new HashSet<>();
        tapUntil.clear();
        cameraPositions.clear();
        cameraReset.clear();
    }

    private final Runnable loop = new Runnable() {
        @Override public void run() {
            if (!running) return;
            tick();
            handler.postDelayed(this, 16L);
        }
    };

    private void tick() {
        ZployAccessibilityService accessibility = ZployAccessibilityService.getInstance();
        if (accessibility == null || !accessibility.isTargetGameForeground() || accessibility.isOverlayEditing()) {
            reset();
            return;
        }

        String backend = Prefs.backend(context);
        boolean shizukuAllowed = !Prefs.BACKEND_ACCESSIBILITY.equals(backend);
        if (!shizukuAllowed || !ShizukuBridge.get().isReady()) {
            touch.cancelAll();
            return;
        }

        ControllerState state = ControllerStore.get().snapshot();
        List<MappingItem> mappings = store.load();
        long now = SystemClock.uptimeMillis();
        WindowManager wm = context.getSystemService(WindowManager.class);
        if (wm == null) return;
        Rect bounds = wm.getCurrentWindowMetrics().getBounds();
        int width = bounds.width(), height = bounds.height();
        float shortSide = Math.min(width, height);

        Map<String, TouchPoint> desired = new LinkedHashMap<>();
        Set<Integer> currentKeys = effectivePressedKeys(state);

        for (MappingItem item : mappings) {
            switch (item.type) {
                case JOYSTICK:
                    if (item.keyCode == MappingItem.KEY_LEFT_STICK || item.keyCode == MappingItem.KEY_RIGHT_STICK) {
                        float sx = item.keyCode == MappingItem.KEY_LEFT_STICK ? state.lx : state.rx;
                        float sy = item.keyCode == MappingItem.KEY_LEFT_STICK ? state.ly : state.ry;
                        float x = MappingMath.applyDeadZone(sx, item.deadZone);
                        float y = MappingMath.applyDeadZone(sy, item.deadZone);
                        float mag = MappingMath.magnitude(x, y);
                        if (mag > 0.001f) {
                            if (mag > 1f) { x /= mag; y /= mag; }
                            float cx = item.x * width, cy = item.y * height;
                            float r = item.radius * shortSide * item.sensitivity;
                            desired.put(item.id, new TouchPoint(cx + x * r, cy + y * r));
                        }
                    }
                    break;
                case CAMERA:
                    if (item.keyCode == MappingItem.KEY_RIGHT_STICK || item.keyCode == MappingItem.KEY_LEFT_STICK)
                        handleCamera(item, state, width, height, desired);
                    break;
                case TAP:
                    if (state.pressed(item.keyCode) && !previousKeys.contains(item.keyCode)) tapUntil.put(item.id, now + 55L);
                    Long expiry = tapUntil.get(item.id);
                    if (expiry != null && now < expiry) desired.put(item.id, new TouchPoint(item.x * width, item.y * height));
                    else if (expiry != null) tapUntil.remove(item.id);
                    break;
                case HOLD:
                    if (state.pressed(item.keyCode)) desired.put(item.id, new TouchPoint(item.x * width, item.y * height));
                    break;
            }
        }

        touch.sync(desired);
        previousKeys = currentKeys;
    }

    private Set<Integer> effectivePressedKeys(ControllerState state) {
        Set<Integer> keys = new HashSet<>(state.pressedKeys);
        int[] synthesized = {
                android.view.KeyEvent.KEYCODE_BUTTON_L2, android.view.KeyEvent.KEYCODE_BUTTON_R2,
                android.view.KeyEvent.KEYCODE_DPAD_UP, android.view.KeyEvent.KEYCODE_DPAD_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_LEFT, android.view.KeyEvent.KEYCODE_DPAD_RIGHT
        };
        for (int key : synthesized) if (state.pressed(key)) keys.add(key);
        return keys;
    }

    private void handleCamera(MappingItem item, ControllerState state, int width, int height, Map<String, TouchPoint> desired) {
        boolean left = item.keyCode == MappingItem.KEY_LEFT_STICK;
        float rx = MappingMath.cameraCurve(left ? state.lx : state.rx, item.deadZone, 1.45f);
        float ry = MappingMath.cameraCurve(left ? state.ly : state.ry, item.deadZone, 1.45f);
        if (Math.abs(rx) < 0.001f && Math.abs(ry) < 0.001f) {
            cameraPositions.remove(item.id); cameraReset.remove(item.id); return;
        }
        if (cameraReset.remove(item.id)) { cameraPositions.remove(item.id); return; }
        TouchPoint p = cameraPositions.get(item.id);
        if (p == null) p = new TouchPoint(item.x * width, item.y * height);
        float baseSpeed = Math.min(width, height) * 0.012f * item.sensitivity;
        float nx = p.x + rx * baseSpeed, ny = p.y + ry * baseSpeed;
        float minX = width * 0.12f, maxX = width * 0.88f, minY = height * 0.12f, maxY = height * 0.88f;
        if (nx < minX || nx > maxX || ny < minY || ny > maxY) { cameraReset.add(item.id); return; }
        TouchPoint next = new TouchPoint(nx, ny);
        cameraPositions.put(item.id, next);
        desired.put(item.id, next);
    }
}
