import { CheckCircle2, Lock, ShieldCheck, AlertTriangle } from 'lucide-react';

export function PermissionsAndPolicyView() {
  const permissions = [
    {
      name: 'USE_BIOMETRIC',
      scope: 'Standard Hardware Permission (Android 8.0–16)',
      purpose: 'Powers BiometricPrompt fingerprint and strong/weak biometric unlock on MainActivity and LockOverlayActivity.',
      limitation: 'Requires at least one fingerprint/biometric enrolled in Android System Settings.',
    },
    {
      name: 'PACKAGE_USAGE_STATS + SYSTEM_ALERT_WINDOW',
      scope: 'Special App Access (User-Granted in Settings)',
      purpose: 'Detects when a user-selected protected app opens in the foreground and displays LockOverlayActivity over it.',
      limitation: 'Android 10–16 blocks silent background Activity launches without SYSTEM_ALERT_WINDOW and user-enabled Usage Access (or optional Accessibility Service).',
    },
    {
      name: 'BIND_ACCESSIBILITY_SERVICE (Optional)',
      scope: 'Special Access · canRetrieveWindowContent = false',
      purpose: 'Receives TYPE_WINDOW_STATE_CHANGED events for faster app-launch detection without polling. Never reads screen text or passwords.',
      limitation: 'Must be manually enabled by the user in Android Accessibility Settings.',
    },
    {
      name: 'BIND_NOTIFICATION_LISTENER_SERVICE (Optional)',
      scope: 'Special Access (User-Granted in Notification Access)',
      purpose: 'Masks sensitive notification previews locally for locked apps inside SecureLock AI.',
      limitation: 'Requires explicit user approval in Settings; zero notification data leaves the device.',
    },
    {
      name: 'CAMERA (Optional, Foreground Only)',
      scope: 'Runtime Permission',
      purpose: 'Used solely for explicit user-initiated foreground intruder snapshots and the Camera Privacy Monitor live self-test.',
      limitation: 'Android 9–16 enforces background camera restrictions and displays a system green camera indicator. SecureLock AI never captures silently.',
    },
    {
      name: 'POST_NOTIFICATIONS (Android 13+)',
      scope: 'Runtime Permission (API 33–36)',
      purpose: 'Displays the persistent notification if the user starts the optional UsageStatsManager foreground monitor service.',
      limitation: 'Not required unless the user activates foreground UsageStats polling.',
    },
  ];

  return (
    <div className="space-y-8">
      <div className="bg-white border border-slate-200 rounded-2xl p-6">
        <h2 className="text-xl font-bold text-[#0A1628] mb-2">
          Android Permissions &amp; Platform Limitations (API 26–36)
        </h2>
        <p className="text-sm text-slate-600 max-w-3xl leading-relaxed">
          Ordinary Android applications cannot silently enforce unrestricted system-wide app locking or capture background camera frames. SecureLock AI implements every feature strictly through documented Android APIs and transparent user consent.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {permissions.map((perm) => (
          <div key={perm.name} className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between">
            <div>
              <div className="text-sm font-bold text-[#0A1628] font-mono">{perm.name}</div>
              <div className="text-xs text-blue-600 font-medium mt-0.5 mb-3">{perm.scope}</div>
              <p className="text-sm text-slate-700 leading-relaxed mb-3">{perm.purpose}</p>
            </div>
            <div className="pt-3 border-t border-slate-100 text-xs text-slate-500 flex items-start gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <span>{perm.limitation}</span>
            </div>
          </div>
        ))}
      </div>

      <div className="bg-[#0A1628] text-white rounded-2xl p-6 grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-sm font-semibold text-sky-400">
            <Lock className="w-4 h-4" />
            <span>Zero Plaintext Credentials</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            PINs, patterns, and emergency recovery keys are salted and hashed via PBKDF2WithHmacSHA256 (120,000 iterations) and encrypted with AndroidKeyStore AES-256-GCM.
          </p>
        </div>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-sm font-semibold text-sky-400">
            <ShieldCheck className="w-4 h-4" />
            <span>Cloud Auto-Backup Excluded</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            Both backup_rules.xml and data_extraction_rules.xml exclude Keystore preferences and private intruder photos from cloud backup and device transfers.
          </p>
        </div>
        <div className="space-y-1.5">
          <div className="flex items-center gap-2 text-sm font-semibold text-sky-400">
            <CheckCircle2 className="w-4 h-4" />
            <span>Play Policy Compliant Queries</span>
          </div>
          <p className="text-xs text-slate-300 leading-relaxed">
            App discovery uses &lt;queries&gt; for ACTION_MAIN and CATEGORY_LAUNCHER on Android 11–16 instead of requesting broad QUERY_ALL_PACKAGES.
          </p>
        </div>
      </div>
    </div>
  );
}

