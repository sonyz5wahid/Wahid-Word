# Android 4.2 (API 17) Feasibility Audit

**Status:** Initial source audit — 2026-11-13  
**Decision:** Do not use the current ArtCraft Android launcher unchanged. Build a separate API-17-compatible Android shell and prove the document engine in small prototypes before integrating it.

## 1. Repositories inspected

- Wahid Word: `sonyz5wahid/Wahid-Word` (default branch: `main`; repository is writable by the connected account).
- WordCraft engine: `storytold/wordcraft`.
- Android reference builder: `MostafaXS1/artcraft-android`.

The WordCraft workspace declares Rust edition 2024 and Rust version 1.95. Its workspace includes document, layout, DOCX, rendering, PDF, font, and UI crates. Its workspace license is MIT OR Apache-2.0; third-party dependencies retain their own licenses and notices.

## 2. Confirmed Android reference constraints

Inspection of the current ArtCraft Android builder source found:

- Generated Android launcher metadata sets `min_sdk_version = 28` (Android 9).
- Generated Rust Android build target is `aarch64-linux-android`.
- The build invokes Cargo NDK for `arm64-v8a` only.
- The Android project generator uses Android Gradle Plugin 8.7.3 and AndroidX/GameActivity-based integration.
- The builder expects a modern JDK/Android SDK toolchain.

Therefore, its current APK/build pipeline does not meet Wahid Word's API 17 and legacy 32-bit ARM requirements. Changing only `minSdkVersion` would not establish compatibility.

## 3. What can and cannot be concluded yet

### Confirmed

- The WordCraft source is a substantial Rust workspace, not a small embeddable editor library.
- The Android reference builder is currently configured for API 28 and 64-bit ARM.
- The current reference APK size range is above the desired ~50 MB target for several apps; size must be measured after a working build, not promised in advance.

### Not yet proven

- Whether the compiled native library runs correctly on a physical/emulated API 17 device (compile proof now passes; runtime remains untested).
- Whether all native dependencies and transitive build scripts support 32-bit ARM and API 17.
- Whether the rendering stack can run reliably on Android 4.2 devices and their available graphics drivers.
- Whether DOCX round-trip fidelity and PDF generation can be retained within the memory and APK-size limits of old devices.

No compatibility claim should be made until these points are tested on an API 17 emulator/device.

## 4. Recommended implementation strategy

1. Keep the product target and feature requirements in `PRODUCT_SPEC.md`.
2. Create a small Java-based Android shell using framework APIs available on API 17. Avoid making modern AndroidX/Compose/GameActivity a core dependency.
3. Build a minimal API-17 proof app and verify install, launch, touch input, soft keyboard, file access, and lifecycle behavior.
4. Separately prototype the WordCraft Rust document crates for a 32-bit Android target. Start with document model, DOCX import/export, and PDF output; do not bring the desktop UI crate into the legacy shell by default.
5. If the engine cannot meet API 17, evaluate a smaller independent document pipeline rather than shipping an APK that installs but crashes or silently loses document content.
6. Integrate the engine behind a narrow Java/JNI interface only after the native prototype passes.
7. Add features in tested vertical slices: create/edit/save; DOCX open/save round-trip; formatting; images/tables; PDF export; advanced review/layout tools.
8. Measure APK size and memory on real legacy hardware. Treat ~50 MB as a target, not a release guarantee.

## 5. Release gates

A release is not API-17-compatible until all of these pass on API 17:

- APK installs and launches.
- Keyboard input, selection, clipboard, and basic editing work.
- Documents save and reopen without data loss.
- DOCX import/export round-trips preserve the supported formatting.
- PDF export produces a readable multi-page document.
- The app survives background/foreground and low-memory conditions.
- Unsupported DOCX features are reported rather than silently discarded.

## 6. Current project status

**Native compile milestone passed (2026-11-13):**

- Built the actual `wordcraft-android` Rust library with Rust 1.99.0.
- Target: `armv7-linux-androideabi` / Android ABI `armeabi-v7a`.
- NDK: 23.2.8568313.
- Platform: Android API 17.
- Command: `cargo ndk -t armeabi-v7a --platform 17 build --release -p wordcraft-android --lib`.
- GitHub Actions uploaded `libwordcraft_android.so` as artifact `wordcraft-armv7-api17-native-poc` (about 28 MB before compression).

This proves the full native crate compiles for the API-17 ARMv7 target. It does **not** yet prove that it loads or runs on an Android 4.2 device, nor that the current Java editor calls it. The next milestone is packaging this library in the app, generating/connecting its UniFFI bindings, and testing runtime loading before implementing the document workflows.

The current Java UI remains an early editor shell. DOCX import/export, PDF export, images/tables, and the complete WordCraft editing surface are not yet release-ready. No release APK should be published until the engine is integrated and tested on API 17.
