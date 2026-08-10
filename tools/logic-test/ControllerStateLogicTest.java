package com.zploy.app;

import android.view.KeyEvent;
import java.util.HashSet;
import java.util.Set;

public final class ControllerStateLogicTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Set<Integer> keys = new HashSet<>();
        keys.add(KeyEvent.KEYCODE_BUTTON_A);
        ControllerState s = new ControllerState("GameSir", 7,
                0, 0, 0, 0,
                0.70f, 0.80f, -1f, 1f,
                keys, KeyEvent.KEYCODE_BUTTON_A);
        check(s.pressed(KeyEvent.KEYCODE_BUTTON_A), "digital A");
        check(s.pressed(KeyEvent.KEYCODE_BUTTON_L2), "analog LT -> L2");
        check(s.pressed(KeyEvent.KEYCODE_BUTTON_R2), "analog RT -> R2");
        check(s.pressed(KeyEvent.KEYCODE_DPAD_LEFT), "hat left");
        check(s.pressed(KeyEvent.KEYCODE_DPAD_DOWN), "hat down");
        check(!s.pressed(KeyEvent.KEYCODE_DPAD_RIGHT), "hat right false");
        check(!s.pressed(KeyEvent.KEYCODE_DPAD_UP), "hat up false");

        ControllerState low = new ControllerState("GameSir", 7,
                0, 0, 0, 0,
                0.40f, 0.54f, 0f, 0f,
                new HashSet<>(), 0);
        check(!low.pressed(KeyEvent.KEYCODE_BUTTON_L2), "LT threshold");
        check(!low.pressed(KeyEvent.KEYCODE_BUTTON_R2), "RT threshold");
        System.out.println("ControllerStateLogicTest: PASS");
    }
}
