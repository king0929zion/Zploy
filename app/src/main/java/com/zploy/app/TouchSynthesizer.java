package com.zploy.app;

import android.os.SystemClock;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts a desired set of named virtual fingers into a valid Android multi-touch stream. */
public final class TouchSynthesizer {
    private static final float MOVE_EPSILON = 0.5f;

    private static final class Active {
        final int pointerId;
        TouchPoint point;
        Active(int pointerId, TouchPoint point) { this.pointerId = pointerId; this.point = point; }
    }

    private final LinkedHashMap<String, Active> active = new LinkedHashMap<>();
    private long downTime;

    public synchronized void sync(Map<String, TouchPoint> desired) {
        // Remove pointers no longer desired. Remove one at a time so actionIndex is valid.
        List<String> currentKeys = new ArrayList<>(active.keySet());
        for (int i = currentKeys.size() - 1; i >= 0; i--) {
            String key = currentKeys.get(i);
            if (!desired.containsKey(key)) remove(key);
        }

        // Add new pointers.
        for (Map.Entry<String, TouchPoint> entry : desired.entrySet()) {
            if (!active.containsKey(entry.getKey())) add(entry.getKey(), entry.getValue());
        }

        boolean moved = false;
        for (Map.Entry<String, TouchPoint> entry : desired.entrySet()) {
            Active a = active.get(entry.getKey());
            if (a == null) continue;
            TouchPoint p = entry.getValue();
            if (Math.abs(a.point.x - p.x) > MOVE_EPSILON || Math.abs(a.point.y - p.y) > MOVE_EPSILON) {
                a.point = p;
                moved = true;
            }
        }

        if (moved && !active.isEmpty()) inject(MotionEvent.ACTION_MOVE, -1);
    }

    public synchronized void cancelAll() {
        List<String> keys = new ArrayList<>(active.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) remove(keys.get(i));
        active.clear();
        downTime = 0L;
    }

    private void add(String key, TouchPoint point) {
        int pointerId = allocatePointerId();
        if (active.isEmpty()) {
            downTime = SystemClock.uptimeMillis();
            active.put(key, new Active(pointerId, point));
            inject(MotionEvent.ACTION_DOWN, 0);
        } else {
            active.put(key, new Active(pointerId, point));
            inject(MotionEvent.ACTION_POINTER_DOWN, active.size() - 1);
        }
    }


    private int allocatePointerId() {
        // Android pointer ids are small integers. Reuse the lowest id that is
        // not active so profile edits cannot create collisions over time.
        for (int candidate = 0; candidate < 32; candidate++) {
            boolean used = false;
            for (Active a : active.values()) {
                if (a.pointerId == candidate) { used = true; break; }
            }
            if (!used) return candidate;
        }
        throw new IllegalStateException("Too many simultaneous virtual touches");
    }

    private void remove(String key) {
        int index = indexOf(key);
        if (index < 0) return;
        if (active.size() == 1) {
            inject(MotionEvent.ACTION_UP, 0);
            active.remove(key);
            downTime = 0L;
        } else {
            inject(MotionEvent.ACTION_POINTER_UP, index);
            active.remove(key);
        }
    }

    private int indexOf(String key) {
        int i = 0;
        for (String k : active.keySet()) {
            if (k.equals(key)) return i;
            i++;
        }
        return -1;
    }

    private boolean inject(int baseAction, int actionIndex) {
        if (active.isEmpty()) return false;
        int n = active.size();
        int[] pointerIds = new int[n];
        float[] xs = new float[n];
        float[] ys = new float[n];
        int i = 0;
        for (Active a : active.values()) {
            pointerIds[i] = a.pointerId;
            xs[i] = a.point.x;
            ys[i] = a.point.y;
            i++;
        }
        int action = baseAction;
        if ((baseAction == MotionEvent.ACTION_POINTER_DOWN || baseAction == MotionEvent.ACTION_POINTER_UP) && actionIndex >= 0) {
            action |= (actionIndex << MotionEvent.ACTION_POINTER_INDEX_SHIFT);
        }
        long now = SystemClock.uptimeMillis();
        return ShizukuBridge.get().injectMotion(action, downTime == 0 ? now : downTime, now, pointerIds, xs, ys);
    }
}
