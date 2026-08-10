package com.zploy.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local game/profile registry. Every profile can be bound to one package. */
public final class ProfileStore {
    public static final class Profile {
        public final String id;
        public String name;
        public String packageName;
        public String appLabel;

        Profile(String id, String name, String packageName, String appLabel) {
            this.id = id;
            this.name = name;
            this.packageName = packageName == null ? "" : packageName;
            this.appLabel = appLabel == null ? "" : appLabel;
        }

        public boolean isBound() { return !packageName.isEmpty(); }
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
            o.put("packageName", "");
            o.put("appLabel", "");
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
                out.add(new Profile(
                        o.getString("id"),
                        o.optString("name", "Profile"),
                        o.optString("packageName", ""),
                        o.optString("appLabel", "")
                ));
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) {
            out.add(new Profile(DEFAULT_ID, context.getString(R.string.default_profile), "", ""));
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
                o.put("packageName", p.packageName);
                o.put("appLabel", p.appLabel);
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

    public synchronized Profile active() {
        String id = activeId();
        for (Profile p : load()) if (p.id.equals(id)) return p;
        return load().get(0);
    }

    public synchronized String activeName() { return active().name; }
    public synchronized String activePackage() { return active().packageName; }
    public synchronized String activeAppLabel() { return active().appLabel; }

    public synchronized void setActive(String id) {
        for (Profile p : load()) {
            if (p.id.equals(id)) {
                prefs.edit().putString(KEY_ACTIVE, id).apply();
                return;
            }
        }
    }

    public synchronized Profile create(String name) {
        return create(name, "", "");
    }

    public synchronized Profile createForGame(String packageName, String appLabel) {
        String label = appLabel == null || appLabel.trim().isEmpty() ? packageName : appLabel.trim();
        for (Profile p : load()) {
            if (!packageName.isEmpty() && packageName.equals(p.packageName)) {
                setActive(p.id);
                return p;
            }
        }
        return create(label, packageName, label);
    }

    private synchronized Profile create(String name, String packageName, String appLabel) {
        List<Profile> profiles = load();
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) clean = "Profile " + (profiles.size() + 1);
        Profile created = new Profile(UUID.randomUUID().toString(), clean, packageName, appLabel);
        profiles.add(created);
        save(profiles);
        setActive(created.id);
        return created;
    }

    public synchronized void bindGame(String id, String packageName, String appLabel) {
        List<Profile> profiles = load();
        for (Profile p : profiles) {
            if (p.id.equals(id)) {
                p.packageName = packageName == null ? "" : packageName;
                p.appLabel = appLabel == null ? "" : appLabel;
                if ((p.name == null || p.name.trim().isEmpty() || p.name.equals(context.getString(R.string.default_profile)))
                        && !p.appLabel.isEmpty()) p.name = p.appLabel;
            }
        }
        save(profiles);
    }

    public synchronized Profile findByPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return null;
        for (Profile p : load()) if (packageName.equals(p.packageName)) return p;
        return null;
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
