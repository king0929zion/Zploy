# Zploy

Zploy is a personal Android gamepad-to-touch mapper built for Android 16 and optimized around a GameSir Nova 2 Lite controller.

## What is implemented

- **Shizuku-first backend.** Zploy connects to an already-running Shizuku instance and requests only Zploy's Shizuku authorization. It does not create a second wireless-debugging pairing flow.
- **Privileged touch injection.** A Shizuku `UserService` runs as shell/root identity and injects multi-pointer `MotionEvent` streams through Android's framework input manager.
- **Accessibility controller capture.** `AccessibilityService` receives gamepad keys globally and captures joystick motion events on Android 14+.
- **Four mapping types.** Tap, Hold, virtual Joystick, and Camera drag.
- **Simultaneous multi-touch.** Left-stick movement, right-stick camera, and buttons can be active at the same time.
- **Nova 2 Lite compatibility helpers.** LT/RT can be synthesized from analog trigger axes, and the D-pad can be synthesized from HAT_X/HAT_Y if the controller mode does not emit digital key events.
- **In-game overlay editor.** A movable `Z` bubble opens a full-screen editor. Press a physical controller button to add/select its mapping, then drag the marker over the game control.
- **Semi-transparent play markers.** Mapping labels remain visible at configurable opacity while playing and do not consume touch input.
- **Analog tuning in the overlay.** Stick sensitivity, dead zone, and joystick radius are adjustable without leaving the game.
- **Multiple local profiles.** Create, rename, delete and switch separate mapping sets for different games/configurations.
- **Controller test page.** Live GameSir-style controller visualization plus raw stick, trigger, HAT, pressed-key and numeric key-code data.
- **Bilingual UI.** Simplified Chinese and English, including follow-system language behavior.
- **Android 16 layout handling.** The main UI accounts for system-bar insets under edge-to-edge behavior.

## Runtime architecture

```text
GameSir Nova 2 Lite
        |
        v
AccessibilityService
  KeyEvent / MotionEvent
        |
        v
ControllerState
        |
        v
MappingEngine @ ~60 Hz
        |
        v
TouchSynthesizer
        |
        v
Shizuku UserService
        |
        v
InputManagerGlobal
        |
        v
Android touchscreen stream
        |
        v
Game
```

The accessibility path also contains a basic Tap/Hold compatibility backend. Full joystick + camera + simultaneous buttons is intended to use Shizuku.

## Build target

- minSdk: 34
- targetSdk / compileSdk: 36 (Android 16)
- Android Gradle Plugin: 8.10.1
- Gradle required by AGP 8.10: 8.11.1
- JDK: 17
- Shizuku API/provider: 13.1.5

Open the project in a current Android Studio, ensure Android SDK Platform 36 is installed, then build the `app` module. This source package does not include downloaded Android SDK or Gradle binaries.

## First-run setup

1. Start Shizuku normally. If Shizuku is already running, do not pair wireless debugging again for Zploy.
2. Open Zploy and authorize it on the Shizuku card.
3. Open Android Accessibility settings and enable Zploy.
4. Connect the GameSir Nova 2 Lite in its normal Android/HID mode.
5. Open **Test**. Press every front/shoulder/rear/special button and move both sticks, triggers and D-pad. Check the raw key code/axis output.
6. Create or select a profile under **Mapping** / **Settings**.
7. Start mapping, enter the game, then tap the floating `Z` button.
8. Press a controller button to add/select a marker. Drag it over the game's matching button and select Tap or Hold.
9. Position `LS` on the game's movement joystick. Position `RS` in a safe camera-drag area and tune sensitivity/dead zone.
10. Close the editor. Zploy resumes injection and the markers remain at low opacity.

## Nova 2 Lite calibration note

The test screen deliberately shows raw values instead of assuming that every firmware/controller mode reports M, rear buttons, triggers and D-pad identically. If a special/rear button is firmware-remapped to another physical button, Android may expose only the resulting key code. The first real-device test tells us exactly what the user's controller mode provides.

## Verification in this workspace

Run:

```bash
tools/verify.sh
```

Current automated result:

- Mapping math/dead-zone/camera-curve logic: PASS
- Trigger + HAT digital synthesis: PASS
- Android XML/manifest parse: PASS
- Chinese/English resource-key parity: PASS
- Java parser sanity: PASS
- Project/source/AIDL structure: PASS

A physical Android 16 device, Shizuku runtime and Nova 2 Lite hardware are still required for the final privileged-input and in-game behavior checks. This environment has no Android SDK/ADB/device attached, so it would be inaccurate to claim those device-only checks have already passed.

## Private-use note

Games can have their own rules regarding external input mapping and injected touch input. Check the rules applicable to the game/account before using Zploy online.
