package com.zploy.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public final class MappingStore {
    private static final String PREF = "zploy_mappings";
    private final SharedPreferences prefs;
    private final ProfileStore profiles;
    private static final Map<String, List<MappingItem>> cache = new ConcurrentHashMap<>();

    public MappingStore(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        profiles = new ProfileStore(context);
    }

    private String activeProfile() { return profiles.activeId(); }
    private String key(String profileId) { return "items_" + profileId; }

    public synchronized List<MappingItem> load() {
        String profileId = activeProfile();
        List<MappingItem> cached = cache.get(profileId);
        if (cached != null) return new ArrayList<>(cached);
        String raw = prefs.getString(key(profileId), null);
        if (raw == null) {
            List<MappingItem> empty = new ArrayList<>();
            cache.put(profileId, empty);
            return new ArrayList<>(empty);
        }
        try {
            JSONArray arr = new JSONArray(raw);
            List<MappingItem> out = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) out.add(MappingItem.fromJson(arr.getJSONObject(i)));
            cache.put(profileId, out);
            return new ArrayList<>(out);
        } catch (Exception e) {
            List<MappingItem> empty = new ArrayList<>();
            cache.put(profileId, empty);
            return new ArrayList<>(empty);
        }
    }

    public synchronized void save(List<MappingItem> items) {
        String profileId = activeProfile();
        try {
            JSONArray arr = new JSONArray();
            for (MappingItem item : items) arr.put(item.toJson());
            cache.put(profileId, new ArrayList<>(items));
            prefs.edit().putString(key(profileId), arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public synchronized void clearProfile(String profileId) {
        cache.remove(profileId);
        prefs.edit().remove(key(profileId)).apply();
    }

    public synchronized MappingItem findByKey(int keyCode) {
        for (MappingItem item : load()) if (item.keyCode == keyCode) return item;
        return null;
    }

    public static String labelForKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: return "A";
            case KeyEvent.KEYCODE_BUTTON_B: return "B";
            case KeyEvent.KEYCODE_BUTTON_X: return "X";
            case KeyEvent.KEYCODE_BUTTON_Y: return "Y";
            case KeyEvent.KEYCODE_BUTTON_L1: return "LB";
            case KeyEvent.KEYCODE_BUTTON_R1: return "RB";
            case KeyEvent.KEYCODE_BUTTON_L2: return "LT";
            case KeyEvent.KEYCODE_BUTTON_R2: return "RT";
            case KeyEvent.KEYCODE_BUTTON_SELECT: return "View";
            case KeyEvent.KEYCODE_BUTTON_START: return "Menu";
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return "L3";
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return "R3";
            case KeyEvent.KEYCODE_DPAD_UP: return "↑";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "↓";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "←";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "→";
            default: return KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "");
        }
    }

    public static MappingItem newButton(int keyCode, MappingType type) {
        return new MappingItem(UUID.randomUUID().toString(), keyCode, labelForKey(keyCode),
                type, 0.5f, 0.5f, 0.055f, 0.1f, 1f);
    }

    public static MappingItem newAnalog(int keyCode, MappingType type) {
        String label = keyCode == MappingItem.KEY_LEFT_STICK ? "LS" : "RS";
        float radius = type == MappingType.JOYSTICK ? 0.085f : 0.07f;
        float deadZone = keyCode == MappingItem.KEY_LEFT_STICK ? 0.12f : 0.10f;
        return new MappingItem(UUID.randomUUID().toString(), keyCode, label,
                type, 0.5f, 0.5f, radius, deadZone, 1f);
    }
}
