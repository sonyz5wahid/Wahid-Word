# Wahid Word

Wahid Word is a rebranded Android build of the open-source [WordCraft for Android](https://github.com/artcraft-android/wordcraft-android) project. The goal is to ship the real WordCraft Android application under the **Wahid Word** name, not to recreate its interface with a placeholder editor.

## Upstream and license

- Android application and UI: [artcraft-android/wordcraft-android](https://github.com/artcraft-android/wordcraft-android)
- Document engine: [storytold/wordcraft](https://github.com/storytold/wordcraft)
- Upstream license: MIT OR Apache-2.0. Original copyright and third-party notices are retained in the upstream source tree.
- Wahid Word is an independent rebrand and is not affiliated with or endorsed by Microsoft. Microsoft Word is a Microsoft trademark.

## Android project

The Android Studio/Gradle project is:

`upstream/wordcraft-android/apps/wordcraft-android/android/`

The full upstream Rust workspace is retained at `upstream/wordcraft-android/`. The Android app uses Kotlin, Jetpack Compose, UniFFI/JNA and the real Rust WordCraft document engine.

## Compatibility

This rebrand follows the upstream Android app's requirements. **Minimum Android version: Android 10 (API 29)**. Android 4.2/API 17 is no longer a target for this project.

## Build

Requirements: JDK 17, Android SDK Platform 37, Android NDK 29.0.14206865, Rust 1.99.0, and cargo-ndk 4.1.2.

From the Android project directory:

```bash
cd upstream/wordcraft-android/apps/wordcraft-android/android
./gradlew assembleDebug
```

GitHub Actions builds the debug APK and uploads it as the `wahid-word-debug` artifact. A successful compile is not a substitute for installation and real-device testing; release readiness must be assessed separately.

## Branding

The launcher label is **Wahid Word** and the launcher artwork is a custom blue Word-style document icon. Upstream WordCraft credits, licenses, and third-party attributions remain part of the project.
