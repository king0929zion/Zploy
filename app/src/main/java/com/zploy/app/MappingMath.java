package com.zploy.app;

public final class MappingMath {
    private MappingMath() {}

    public static float magnitude(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float applyDeadZone(float value, float deadZone) {
        float a = Math.abs(value);
        if (a <= deadZone) return 0f;
        float scaled = (a - deadZone) / Math.max(0.0001f, 1f - deadZone);
        return Math.copySign(clamp(scaled, 0f, 1f), value);
    }

    public static float cameraCurve(float value, float deadZone, float exponent) {
        float v = applyDeadZone(value, deadZone);
        if (v == 0f) return 0f;
        return Math.copySign((float) Math.pow(Math.abs(v), exponent), v);
    }

    public static float normalizedToPixels(float normalized, int pixels) {
        return clamp(normalized, 0f, 1f) * pixels;
    }
}
