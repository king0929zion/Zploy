package com.zploy.app;

public final class MappingMathLogicTest {
    private static void near(float expected, float actual, float eps, String name) {
        if (Math.abs(expected - actual) > eps) {
            throw new AssertionError(name + ": expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        near(0f, MappingMath.applyDeadZone(0.09f, 0.10f), 0.0001f, "deadzone zero");
        near(0f, MappingMath.applyDeadZone(-0.10f, 0.10f), 0.0001f, "deadzone edge");
        near(1f, MappingMath.applyDeadZone(1f, 0.10f), 0.0001f, "deadzone max");
        near(-1f, MappingMath.applyDeadZone(-1f, 0.10f), 0.0001f, "deadzone negative max");
        near(0.5f, MappingMath.normalizedToPixels(0.5f, 1), 0.0001f, "normalized");
        near(0f, MappingMath.cameraCurve(0.05f, 0.10f, 1.45f), 0.0001f, "camera deadzone");
        float a = MappingMath.cameraCurve(0.4f, 0.10f, 1.45f);
        float b = MappingMath.cameraCurve(0.8f, 0.10f, 1.45f);
        if (!(b > a && a > 0f)) throw new AssertionError("camera curve monotonic");
        System.out.println("MappingMathLogicTest: PASS");
    }
}
