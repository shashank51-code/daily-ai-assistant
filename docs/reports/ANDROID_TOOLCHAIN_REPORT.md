# Android Toolchain Report

Date: 2026-11-19 (toolchain prepared 2026-11-17; build re-verified 2026-11-19)

## Environment inspection (before changes)

| Item | State found |
|---|---|
| Node / npm | Node 24.15.0, npm 12.0.2 |
| Shell Java | Oracle JDK 25.0.1 (`java -version`); Java 8 also installed; `JAVA_HOME` unset |
| JDK 17 / JDK 21 | Not installed on the system |
| Android SDK | No `ANDROID_HOME`, `ANDROID_SDK_ROOT`, Android Studio or SDK platforms |
| Capacitor | 6.2.3 (CLI/Core/Android) |
| Android Gradle Plugin | 8.2.1 |
| Gradle wrapper | 8.2.1 |
| App SDK levels | min 22, compile/target 34 |
| Docker / Chrome | Docker Desktop and Chrome installed |

## Blockers identified

The previous Capacitor 7 trial was blocked because it required JDK 21 and Android SDK platform 34, while the machine had neither a usable JDK 17/21 nor any Android SDK. The existing Capacitor 6 project used JDK 17 + API 34. The adopted Capacitor 8 target requires JDK 21 + API 36.

## Prepared project-local toolchain (no global changes)

| Item | Prepared toolchain |
|---|---|
| JDK 17 | Temurin 17.0.21+9 at `.tools/jdk-17/jdk-17.0.21+9` |
| JDK 21 | Temurin 21.0.13+9 at `.tools/jdk-21/jdk-21.0.13+9` |
| Android SDK | `.tools/android-sdk` |
| SDK platforms | `android-34`, `android-36` (platform 35 not installed; not required by Cap 8) |
| Build tools | 34.0.0, 35.0.0, 36.0.0 |
| platform-tools | Installed |
| Android Gradle Plugin | 8.13.0 |
| Gradle wrapper | 8.14.3 (downloaded on first build) |
| App SDK levels | min 24, compile/target 36 |
| Capacitor | 8.6.2 |

Downloads were verified against published SHA-256 checksums. The Google SDK license prompt was accepted under the user's explicit instruction. No system-wide `JAVA_HOME`, `ANDROID_HOME` or `ANDROID_SDK_ROOT` was set, and the existing Oracle JDK 25 / Java 8 installations were left untouched. `.tools/` is gitignored and is not part of the repository.

## How the build selects the toolchain

`frontend/scripts/android-build.mjs`:

1. Reads the installed `@capacitor/core` major from `package.json`.
2. Requires JDK 21 for Capacitor ≥ 7 (JDK 17 for Capacitor 6), searching `JAVA_HOME`, `.tools/jdk-<major>`, then `~/.jdks`.
3. Requires API 36 for Capacitor ≥ 8 (35 for Cap 7, 34 for Cap 6), searching `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `.tools/android-sdk`, then default per-user locations.
4. Writes the gitignored `android/local.properties` if absent.
5. Runs `vite build --mode android`, `npx cap sync android`, then `gradlew assembleDebug` with JDK/SDK passed only to that child process.

This preserves the existing Capacitor 6/7 path (JDK 17 + API 34/35) while supporting the adopted Capacitor 8 toolchain.

## Build verification (2026-11-19)

```text
$ cd frontend && npm run android:build
JDK:  ...\.tools\jdk-21\jdk-21.0.13+9
SDK:  ...\.tools\android-sdk

==> Building web assets for Android      PASS
==> Syncing Capacitor                    PASS
==> Assembling debug APK                 PASS
BUILD SUCCESSFUL in 2m 5s
APK: frontend/android/app/build/outputs/apk/debug/app-debug.apk
```

A build requires `frontend/.env.android` with a non-localhost `VITE_API_BASE_URL`; the verification used `http://10.0.2.2:8080` (emulator loopback). That file is gitignored; the repository only contains `.env.android.example`.

APK facts:

- Path: `frontend/android/app/build/outputs/apk/debug/app-debug.apk`
- Size: 4,260,031 bytes
- SHA-256: `1F3BFACC0BBE3621DE0967EE7CC15F7BA999E3A6A0CF14D75F6FAB8832A034BB`
- `aapt dump badging`: package `com.dailyai.assistant`, versionCode 1, minSdk 24, targetSdk 36, compileSdk 36, INTERNET permission.

## Limitations

- No emulator or connected Android device and no Android Studio: APK installation, native UI runtime, device deep links and Capacitor bridge behavior were not tested on-device.
- SDK platform 35 was not installed because Capacitor 8 requires 36 and the previous Capacitor 6 path needs 34.
- The APK is a debug build; release signing/minification and Play distribution were not performed.

Official references: [Capacitor 8 migration](https://capacitorjs.com/docs/updating/8-0), [Capacitor environment setup](https://capacitorjs.com/docs/getting-started/environment-setup), [AGP 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes), [Android SDK Manager](https://developer.android.com/tools/sdkmanager).
