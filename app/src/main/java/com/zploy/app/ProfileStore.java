package com.zploy.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Small local profile registry. Profiles keep separate mapping sets. */
public final class ProfileStore {
    public static final class Profile {
        public final String id;
        public String name;
        Profile(String id, String name) { this.id = id; this.name = name; }
    }

    private static final String PREF = "zploy_profiles";
    private static final String KEY_LIST = "profiles";
    private static final String KEY_ACTIVE = "active";
    private static final String DEFAULT_ID = "default";
    private final SharedPreferences prefs;
    private final Context context;

    public ProfileStore(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        ensureInitialized();
    }

    private synchronized void ensureInitialized() {
        if (prefs.contains(KEY_LIST)) return;
        JSONArray arr = new JSONArray();
        try {
            JSONObject o = new JSONObject();
            o.put("id", DEFAULT_ID);
            o.put("name", context.getString(R.string.default_profile));
            arr.put(o);
        } catch (Exception ignored) {}
        prefs.edit().putString(KEY_LIST, arr.toString()).putString(KEY_ACTIVE, DEFAULT_ID).apply();
    }

    public synchronized List<Profile> load() {
        ensureInitialized();
        List<Profile> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_LIST, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Profile(o.getString("id"), o.optString("name", "Profile")));
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) {
            out.add(new Profile(DEFAULT_ID, context.getString(R.string.default_profile)));
            save(out);
        }
        return out;
    }

    private synchronized void save(List<Profile> profiles) {
        JSONArray arr = new JSONArray();
        try {
            for (Profile p : profiles) {
                JSONObject o = new JSONObject();
                o.put("id", p.id);
                o.put("name", p.name);
                arr.put(o);
            }
            prefs.edit().putString(KEY_LIST, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public synchronized String activeId() {
        List<Profile> profiles = load();
        String active = prefs.getString(KEY_ACTIVE, profiles.get(0).id);
        for (Profile p : profiles) if (p.id.equals(active)) return active;
        active = profiles.get(0).id;
        prefs.edit().putString(KEY_ACTIVE, active).apply();
        return active;
    }

    public synchronized String activeName() {
        String id = activeId();
        for (Profile p : load()) if (p.id.equals(id)) return p.name;
        return context.getString(R.string.default_profile);
    }

    public synchronized void setActive(String id) {
        for (Profile p : load()) {
            if (p.id.equals(id)) {
                prefs.edit().putString(KEY_ACTIVE, id).apply();
                return;
            }
        }
    }

    public synchronized Profile create(String name) {
        List<Profile> profiles = load();
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) clean = "Profile " + (profiles.size() + 1);
        Profile created = new Profile(UUID.randomUUID().toString(), clean);
        profiles.add(created);
        save(profiles);
        setActive(created.id);
        return created;
    }

    public synchronized void rename(String id, String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) return;
        List<Profile> profiles = load();
        for (Profile p : profiles) if (p.id.equals(id)) p.name = clean;
        save(profiles);
    }

    public synchronized boolean delete(String id) {
        List<Profile> profiles = load();
        if (profiles.size() <= 1) return false;
        boolean removed = profiles.removeIf(p -> p.id.equals(id));
        if (!removed) return false;
        save(profiles);
        if (id.equals(prefs.getString(KEY_ACTIVE, ""))) {
            prefs.edit().putString(KEY_ACTIVE, profiles.get(0).id).apply();
        }
        return true;
    }
}
