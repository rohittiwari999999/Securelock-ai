import JSZip from 'jszip';

export interface ProjectFileEntry {
  path: string;
  name: string;
  category: string;
  content: string;
  language: 'kotlin' | 'xml' | 'gradle' | 'markdown' | 'proguard' | 'yaml';
}

export type SimulatorScreenId =
  | 'unlock'
  | 'dashboard'
  | 'app_lock'
  | 'vault'
  | 'intruder_selfie'
  | 'lock_notifications'
  | 'fake_crash'
  | 'camera_monitor'
  | 'private_browser'
  | 'statistics'
  | 'theme_customization'
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

const rawModules = import.meta.glob(['/android-project/**/*', '/android-project/.github/**/*'], {
  query: '?raw',
  import: 'default',
  eager: true,
}) as Record<string, string>;

function detectLanguage(path: string): ProjectFileEntry['language'] {
  if (path.endsWith('.kt')) return 'kotlin';
  if (path.endsWith('.xml')) return 'xml';
  if (path.endsWith('.md')) return 'markdown';
  if (path.endsWith('.pro')) return 'proguard';
  if (path.endsWith('.yml') || path.endsWith('.yaml')) return 'yaml';
  return 'gradle';
}

function detectCategory(path: string): string {
  if (path.startsWith('.github/')) return 'CI/CD (GitHub Actions)';
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
    title: 'Multi-Method Unlock Gate & Biometrics',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/UnlockScreen.kt',
    apis: [
      'androidx.biometric.BiometricPrompt',
      '4-digit / 6-digit / Custom PIN + Pattern + Password',
      'PBKDF2WithHmacSHA256 (120,000 iterations)',
      'Per-app LockMethod resolution',
    ],
    androidCompatibility:
      'MainActivity extends FragmentActivity for BiometricPrompt compatibility on Android 8.0–16. Dynamically adopts all 8 themes and custom accent colors.',
    summary:
      'Supports 4/6/custom PINs, 3x3 Pattern Lock, Alphanumeric Passwords, and BiometricPrompt with configurable intruder attempt threshold and foreground snapshot prompt.',
  },
  dashboard: {
    id: 'dashboard',
    title: '9-Module Security Dashboard',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/DashboardScreen.kt',
    apis: [
      'Material 3 LazyVerticalGrid (9 modules)',
      'ProcessLifecycleOwner + ACTION_SCREEN_OFF relock',
      'StateFlow reactive encrypted storage',
    ],
    androidCompatibility:
      'Uses enableEdgeToEdge() and Material 3 adaptive grid sizing for phones, foldables, and tablets on Android 15–16.',
    summary:
      'Premium 9-card dashboard: App Lock, Secret Gallery Vault, Intruder Detection, Lock Notifications, Fake Crash Screen, Camera Privacy, Private Browser, Statistics, and Theme Customization.',
  },
  app_lock: {
    id: 'app_lock',
    title: 'App Lock & Per-App Auth Preferences',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/AppLockScreen.kt',
    apis: [
      'PackageManager.queryIntentActivities (<queries>)',
      'Per-app LockMethod overrides',
      'Intent.ACTION_SCREEN_OFF immediate relock',
      'UsageStatsManager / Accessibility + Overlay',
    ],
    androidCompatibility:
      'Uses <queries> for ACTION_MAIN + CATEGORY_LAUNCHER on Android 11–16. Discloses Usage Access / Accessibility + Display Over Other Apps requirements honestly.',
    summary:
      'Select installed apps, assign per-app lock methods (PIN, pattern, password), enable lock-on-screen-off, and configure relock delays.',
  },
  vault: {
    id: 'vault',
    title: 'Secret Gallery Vault (AES-256-GCM)',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/VaultScreen.kt',
    apis: [
      'AndroidKeyStore AES-256-GCM binary file cipher',
      'ActivityResultContracts.OpenMultipleDocuments',
      'In-memory image decoding + wiped temp video playback',
      'StatFs storage meter & SAF encrypted backup/restore',
    ],
    androidCompatibility:
      'Stores encrypted .slv files exclusively inside app-private filesDir/vault/ without leaving unprotected copies. Uses SAF for multi-select import and authenticated export on Android 8–16.',
    summary:
      'Private photo & video vault with multiple albums (create, rename, safe delete), PIN/fingerprint gate, grid/list layouts, search, private viewer, and encrypted backup/restore.',
  },
  intruder_selfie: {
    id: 'intruder_selfie',
    title: 'Intruder Detection & Foreground Capture',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/IntruderSelfieScreen.kt',
    apis: [
      'Configurable attempt threshold (1, 2, 3, 5 tries)',
      'CameraX ProcessCameraProvider + ImageCapture',
      'Keystore-encrypted private intruder storage',
    ],
    androidCompatibility:
      'Strictly respects Android 14–16 background camera restrictions. Never triggers silent background camera access or bypasses hardware privacy dots.',
    summary:
      'Tracks failed authentication attempts with timestamps, target app, and lock method; supports explicit foreground camera snapshots and encrypted event gallery deletion.',
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
    title: 'Fake Crash / Simulated Decoy Screens',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/FakeCrashScreen.kt',
    apis: [
      'LockOverlayActivity (FLAG_SECURE)',
      'DecoyMode enum (6 simulated templates)',
    ],
    androidCompatibility:
      'Triggers strictly inside SecureLock protection flow, requires authentication to exit, clearly marks screens as simulated, and never impersonates genuine OS dialogs.',
    summary:
      'Choose from App Has Stopped (Simulated), Application Unavailable, Loading Error, Network Error, Blank Screen, or Custom Error Message.',
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
      'Displays real-time sensor availability via CameraManager.AvailabilityCallback and links to Android 12–16 system Privacy Dashboard controls.',
    summary:
      'Shows CAMERA permission state, sensor availability, OS privacy indicator guidance, and a user-controlled CameraX preview test.',
  },
  private_browser: {
    id: 'private_browser',
    title: 'Private Browser',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/PrivateBrowserScreen.kt',
    apis: [
      'android.webkit.WebView (LOAD_NO_CACHE)',
      'WebSettingsCompat.setSafeBrowsingEnabled',
      'CookieManager & WebStorage wipe',
    ],
    androidCompatibility:
      'Uses androidx.webkit Safe Browsing on API 26–36, disables cookies and DOM storage, warns on unencrypted HTTP URLs, and wipes all data on exit.',
    summary:
      'Multi-tab incognito browser with DuckDuckGo search, back/forward navigation, HTTP safety warnings, and confirmed downloads.',
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
  theme_customization: {
    id: 'theme_customization',
    title: '8-Theme Engine & Custom App Icons',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/ThemeCustomizationScreen.kt',
    apis: [
      'Material 3 dynamic ColorScheme + CompositionLocal',
      'PackageManager.setComponentEnabledSetting (<activity-alias>)',
    ],
    androidCompatibility:
      'Switches launcher icons cleanly using 4 <activity-alias> declarations in AndroidManifest.xml across Android 8.0–16 without breaking app launch.',
    summary:
      '8 themes (Midnight Blue, Black Gold, Neon Purple, Pure White, Dark AMOLED, Cyber Green, Ocean Blue, Rose Gold), custom accents, light/dark toggle, card styles, and custom app icons.',
  },
  settings: {
    id: 'settings',
    title: 'Settings, Lock Systems & Recovery',
    kotlinPath: 'app/src/main/java/com/securelock/ai/ui/screens/SettingsScreen.kt',
    apis: [
      '4/6/Custom PIN + Pattern + Alphanumeric Password',
      'ActivityResultContracts.CreateDocument / OpenDocument',
      'SecureRandom recovery code generation',
    ],
    androidCompatibility:
      'Uses Storage Access Framework for local JSON backup/restore without requiring broad external storage permissions on Android 10–16.',
    summary:
      'Configures lock systems with credential confirmation, biometrics, lock-on-screen-off, relock delay, local backup/restore, and emergency recovery.',
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
