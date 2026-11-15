# AssistX

**A permission-aware Android floating assistant and quick-action utility.**

AssistX is a native Android application built with Kotlin and Jetpack Compose. The project includes a configurable settings experience and a floating-ball service intended to provide quick access to supported assistant actions.

> **Project status:** The Android source currently identifies the app as AssistX, version 1.5.0 (version code 8). The presence of a service or permission in the manifest is not proof that a feature works on every device. Validate overlay, camera, usage-access and background behaviour on each supported Android/OEM combination.

## Project information

| Field | Value |
|---|---|
| Application ID | `com.aistudio.assistx.fkrq` |
| Version | `1.5.0` (version code `8`) |
| Minimum Android | Android 7.0 (API 24) |
| Target SDK | API 36 |
| Primary language | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Repository | [puttishiva147/Flux-assist-](https://github.com/puttishiva147/Flux-assist-) |
| Gradle root project | `AssistX` |
| Default branch | `main` |

## Current source areas

- Main settings screen and ViewModel-backed settings state.
- Light, dark and system theme selection.
- Floating assistant ball implemented through an Android service.
- Notification permission request and permission-state refresh.
- Camera and usage-access capabilities declared for supported workflows.
- Foreground-service and boot-related integration declared in the Android manifest.

Consult the current source before assuming additional behaviour is available. This README intentionally does not describe unverified features as complete.

## Technology

- Kotlin
- Android SDK
- Jetpack Compose / Material 3
- AndroidX Activity and lifecycle
- Kotlin Coroutines
- Room / DataStore dependencies
- Firebase and Google Services Gradle integration
- Roborazzi and Android test dependencies

The version catalogue in `gradle/libs.versions.toml` and `app/build.gradle.kts` are authoritative.

## Build

1. Open the repository in Android Studio.
2. Install the Android SDK required by the project (API 36).
3. Sync Gradle.
4. Build the debug APK:

```bash
./gradlew assembleDebug
```

Run unit tests:

```bash
./gradlew testDebugUnitTest
```

The debug APK is normally written under `app/build/outputs/apk/debug/`.

## Permissions and safety

Depending on the feature used, Android may request notification, overlay, camera, usage-access, foreground-service or boot permissions. Some are special app access settings rather than ordinary runtime permissions.

- Request permissions only when their related feature is used.
- Explain why each permission is needed.
- Keep the floating control dismissible and user-controlled.
- Do not imply that overlays or background services are guaranteed to remain active; Android and OEM battery policies can stop or restrict them.

## Security

- Never commit API keys, Firebase private keys, signing keystores or passwords.
- Store secrets in local, ignored configuration or GitHub Actions secrets.
- Review exported components and service permissions before release.
- A debug APK is for testing and is not a production-signed distribution.

## Credits

**Project owner and creator:** Shiva Putti  
**GitHub:** [@puttishiva147](https://github.com/puttishiva147)  
**Project:** AssistX (repository: Flux-assist-)

Third-party libraries and Android platform components retain their respective authorship and licence terms. See dependency metadata and upstream project licences.

---

_Last documentation review: 15 November 2026. This is a documentation review date, not the repository creation date or last source-code update._
