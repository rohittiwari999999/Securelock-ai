# SecureLock AI – App Lock & Privacy Protector (Android Studio Project)

Native Kotlin + Jetpack Compose + Material 3 offline-first Android security application targeting **Android 8.0 (API 26)** through **Android 16 (API 36)**.

## Project Setup in Android Studio

1. Extract or open the `android-project` folder in **Android Studio Meerkat / Ladybug (2024.3+)** with **JDK 17**.
2. Let Gradle sync (`AGP 8.9.1`, `Kotlin 2.1.20`, `Compose BOM 2025.05.00`, `compileSdk = 36`, `minSdk = 26`).
3. Run on a physical device or Android Emulator (API 26–36):
   - Enroll a fingerprint in **Android Settings > Security > Fingerprint** (or Emulator Extended Controls > Fingerprint) to test `BiometricPrompt`.
   - On first launch, create a 4–6 digit master PIN and record your one-time Emergency Recovery Code.
4. Build optimized release APK/AAB:
   ```bash
   ./gradlew assembleRelease
   ```
   Release builds enable `isMinifyEnabled = true`, `isShrinkResources = true`, R8 log stripping (`proguard-rules.pro`), and ABI splits (`armeabi-v7a`, `arm64-v8a`, `x86_64`).

## Features Supported via Official Android APIs

- **Keystore & Salted Credential Storage**: AES-256-GCM keys in `AndroidKeyStore` (`KeystoreCryptoManager`) + `PBKDF2WithHmacSHA256` (120,000 iterations) for PIN, pattern, and recovery code hashes. Zero plaintext credentials or hardcoded passwords.
- **BiometricPrompt Authentication**: `androidx.biometric.BiometricPrompt` (`BIOMETRIC_STRONG or BIOMETRIC_WEAK`) in `MainActivity` and `LockOverlayActivity`.
- **Installed App Discovery**: Uses `<queries>` for `ACTION_MAIN` + `CATEGORY_LAUNCHER` on Android 11–16 without broad `QUERY_ALL_PACKAGES`.
- **Intruder Alert & Foreground Camera**: Records failed unlock timestamps locally; offers an explicit, visible CameraX foreground snapshot saved exclusively to `filesDir/intruders/`.
- **Local Notification Privacy Vault**: Optional `NotificationListenerService` masks notification previews for protected apps locally without any network access.
- **Customizable App Protection Cover**: Neutral "App Unavailable" screen before authentication (never mimics Android OS crash dialogs).
- **Camera Privacy Monitor**: Uses `CameraManager.AvailabilityCallback`, checks `CAMERA` runtime status, links to Android System Privacy Settings, and runs a user-controlled CameraX preview test.
- **Private Browser**: Multi-tab `WebView` with `LOAD_NO_CACHE`, cookies disabled, Safe Browsing HTTPS checks, explicit `DownloadManager` confirmation, and full cache/history/WebStorage wipe on exit.
- **Local Statistics & Backup**: Weekly activity Canvas chart, recent security events, and Storage Access Framework (`CreateDocument` / `OpenDocument`) JSON backup/restore.

## Honest Android Platform Limitations & Required Special Permissions

1. **System-Wide App Locking (`PACKAGE_USAGE_STATS` / `BIND_ACCESSIBILITY_SERVICE` + `SYSTEM_ALERT_WINDOW`)**:
   - Ordinary Android apps cannot silently intercept or block other apps.
   - Users must manually grant **Display Over Other Apps** (`SYSTEM_ALERT_WINDOW`) and either **Usage Access** (`PACKAGE_USAGE_STATS`) or **Accessibility Service** (`AppLockAccessibilityService`, configured with `canRetrieveWindowContent="false"`).
2. **Background Camera Restrictions (Android 9–16)**:
   - Android blocks silent background camera capture and displays a hardware green camera privacy dot whenever the camera is active. SecureLock AI only opens CameraX in a visible foreground dialog with explicit user action.
3. **Notification Masking (`BIND_NOTIFICATION_LISTENER_SERVICE`)**:
   - Requires explicit user opt-in in **System Settings > Notification Access**.
4. **Android 14+ Foreground Service (`FOREGROUND_SERVICE_SPECIAL_USE`)**:
   - If the user activates the optional `UsageStatsManager` monitor (`AppLockMonitorService`), Android displays an ongoing notification and requires `POST_NOTIFICATIONS` on Android 13+.
