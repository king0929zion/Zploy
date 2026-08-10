package com.zploy.app;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** In-game floating button plus draggable semi-transparent mapping labels. */
public final class OverlayController {
    private final ZployAccessibilityService service;
    private final WindowManager wm;
    private final MappingStore store;
    private TextView bubble;
    private FrameLayout overlay;
    private boolean bubbleShown;
    private boolean overlayShown;
    private boolean editing;
    private MappingItem selected;
    private float bubbleDownX, bubbleDownY;
    private int bubbleStartX, bubbleStartY;

    public OverlayController(ZployAccessibilityService service) {
        this.service = service;
        this.wm = service.getSystemService(WindowManager.class);
        this.store = new MappingStore(service);
    }

    public boolean isEditing() { return editing; }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            showBubble();
            showMappingOverlay(false);
        } else {
            editing = false;
            removeOverlay();
            removeBubble();
        }
    }

    public void openEditor() {
        if (!bubbleShown) showBubble();
        showMappingOverlay(true);
    }

    public void toggleEditor() {
        showMappingOverlay(!editing);
    }

    public void onControllerKeyPressed(int keyCode) {
        if (!editing || keyCode == KeyEvent.KEYCODE_UNKNOWN) return;
        List<MappingItem> items = store.load();
        MappingItem found = null;
        for (MappingItem item : items) {
            if (item.keyCode == keyCode) { found = item; break; }
        }
        if (found == null) {
            found = MappingStore.newButton(keyCode);
            items.add(found);
            store.save(items);
        }
        selected = found;
        renderOverlay();
    }

    public void refresh() {
        if (overlayShown) renderOverlay();
    }

    public void destroy() {
        removeOverlay();
        removeBubble();
    }

    private WindowManager.LayoutParams bubbleParams() {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                Ui.dp(service, 52), Ui.dp(service, 52),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = Ui.dp(service, 16);
        p.y = Ui.dp(service, 160);
        return p;
    }

    private void showBubble() {
        if (bubbleShown || wm == null) return;
        bubble = Ui.text(service, "Z", 16, Color.WHITE, true);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.round(Ui.BLACK, 19, service));
        bubble.setAlpha(0.92f);
        bubble.setOnTouchListener((v, event) -> {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) bubble.getLayoutParams();
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    bubbleDownX = event.getRawX();
                    bubbleDownY = event.getRawY();
                    bubbleStartX = lp.x;
                    bubbleStartY = lp.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = bubbleStartX + Math.round(event.getRawX() - bubbleDownX);
                    lp.y = bubbleStartY + Math.round(event.getRawY() - bubbleDownY);
                    wm.updateViewLayout(bubble, lp);
                    return true;
                case MotionEvent.ACTION_UP:
                    float dist = Math.abs(event.getRawX() - bubbleDownX) + Math.abs(event.getRawY() - bubbleDownY);
                    if (dist < Ui.dp(service, 10)) toggleEditor();
                    return true;
            }
            return false;
        });
        wm.addView(bubble, bubbleParams());
        bubbleShown = true;
    }

    private WindowManager.LayoutParams overlayParams(boolean edit) {
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (!edit) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void showMappingOverlay(boolean edit) {
        editing = edit;
        if (overlayShown) {
            wm.updateViewLayout(overlay, overlayParams(edit));
            renderOverlay();
            return;
        }
        overlay = new FrameLayout(service);
        overlay.setBackgroundColor(Color.TRANSPARENT);
        wm.addView(overlay, overlayParams(edit));
        overlayShown = true;
        renderOverlay();
    }

    private void renderOverlay() {
        if (!overlayShown || overlay == null) return;
        overlay.removeAllViews();
        List<MappingItem> items = new ArrayList<>(store.load());
        Rect bounds = wm.getCurrentWindowMetrics().getBounds();
        int width = bounds.width();
        int height = bounds.height();
        float playAlpha = Prefs.overlayAlpha(service);

        for (MappingItem item : items) {
            TextView marker = Ui.text(service, item.label, item.type == MappingType.CAMERA || item.type == MappingType.JOYSTICK ? 13 : 14,
                    Color.WHITE, true);
            marker.setGravity(Gravity.CENTER);
            int size = Ui.dp(service, item.type == MappingType.CAMERA || item.type == MappingType.JOYSTICK ? 70 : 50);
            int bg = selected != null && selected.id.equals(item.id) && editing
                    ? Color.argb(230, 17, 17, 17) : Color.argb(205, 45, 45, 45);
            marker.setBackground(Ui.round(bg, size / service.getResources().getDisplayMetrics().density / 2f, service));
            marker.setAlpha(editing ? 0.9f : playAlpha);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
            lp.leftMargin = Math.round(item.x * width - size / 2f);
            lp.topMargin = Math.round(item.y * height - size / 2f);
            marker.setLayoutParams(lp);

            if (editing) {
                marker.setOnClickListener(v -> {
                    selected = item;
                    renderOverlay();
                });
                marker.setOnTouchListener(new View.OnTouchListener() {
                    float downX, downY;
                    int startL, startT;
                    boolean moved;
                    @Override public boolean onTouch(View v, MotionEvent e) {
                        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) v.getLayoutParams();
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                downX = e.getRawX(); downY = e.getRawY(); startL = p.leftMargin; startT = p.topMargin; moved = false;
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                int dx = Math.round(e.getRawX() - downX), dy = Math.round(e.getRawY() - downY);
                                if (Math.abs(dx) + Math.abs(dy) > Ui.dp(service, 4)) moved = true;
                                p.leftMargin = startL + dx; p.topMargin = startT + dy;
                                v.setLayoutParams(p);
                                return true;
                            case MotionEvent.ACTION_UP:
                                if (moved) {
                                    item.x = MappingMath.clamp((p.leftMargin + size / 2f) / width, 0f, 1f);
                                    item.y = MappingMath.clamp((p.topMargin + size / 2f) / height, 0f, 1f);
                                    replaceAndSave(item);
                                    selected = item;
                                } else {
                                    selected = item;
                                }
                                renderOverlay();
                                return true;
                        }
                        return false;
                    }
                });
            }
            overlay.addView(marker);
        }

        if (editing) addEditorPanel(items);
    }

    private void addEditorPanel(List<MappingItem> items) {
        LinearLayout panel = Ui.vertical(service);
        panel.setPadding(Ui.dp(service, 12), Ui.dp(service, 10), Ui.dp(service, 12), Ui.dp(service, 10));
        panel.setBackground(Ui.round(Color.argb(238, 17, 17, 17), 22, service));

        String title = selected == null ? service.getString(R.string.press_button_to_add)
                : selected.label + " · " + typeText(selected.type);
        TextView titleView = Ui.text(service, title, 12, Color.WHITE, true);
        panel.addView(titleView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(service, 34)));

        LinearLayout actions = new LinearLayout(service);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        if (selected == null) {
            addPanelButton(actions, service.getString(R.string.close), () -> showMappingOverlay(false));
        } else if (selected.keyCode == MappingItem.KEY_LEFT_STICK || selected.keyCode == MappingItem.KEY_RIGHT_STICK) {
            addPanelButton(actions, service.getString(R.string.joystick), () -> changeSelectedType(MappingType.JOYSTICK));
            addPanelButton(actions, service.getString(R.string.camera), () -> changeSelectedType(MappingType.CAMERA));
            addPanelButton(actions, service.getString(R.string.reset), () -> resetSelectedAnalog());
            addPanelButton(actions, service.getString(R.string.close), () -> showMappingOverlay(false));
        } else {
            addPanelButton(actions, service.getString(R.string.tap), () -> changeSelectedType(MappingType.TAP));
            addPanelButton(actions, service.getString(R.string.hold), () -> changeSelectedType(MappingType.HOLD));
            addPanelButton(actions, service.getString(R.string.delete), this::deleteSelected);
            addPanelButton(actions, service.getString(R.string.close), () -> showMappingOverlay(false));
        }
        panel.addView(actions);

        if (selected != null && (selected.type == MappingType.JOYSTICK || selected.type == MappingType.CAMERA)) {
            addSlider(panel, service.getString(R.string.sensitivity), 30, 200,
                    Math.round(selected.sensitivity * 100f), value -> {
                        selected.sensitivity = value / 100f;
                        replaceAndSave(selected);
                    });
            addSlider(panel, service.getString(R.string.dead_zone), 0, 30,
                    Math.round(selected.deadZone * 100f), value -> {
                        selected.deadZone = value / 100f;
                        replaceAndSave(selected);
                    });
            if (selected.type == MappingType.JOYSTICK) {
                addSlider(panel, service.getString(R.string.radius), 3, 18,
                        Math.round(selected.radius * 100f), value -> {
                            selected.radius = value / 100f;
                            replaceAndSave(selected);
                        });
            }
        }

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        p.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        p.topMargin = Ui.dp(service, 24);
        overlay.addView(panel, p);
    }

    private interface IntValueListener { void onValue(int value); }

    private void addSlider(LinearLayout parent, String label, int min, int max, int current,
                           IntValueListener listener) {
        LinearLayout block = Ui.vertical(service);
        block.setPadding(0, Ui.dp(service, 7), 0, 0);
        TextView name = Ui.text(service, label, 10, Color.rgb(190, 190, 190), false);
        block.addView(name);
        SeekBar bar = new SeekBar(service);
        bar.setMax(max - min);
        bar.setProgress(Math.max(0, Math.min(max - min, current - min)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) listener.onValue(progress + min);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        block.addView(bar, new LinearLayout.LayoutParams(Ui.dp(service, 300), Ui.dp(service, 38)));
        parent.addView(block);
    }

    private void addPanelButton(LinearLayout row, String label, Runnable action) {
        TextView b = Ui.text(service, label, 11, Color.WHITE, false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.round(Color.rgb(45, 45, 45), 14, service));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(service, 72), Ui.dp(service, 38));
        bp.rightMargin = Ui.dp(service, 6);
        row.addView(b, bp);
        b.setOnClickListener(v -> action.run());
    }

    private void changeSelectedType(MappingType type) {
        if (selected == null) return;
        boolean analogSource = selected.keyCode == MappingItem.KEY_LEFT_STICK
                || selected.keyCode == MappingItem.KEY_RIGHT_STICK;
        if (analogSource && type != MappingType.JOYSTICK && type != MappingType.CAMERA) return;
        if (!analogSource && type != MappingType.TAP && type != MappingType.HOLD) return;
        selected.type = type;
        replaceAndSave(selected);
        renderOverlay();
    }

    private void resetSelectedAnalog() {
        if (selected == null) return;
        selected.sensitivity = 1f;
        selected.deadZone = selected.keyCode == MappingItem.KEY_LEFT_STICK ? 0.12f : 0.10f;
        selected.radius = selected.keyCode == MappingItem.KEY_LEFT_STICK ? 0.085f : 0.07f;
        replaceAndSave(selected);
        renderOverlay();
    }

    private void deleteSelected() {
        if (selected == null) return;
        if (selected.keyCode == MappingItem.KEY_LEFT_STICK || selected.keyCode == MappingItem.KEY_RIGHT_STICK) return;
        List<MappingItem> current = store.load();
        current.removeIf(m -> m.id.equals(selected.id));
        store.save(current);
        selected = null;
        renderOverlay();
    }

    private String typeText(MappingType type) {
        switch (type) {
            case HOLD: return service.getString(R.string.hold);
            case JOYSTICK: return service.getString(R.string.joystick);
            case CAMERA: return service.getString(R.string.camera);
            default: return service.getString(R.string.tap);
        }
    }

    private void replaceAndSave(MappingItem changed) {
        List<MappingItem> current = store.load();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).id.equals(changed.id)) { current.set(i, changed); break; }
        }
        store.save(current);
    }

    private void removeOverlay() {
        if (overlayShown && overlay != null && wm != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlayShown = false;
        overlay = null;
    }

    private void removeBubble() {
        if (bubbleShown && bubble != null && wm != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
        }
        bubbleShown = false;
        bubble = null;
    }
}
