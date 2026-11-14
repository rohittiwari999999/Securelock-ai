import JSZip from 'jszip';

export interface ProjectFileEntry {
  path: string;
  name: string;
  category: string;
  content: string;
  language: 'kotlin' | 'xml' | 'gradle' | 'markdown' | 'proguard';
}

export type SimulatorScreenId =
  | 'unlock'
  | 'dashboard'
  | 'app_lock'
  | 'intruder_selfie'
  | 'lock_notifications'
  | 'fake_crash'
  | 'camera_monitor'
  | 'private_browser'
  | 'statistics'
  | 'settings'
  | 'permissions';

export interface ScreenArchitectureSpec {
  id: SimulatorScreenId;
  title: string;
  kotlinPath: string;
  apis: string[];
  androidCompatibility: string;
  summary: string;
}

const rawModules = import.meta.glob('/android-project/**/*', {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

function detectLanguage(path: string): ProjectFileEntry['language'] {
  if (path.endsWith('.kt')) return 'kotlin';
  if (path.endsWith('.xml')) return 'xml';
  if (path.endsWith('.md')) return 'markdown';
  if (path.endsWith('.pro')) return 'proguard';
  return 'gradle';
}

function detectCategory(path: string): string {
  if (path.includes('/ui/screens/')) return 'Compose Screens';
  if (path.includes('/ui/components/') || path.includes('/ui/theme/') || path.includes('/ui/navigation/')) {
    return 'UI Theme & Navigation';
  }
  if (path.includes('/security/') || path.includes('/data/')) return 'Security & Keystore';
  if (path.includes('/service/')) return 'Android Services';
  if (path.includes('/res/')) return 'Resources & Manifest';
  if (path.includes('/test/')) return 'Unit Tests';
  if (path.endsWith('.kt')) return 'App Entry & Activities';
  return 'Gradle & Configuration';
}

export const ANDROID_PROJECT_FILES: ProjectFileEntry[] = Object.entries(rawModules)
  .map(([fullPath, content]) => {
    const cleanPath = fullPath.replace(/^\/android-project\//, '');
    const parts = cleanPath.split('/');
    return {
      path: cleanPath,
      name: parts[parts.length - 1] ?? cleanPath,
      category: detectCategory(cleanPath),
      content,
      language: detectLanguage(cleanPath),
    };
  })
  .sort((a, b) => a.path.localeCompare(b.path));

export const SCREEN_SPECS: Record<SimulatorScreenId, ScreenArchitectureSpec> = {
  unlock: {
    id: 'unlock',
    title: 'Fingerprint, PIN & Pattern Unlock',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/UnlockScreen.kt',
    apis: [
      'androidx.biometric.BiometricPrompt',
      'BiometricManager.Authenticators.BIOMETRIC_STRONG',
      'PBKDF2WithHmacSHA256 (120,000 iterations)',
      'AndroidKeyStore AES-256-GCM',
    ],
    androidCompatibility:
      'MainActivity extends FragmentActivity for BiometricPrompt compatibility on Android 8.0 through Android 16. Credentials are never stored in plaintext or hardcoded.',
    summary:
      'Verifies user identity via BiometricPrompt, salted PIN, or 3x3 Pattern Canvas, logging local attempts and offering an explicit foreground camera prompt after repeated failures.',
  },
  dashboard: {
    id: 'dashboard',
    title: '7-Module Security Dashboard',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/DashboardScreen.kt',
    apis: [
      'Material 3 LazyVerticalGrid',
      'ProcessLifecycleOwner auto-relock',
      'StateFlow reactive storage',
    ],
    androidCompatibility:
      'Uses enableEdgeToEdge() and Material 3 adaptive grid sizing for phones, foldables, and tablets on Android 15–16.',
    summary:
      'Deep Navy header with Keystore status and 7 white rounded security cards: App Lock, Intruder Selfie, Lock Notifications, Fake App Crash, Camera Privacy Monitor, Private Browser, and Statistics.',
  },
  app_lock: {
    id: 'app_lock',
    title: 'App Lock & Installed App Selector',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/AppLockScreen.kt',
    apis: [
      'PackageManager.queryIntentActivities (<queries>)',
      'UsageStatsManager (PACKAGE_USAGE_STATS)',
      'AppLockAccessibilityService (TYPE_WINDOW_STATE_CHANGED)',
      'Settings.canDrawOverlays (SYSTEM_ALERT_WINDOW)',
    ],
    androidCompatibility:
      'Uses <queries> for ACTION_MAIN + CATEGORY_LAUNCHER instead of broad QUERY_ALL_PACKAGES. Explicitly discloses that Usage Access / Accessibility + Overlay permissions are required on Android 10–16.',
    summary:
      'Lists user-launchable installed apps, configures per-app lock toggles, protects System Settings, and sets automatic relock grace timeouts.',
  },
  intruder_selfie: {
    id: 'intruder_selfie',
    title: 'Intruder Alert & Foreground Capture',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/IntruderSelfieScreen.kt',
    apis: [
      'CameraX ProcessCameraProvider + ImageCapture',
      'ActivityResultContracts.RequestPermission (CAMERA)',
      'App-private filesDir/intruders/',
    ],
    androidCompatibility:
      'Strictly respects Android 9–16 background camera restrictions. Never triggers silent background camera access; offers a visible foreground capture prompt and stores images privately.',
    summary:
      'Tracks failed authentication timestamps locally, lets the user capture a foreground verification snapshot, and provides private viewing and deletion.',
  },
  lock_notifications: {
    id: 'lock_notifications',
    title: 'Lock Notifications Privacy Vault',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/LockNotificationsScreen.kt',
    apis: [
      'NotificationListenerService',
      'NotificationManagerCompat.getEnabledListenerPackages',
      'AndroidKeyStore AES-256-GCM local log',
    ],
    androidCompatibility:
      'Requires user opt-in in Android Settings > Notification Access. Masked previews remain strictly on-device.',
    summary:
      'Redacts sensitive notification titles and message bodies for protected packages and stores masked records in an offline vault.',
  },
  fake_crash: {
    id: 'fake_crash',
    title: 'Customizable App Protection Cover',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/FakeCrashScreen.kt',
    apis: [
      'LockOverlayActivity (FLAG_SECURE)',
      'AppProtectionGateScreen Composable',
    ],
    androidCompatibility:
      'Presents a neutral "App Unavailable" protection cover before authentication while strictly avoiding imitation of Android OS system crash dialogs.',
    summary:
      'Allows custom heading, neutral message, cover icon, and background color, with a direct path to authenticate and unlock.',
  },
  camera_monitor: {
    id: 'camera_monitor',
    title: 'Camera Privacy Monitor',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/CameraPrivacyMonitorScreen.kt',
    apis: [
      'CameraManager.AvailabilityCallback',
      'Settings.ACTION_PRIVACY_SETTINGS (Android 12+)',
      'CameraX PreviewView self-test',
    ],
    androidCompatibility:
      'Displays real-time sensor availability via CameraManager.AvailabilityCallback and directs users to Android 12–16 system Privacy Dashboard controls.',
    summary:
      'Shows CAMERA permission state, active sensor availability, OS privacy indicator guidance, and a user-initiated CameraX preview test.',
  },
  private_browser: {
    id: 'private_browser',
    title: 'Private Browser',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/PrivateBrowserScreen.kt',
    apis: [
      'android.webkit.WebView (LOAD_NO_CACHE)',
      'WebSettingsCompat.setSafeBrowsingEnabled',
      'CookieManager & WebStorage wipe',
      'DownloadManager confirmation',
    ],
    androidCompatibility:
      'Uses androidx.webkit Safe Browsing on API 26–36, disables cookies and DOM storage, warns on unencrypted HTTP URLs, and clears all data on exit.',
    summary:
      'Multi-tab incognito browser with DuckDuckGo search, back/forward navigation, HTTP safety prompts, and confirmed downloads.',
  },
  statistics: {
    id: 'statistics',
    title: 'Local Statistics Dashboard',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/StatisticsScreen.kt',
    apis: [
      'Compose Canvas weekly bar chart',
      'Kotlinx Serialization encrypted local JSON',
    ],
    androidCompatibility:
      '100% local-only analytics excluded from Android cloud auto-backup via backup_rules.xml and data_extraction_rules.xml.',
    summary:
      'Displays protected app counts, total/successful/failed unlocks, a 7-day stacked activity chart, and a chronological security event log.',
  },
  settings: {
    id: 'settings',
    title: 'Settings, Backup & Recovery',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/SettingsScreen.kt',
    apis: [
      'ActivityResultContracts.CreateDocument',
      'ActivityResultContracts.OpenDocument',
      'SecureRandom recovery code generation',
    ],
    androidCompatibility:
      'Uses Storage Access Framework for local JSON backup/restore without requiring broad external storage permissions on Android 10–16.',
    summary:
      'Manages PIN/Pattern updates, BiometricPrompt toggle, auto-lock timing, theme, language, local backup/restore, and emergency recovery.',
  },
  permissions: {
    id: 'permissions',
    title: 'Permissions Manager',
    kotlinPath: 'app/src/main/java/com/securelock/ai/security/PermissionHelper.kt',
    apis: [
      'AppOpsManager.unsafeCheckOpNoThrow',
      'Settings.canDrawOverlays',
      'POST_NOTIFICATIONS (Android 13+)',
    ],
    androidCompatibility:
      'Handles runtime permissions and deep-links to Android Special App Access screens, refreshing live on ON_RESUME.',
    summary:
      'Transparent status tracker for Overlay, Usage Access, Accessibility, Notification Listener, Foreground Camera, and Android 13+ Notifications.',
  },
};

export async function downloadAndroidProjectZip(): Promise<void> {
  const zip = new JSZip();
  const rootFolder = zip.folder('SecureLockAI-AndroidStudio');
  if (!rootFolder) return;

  for (const file of ANDROID_PROJECT_FILES) {
    rootFolder.file(file.path, file.content);
  }

  const blob = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'SecureLockAI-AndroidStudio.zip';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
