# Wahid Word — Android API 17 port status

This directory is the complete upstream WordCraft for Android source at commit
b2b7cf913c6f61604062e2db30962f32ec921b11. It is vendored for porting; upstream
files are not yet claimed to be API 17 compatible.

## Verified blockers

- Upstream Android module sets `minApi = 29` and ships a Kotlin/Jetpack Compose UI.
- Upstream build currently expects a recent Android Gradle Plugin, JDK 17+,
  Android NDK 29 and modern Rust/cargo-ndk.
- Lowering only minSdk/minApi is not a compatibility port.
- Android 4.2 requires API 17 and 32-bit ARM (`armeabi-v7a`) as the first
  practical native target. Modern NDK releases no longer support API 17.

## Port sequence

1. Establish a reproducible build of the unmodified upstream source.
2. Identify the Rust engine crates and compile a minimal engine-only test
   for `armv7-linux-androideabi` with a toolchain that still supports API 17.
3. Audit every native dependency for API 18+ symbols and replace or pin
   incompatible dependencies.
4. Replace the Compose Android frontend with Java Android Views (API 17),
   preserving the Rust document engine behind a narrow JNI interface.
5. Build and run on an Android 4.2 emulator/device; test DOCX open/edit/save,
   then rendering, images, tables and PDF export.
6. Only mark API 17 supported after device-level tests pass.

## Current state

Import only. No API 17 APK has been built or tested yet. Do not lower the
manifest SDK and label the result compatible until all steps above pass.