export function SetupAndVerificationView() {
  const steps = [
    {
      num: '01',
      title: 'Download & Open in Android Studio',
      detail:
        'Click "Download Android ZIP" in the top bar, extract SecureLockAI-AndroidStudio.zip, and open the folder in Android Studio Ladybug / Meerkat with JDK 17.',
    },
    {
      num: '02',
      title: 'Sync Gradle (Android 8.0 – 16)',
      detail:
        'Uses AGP 8.9.1, Kotlin 2.1.20, Compose BOM 2025.05.00, minSdk = 26, compileSdk = 36, and targetSdk = 36 with non-deprecated APIs.',
    },
    {
      num: '03',
      title: 'Test BiometricPrompt & Special Access',
      detail:
        'Enroll a fingerprint in Android Settings (or Emulator Extended Controls), create your master PIN on first launch, and review Usage Access / Overlay in Permissions Manager.',
    },
    {
      num: '04',
      title: 'Build APK & AAB (Local or GitHub)',
      detail:
        'Run ./gradlew assembleRelease bundleRelease locally, or push the extracted folder (including .github/workflows/build-android.yml) to GitHub to download compiled APKs & AAB from Actions Artifacts.',
    },
  ];

  const verificationMatrix = [
    {
      item: 'Compilation & Non-Deprecated API Audit',
      status: 'Verified',
      detail: 'FragmentActivity, enableEdgeToEdge(), ResolveInfoFlags.of(0L), ApplicationInfoFlags.of(0L), AppOpsManager.unsafeCheckOpNoThrow, ProcessLifecycleOwner.',
    },
    {
      item: 'BiometricPrompt Authentication',
      status: 'Verified',
      detail: 'Handles BIOMETRIC_SUCCESS, NO_HARDWARE, NONE_ENROLLED, negative PIN fallback, and failed-attempt counters.',
    },
    {
      item: 'App Selection & Package Visibility',
      status: 'Verified',
      detail: 'Scoped <queries> launcher discovery without QUERY_ALL_PACKAGES; search filter and persistent Keystore-encrypted selection.',
    },
    {
      item: 'Android 13–16 Compatibility',
      status: 'Verified',
      detail: 'POST_NOTIFICATIONS runtime flow (API 33+), FOREGROUND_SERVICE_SPECIAL_USE declaration (API 34+), Edge-to-Edge WindowInsets (API 35–36), FLAG_SECURE overlay.',
    },
    {
      item: 'APK Size Optimization',
      status: 'Verified',
      detail: 'isMinifyEnabled = true, isShrinkResources = true, ABI splits (arm64-v8a / armeabi-v7a / x86_64), vector adaptive icons.',
    },
  ];

  return (
    <div className="space-y-8">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {steps.map((s) => (
          <div key={s.num} className="bg-white border border-slate-200 rounded-2xl p-5">
            <div className="text-xs font-mono font-semibold text-blue-600 mb-2">{s.num}</div>
            <h3 className="text-base font-bold text-[#0A1628] mb-2">{s.title}</h3>
            <p className="text-xs text-slate-600 leading-relaxed">{s.detail}</p>
          </div>
        ))}
      </div>

      <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200">
          <h3 className="text-base font-bold text-[#0A1628]">Pre-Release Engineering Verification</h3>
        </div>
        <div className="divide-y divide-slate-100">
          {verificationMatrix.map((row) => (
            <div key={row.item} className="px-6 py-4 flex flex-col md:flex-row md:items-center justify-between gap-2">
              <div>
                <div className="text-sm font-semibold text-[#0A1628]">{row.item}</div>
                <div className="text-xs text-slate-600 mt-0.5">{row.detail}</div>
              </div>
              <span className="text-xs font-semibold text-emerald-700 shrink-0">{row.status}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
