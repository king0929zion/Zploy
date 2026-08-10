# Zploy verification report

Date: 2026-08-10
Target: Android 16 / API 36
Primary backend: existing Shizuku instance + Shizuku UserService

## Automated checks completed

1. `MappingMathLogicTest`
   - radial dead-zone zeroing and rescaling
   - negative-axis symmetry
   - normalized coordinate conversion
   - camera response dead-zone
   - nonlinear camera curve monotonicity
   - Result: PASS

2. `ControllerStateLogicTest`
   - digital button passthrough
   - analog LT/RT threshold synthesis
   - HAT X/Y D-pad synthesis
   - Result: PASS

3. Android XML parse
   - manifest
   - accessibility-service configuration
   - Chinese/English strings
   - theme and adaptive icon XML
   - Result: PASS (9 XML files)

4. Localization parity
   - Simplified Chinese and English string keys match
   - Result: PASS (64 keys)

5. Java source parser sanity
   - javac parser invoked across all Java sources
   - unresolved Android/Shizuku classes are expected because this workspace has no Android SDK classpath
   - no parser-pattern errors such as unclosed blocks, illegal starts or missing delimiters
   - Result: PASS (18 Java files)

6. Functional safeguards implemented and statically reviewed
   - Shizuku is the preferred Auto backend
   - existing Shizuku binder is reused; Zploy has no independent wireless-debug pairing workflow
   - UserService uses a stable Shizuku tag/version and reserved destroy AIDL transaction
   - virtual pointer IDs are allocated from currently-free IDs to avoid long-run collisions after profile edits
   - injection is suspended while the overlay editor is open
   - trigger/D-pad axis rising edges can create mappings in the editor
   - Android 16 system-bar insets are handled in the main UI

## Device-only verification still required

This workspace does not expose an Android device, Android SDK/ADB transport, running Shizuku instance, or GameSir Nova 2 Lite hardware. These checks therefore remain pending and are not marked as passed:

- compile/install/run on the user's Android 16 phone
- Shizuku permission handshake and UserService startup on that phone
- Android 16 `InputManagerGlobal` touch injection under the phone's Shizuku shell identity
- exact GameSir Nova 2 Lite HID report for the user's selected controller mode
- simultaneous LS + RS + Tap/Hold behavior inside the target game
- camera direction/sensitivity calibration for the target game
- game/anti-cheat acceptance of injected input

The built-in Test page exists specifically to make this first-device calibration fast and observable.
