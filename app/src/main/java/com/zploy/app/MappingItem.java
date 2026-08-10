package com.zploy.app;

import org.json.JSONException;
import org.json.JSONObject;

public final class MappingItem {
    public static final int KEY_LEFT_STICK = -1001;
    public static final int KEY_RIGHT_STICK = -1002;

    public String id;
    public int keyCode;
    public String label;
    public MappingType type;
    public float x;
    public float y;
    public float radius;
    public float deadZone;
    public float sensitivity;

    public MappingItem(String id, int keyCode, String label, MappingType type,
                       float x, float y, float radius, float deadZone, float sensitivity) {
        this.id = id;
        this.keyCode = keyCode;
        this.label = label;
        this.type = type;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.deadZone = deadZone;
        this.sensitivity = sensitivity;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("keyCode", keyCode);
        o.put("label", label);
        o.put("type", type.name());
        o.put("x", x);
        o.put("y", y);
        o.put("radius", radius);
        o.put("deadZone", deadZone);
        o.put("sensitivity", sensitivity);
        return o;
    }

    public static MappingItem fromJson(JSONObject o) throws JSONException {
        return new MappingItem(
                o.getString("id"),
                o.getInt("keyCode"),
                o.optString("label", "?"),
                MappingType.valueOf(o.getString("type")),
                (float) o.getDouble("x"),
                (float) o.getDouble("y"),
                (float) o.optDouble("radius", 0.08),
                (float) o.optDouble("deadZone", 0.12),
                (float) o.optDouble("sensitivity", 1.0)
        );
    }
}
