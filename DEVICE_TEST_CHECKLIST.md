# Zploy Android 16 device test checklist

## Environment

- Shizuku shows Running before Zploy starts.
- Zploy receives Shizuku authorization.
- Zploy Accessibility Service is enabled.
- GameSir Nova 2 Lite is connected in normal Android/HID mode.

## Controller test

Check that the Test page reacts to:

- A / B / X / Y
- LB / RB
- LT / RT from 0.000 to roughly 1.000
- D-pad all four directions
- left stick X/Y
- right stick X/Y
- L3 / R3
- center/menu/view/M/special keys
- rear keys, if they are exposed independently by the current firmware mode

Record any unexpected `Last key: ... (number)` values.

## Mapping smoke test

1. Create a temporary profile.
2. Map A as Tap and verify one touch per press.
3. Map X as Hold and verify touch remains down until release.
4. Move LS and confirm the game joystick follows direction and returns cleanly to center.
5. Move RS and confirm the camera continues turning while held off-center.
6. Hold LS + RS together.
7. While holding both sticks, press Tap and Hold mappings.
8. Open the Z editor and verify configuration button presses do not activate game actions.
9. Close the editor and verify mapping resumes.
10. Confirm translucent markers do not block normal finger touches.

## Camera calibration

- Adjust dead zone until the view does not drift at rest.
- Adjust sensitivity until a full right-stick deflection turns at a comfortable speed.
- If horizontal or vertical direction is reversed in the target game, note it for an inversion toggle in the next build.
