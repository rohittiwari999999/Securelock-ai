import { ChangeEvent, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Camera,
  Check,
  CloudOff,
  Delete,
  Download,
  Edit3,
  Fingerprint,
  FolderPlus,
  Globe,
  Grid,
  List,
  Lock,
  NotificationsOff,
  Palette,
  PauseCircle,
  PhotoIcon,
  PlayCircle,
  Plus,
  RefreshCw,
  Search,
  Settings,
  Shield,
  ShieldAlert,
  Trash2,
  UserCheck,
  VisibilityOff,
  X,
} from './SimulatorIcons';
import { SimulatorScreenId } from '../data/androidProject';

interface MobileSimulatorProps {
  activeScreen: SimulatorScreenId;
  onScreenChange: (screen: SimulatorScreenId) => void;
}

type SimLockMethod = 'PIN_4' | 'PIN_6' | 'PIN_CUSTOM' | 'PATTERN' | 'PASSWORD';
type SimThemeId =
  | 'Midnight Blue'
  | 'Black Gold'
  | 'Neon Purple'
  | 'Pure White'
  | 'Dark AMOLED'
  | 'Cyber Green'
  | 'Ocean Blue'
  | 'Rose Gold';
type SimDecoyMode =
  | 'APP_STOPPED_SIMULATED'
  | 'APPLICATION_UNAVAILABLE'
  | 'LOADING_ERROR'
  | 'NETWORK_ERROR'
  | 'BLANK_SCREEN'
  | 'CUSTOM_MESSAGE';

interface ThemeTokens {
  bg: string;
  surface: string;
  card: string;
  cardText: string;
  accent: string;
  secondary: string;
  fg: string;
}

const THEME_MAP: Record<SimThemeId, ThemeTokens> = {
  'Midnight Blue': {
    bg: '#0A1628',
    surface: '#112240',
    card: '#FFFFFF',
    cardText: '#0A1628',
    accent: '#2563EB',
    secondary: '#38BDF8',
    fg: '#FFFFFF',
  },
  'Black Gold': {
    bg: '#0B0B0D',
    surface: '#17171C',
    card: '#16161A',
    cardText: '#F8FAFC',
    accent: '#D4AF37',
    secondary: '#F59E0B',
    fg: '#FFFFFF',
  },
  'Neon Purple': {
    bg: '#120B24',
    surface: '#1D1338',
    card: '#1D1338',
    cardText: '#F8FAFC',
    accent: '#7C3AED',
    secondary: '#C084FC',
    fg: '#FFFFFF',
  },
  'Pure White': {
    bg: '#F1F5F9',
    surface: '#E2E8F0',
    card: '#FFFFFF',
    cardText: '#0A1628',
    accent: '#2563EB',
    secondary: '#0284C7',
    fg: '#0A1628',
  },
  'Dark AMOLED': {
    bg: '#000000',
    surface: '#111114',
    card: '#0E0E11',
    cardText: '#F8FAFC',
    accent: '#3B82F6',
    secondary: '#60A5FA',
    fg: '#FFFFFF',
  },
  'Cyber Green': {
    bg: '#041410',
    surface: '#0B241D',
    card: '#0B241D',
    cardText: '#ECFDF5',
    accent: '#10B981',
    secondary: '#34D399',
    fg: '#FFFFFF',
  },
  'Ocean Blue': {
    bg: '#062032',
    surface: '#0C314A',
    card: '#FFFFFF',
    cardText: '#0A1628',
    accent: '#0284C7',
    secondary: '#38BDF8',
    fg: '#FFFFFF',
  },
  'Rose Gold': {
    bg: '#211118',
    surface: '#331B26',
    card: '#FFF1F2',
    cardText: '#211118',
    accent: '#E11D48',
    secondary: '#FDA4AF',
    fg: '#FFFFFF',
  },
};

const DECOY_PRESETS: Record<
  SimDecoyMode,
  { label: string; title: string; message: string }
> = {
  APP_STOPPED_SIMULATED: {
    label: 'App Has Stopped (Simulated)',
    title: 'Application Paused (Simulated)',
    message: 'This simulated notice is part of SecureLock protection. Verify your identity to continue.',
  },
  APPLICATION_UNAVAILABLE: {
    label: 'Application Unavailable',
    title: 'Application Unavailable',
    message: 'This application is currently protected by SecureLock AI. Authenticate to continue.',
  },
  LOADING_ERROR: {
    label: 'Loading Error',
    title: 'Unable to Load Content',
    message: 'Simulated protection cover active. Authenticate to resume your session.',
  },
  NETWORK_ERROR: {
    label: 'Network Error',
    title: 'Connection Offline',
    message: 'Simulated network interruption screen. Unlock SecureLock AI to open this app.',
  },
  BLANK_SCREEN: {
    label: 'Blank Screen',
    title: '',
    message: '',
  },
  CUSTOM_MESSAGE: {
    label: 'Custom Error Message',
    title: 'Protected Space',
    message: 'Custom decoy message configured in SecureLock AI.',
  },
};

interface SimApp {
  packageName: string;
  name: string;
  locked: boolean;
  overrideMethod?: SimLockMethod;
}

interface SimVaultAlbum {
  id: string;
  name: string;
}

interface SimVaultItem {
  id: string;
  albumId: string;
  name: string;
  type: 'PHOTO' | 'VIDEO';
  sizeBytes: number;
  previewUrl?: string;
  addedAt: string;
}

interface SimEvent {
  id: string;
  title: string;
  detail: string;
  time: string;
  success: boolean;
}

interface SimIntruder {
  id: string;
  time: string;
  attempts: number;
  target: string;
  method: string;
  hasPhoto: boolean;
}

interface SimNotif {
  id: string;
  app: string;
  maskedTitle: string;
  maskedBody: string;
  time: string;
}

interface BrowserTab {
  id: string;
  title: string;
  url: string;
  history: string[];
  historyIdx: number;
}

const INITIAL_APPS: SimApp[] = [
  { packageName: 'com.whatsapp', name: 'WhatsApp', locked: true, overrideMethod: 'PATTERN' },
  { packageName: 'com.google.android.gm', name: 'Gmail', locked: true },
  { packageName: 'com.google.android.apps.photos', name: 'Google Photos', locked: true },
  { packageName: 'com.android.settings', name: 'System Settings', locked: true, overrideMethod: 'PASSWORD' },
  { packageName: 'com.chase.sig.android', name: 'Banking Vault', locked: false },
];

export function MobileSimulator({ activeScreen, onScreenChange }: MobileSimulatorProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [masterPin, setMasterPin] = useState('2580');
  const [savedPattern, setSavedPattern] = useState<number[]>([0, 1, 4, 7]);
  const [masterPassword, setMasterPassword] = useState('Vault@2026');
  const [defaultLockMethod, setDefaultLockMethod] = useState<SimLockMethod>('PIN_4');
  const [unlockMethod, setUnlockMethod] = useState<SimLockMethod>('PIN_4');
  const [enteredPin, setEnteredPin] = useState('');
  const [enteredPattern, setEnteredPattern] = useState<number[]>([]);
  const [enteredPassword, setEnteredPassword] = useState('');
  const [authStatus, setAuthStatus] = useState('Verify identity to unlock SecureLock AI');
  const [authError, setAuthError] = useState(false);
  const [consecutiveFails, setConsecutiveFails] = useState(0);
  const [showForegroundCapturePrompt, setShowForegroundCapturePrompt] = useState(false);

  const [biometricEnabled, setBiometricEnabled] = useState(true);
  const [relockTimeout, setRelockTimeout] = useState(30);
  const [lockOnScreenOff, setLockOnScreenOff] = useState(true);
  const [protectSettings, setProtectSettings] = useState(true);
  const [foregroundPromptEnabled, setForegroundPromptEnabled] = useState(true);
  const [intruderThreshold, setIntruderThreshold] = useState(3);
  const [lockNotificationsEnabled, setLockNotificationsEnabled] = useState(true);
  const [recoveryCode, setRecoveryCode] = useState('7K4M-9P2X-Q8WN');

  // 8-Theme Engine State
  const [themeId, setThemeId] = useState<SimThemeId>('Midnight Blue');
  const [customAccent, setCustomAccent] = useState<string | null>(null);
  const [forceLightCards, setForceLightCards] = useState<boolean | null>(null);
  const [cardStyle, setCardStyle] = useState<'Rounded' | 'Outlined' | 'Compact'>('Rounded');
  const [appIconStyle, setAppIconStyle] = useState('SecureLock Shield');

  const activeTheme = useMemo(() => {
    const base = THEME_MAP[themeId];
    const cardBg = forceLightCards === true ? '#FFFFFF' : forceLightCards === false ? base.surface : base.card;
    const cardFg = forceLightCards === true ? '#0A1628' : forceLightCards === false ? '#FFFFFF' : base.cardText;
    return {
      ...base,
      accent: customAccent ?? base.accent,
      card: cardBg,
      cardText: cardFg,
    };
  }, [themeId, customAccent, forceLightCards]);

  // Secret Gallery Vault State
  const [vaultUnlocked, setVaultUnlocked] = useState(false);
  const [vaultPinInput, setVaultPinInput] = useState('');
  const [vaultPinError, setVaultPinError] = useState<string | null>(null);
  const [vaultAlbums, setVaultAlbums] = useState<SimVaultAlbum[]>([
    { id: 'alb-1', name: 'Private Roll' },
    { id: 'alb-2', name: 'ID Documents' },
  ]);
  const [activeAlbumId, setActiveAlbumId] = useState('alb-1');
  const [vaultItems, setVaultItems] = useState<SimVaultItem[]>([
    {
      id: 'vm-1',
      albumId: 'alb-1',
      name: 'Passport_Scan_Encrypted.jpg',
      type: 'PHOTO',
      sizeBytes: 1_840_000,
      addedAt: 'Today · 09:12',
    },
    {
      id: 'vm-2',
      albumId: 'alb-1',
      name: 'Home_Safe_Combo.mp4',
      type: 'VIDEO',
      sizeBytes: 6_420_000,
      addedAt: 'Yesterday · 21:05',
    },
    {
      id: 'vm-3',
      albumId: 'alb-2',
      name: 'Driver_License_Front.png',
      type: 'PHOTO',
      sizeBytes: 980_000,
      addedAt: 'Nov 12 · 14:30',
    },
  ]);
  const [vaultGridView, setVaultGridView] = useState(true);
  const [vaultSearch, setVaultSearch] = useState('');
  const [newAlbumName, setNewAlbumName] = useState('');
  const [renamingAlbum, setRenamingAlbum] = useState(false);
  const [viewingVaultItem, setViewingVaultItem] = useState<SimVaultItem | null>(null);
  const [vaultNotice, setVaultNotice] = useState<string | null>(null);

  const [apps, setApps] = useState<SimApp[]>(INITIAL_APPS);
  const [appSearch, setAppSearch] = useState('');
  const [launchedTargetApp, setLaunchedTargetApp] = useState<string | null>(null);

  // Decoy Screen State
  const [coverEnabled, setCoverEnabled] = useState(false);
  const [decoyMode, setDecoyMode] = useState<SimDecoyMode>('APPLICATION_UNAVAILABLE');
  const [coverTitle, setCoverTitle] = useState(DECOY_PRESETS.APPLICATION_UNAVAILABLE.title);
  const [coverMessage, setCoverMessage] = useState(DECOY_PRESETS.APPLICATION_UNAVAILABLE.message);
  const [coverBg, setCoverBg] = useState('#0A1628');
  const [coverIcon, setCoverIcon] = useState<'shield' | 'lock' | 'hidden' | 'pause'>('shield');
  const [showingCoverPreview, setShowingCoverPreview] = useState(false);

  const [cameraGranted, setCameraGranted] = useState(true);
  const [cameraTestActive, setCameraTestActive] = useState(false);

  const [intruders, setIntruders] = useState<SimIntruder[]>([
    {
      id: 'int-1',
      time: 'Today · 09:14:22',
      attempts: 3,
      target: 'Google Photos',
      method: 'PIN',
      hasPhoto: true,
    },
  ]);

  const [maskedNotifs, setMaskedNotifs] = useState<SimNotif[]>([
    {
      id: 'n-1',
      app: 'WhatsApp',
      maskedTitle: 'Al••••',
      maskedBody: 'Sensitive content hidden by SecureLock AI',
      time: '09:42',
    },
    {
      id: 'n-2',
      app: 'Gmail',
      maskedTitle: 'Ba••••',
      maskedBody: 'Sensitive content hidden by SecureLock AI',
      time: '08:19',
    },
  ]);

  const [stats, setStats] = useState({
    total: 42,
    success: 39,
    failed: 3,
    weekly: [
      { day: 'Thu', ok: 5, fail: 0 },
      { day: 'Fri', ok: 6, fail: 1 },
      { day: 'Sat', ok: 4, fail: 0 },
      { day: 'Sun', ok: 7, fail: 0 },
      { day: 'Mon', ok: 8, fail: 1 },
      { day: 'Tue', ok: 5, fail: 0 },
      { day: 'Wed', ok: 4, fail: 1 },
    ],
  });

  const [events, setEvents] = useState<SimEvent[]>([
    {
      id: 'ev-1',
      title: 'Biometric unlock succeeded',
      detail: 'Target: SecureLock AI',
      time: '09:45:10',
      success: true,
    },
    {
      id: 'ev-2',
      title: 'PIN unlock failed',
      detail: 'Target: Google Photos',
      time: '09:14:22',
      success: false,
    },
  ]);

  const [tabs, setTabs] = useState<BrowserTab[]>([
    {
      id: 'tab-1',
      title: 'DuckDuckGo Privacy',
      url: 'https://duckduckgo.com',
      history: ['https://duckduckgo.com'],
      historyIdx: 0,
    },
  ]);
  const [activeTabIdx, setActiveTabIdx] = useState(0);
  const [urlInput, setUrlInput] = useState('https://duckduckgo.com');
  const [httpWarningUrl, setHttpWarningUrl] = useState<string | null>(null);

  const [pinEditorOpen, setPinEditorOpen] = useState(false);
  const [newPinDraft, setNewPinDraft] = useState('');
  const [confirmPinDraft, setConfirmPinDraft] = useState('');
  const [patternEditorOpen, setPatternEditorOpen] = useState(false);
  const [newPatternDraft, setNewPatternDraft] = useState<number[]>([]);
  const [passwordEditorOpen, setPasswordEditorOpen] = useState(false);
  const [newPasswordDraft, setNewPasswordDraft] = useState('');

  const lockedCount = useMemo(() => apps.filter((a) => a.locked).length, [apps]);
  const totalVaultUsed = useMemo(
    () => vaultItems.reduce((acc, it) => acc + it.sizeBytes, 0),
    [vaultItems]
  );

  const cardClasses =
    cardStyle === 'Outlined'
      ? 'border border-slate-300/40 rounded-2xl'
      : cardStyle === 'Compact'
        ? 'rounded-xl shadow-sm'
        : 'rounded-2xl shadow-md';

  const recordAttempt = (success: boolean, method: string, target = 'SecureLock AI') => {
    const nowTime = new Date().toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
    setStats((prev) => {
      const updatedWeekly = prev.weekly.map((item, idx) =>
        idx === prev.weekly.length - 1
          ? {
              ...item,
              ok: item.ok + (success ? 1 : 0),
              fail: item.fail + (success ? 0 : 1),
            }
          : item
      );
      return {
        total: prev.total + 1,
        success: prev.success + (success ? 1 : 0),
        failed: prev.failed + (success ? 0 : 1),
        weekly: updatedWeekly,
      };
    });
    setEvents((prev) => [
      {
        id: `ev-${Date.now()}`,
        title: `${method} unlock ${success ? 'succeeded' : 'failed'}`,
        detail: `Target: ${target}`,
        time: nowTime,
        success,
      },
      ...prev.slice(0, 14),
    ]);

    if (success) {
      setConsecutiveFails(0);
      setAuthError(false);
      setAuthStatus('Identity verified');
      setEnteredPin('');
      setEnteredPattern([]);
      setEnteredPassword('');
      setLaunchedTargetApp(null);
      onScreenChange('dashboard');
    } else {
      const nextFails = consecutiveFails + 1;
      setConsecutiveFails(nextFails);
      setAuthError(true);
      setAuthStatus(`${method} failed (${nextFails} failed attempt${nextFails > 1 ? 's' : ''})`);
      if (nextFails >= intruderThreshold) {
        setIntruders((prev) => [
          {
            id: `int-${Date.now()}`,
            time: `Today · ${nowTime}`,
            attempts: nextFails,
            target,
            method,
            hasPhoto: false,
          },
          ...prev,
        ]);
        if (foregroundPromptEnabled) {
          setShowForegroundCapturePrompt(true);
        }
      }
    }
  };

  const handlePinDigit = (digit: string) => {
    const maxLen = Math.max(masterPin.length, 4);
    if (enteredPin.length >= 12) return;
    const next = enteredPin + digit;
    setEnteredPin(next);
    setAuthError(false);
    if (next === masterPin) {
      recordAttempt(true, 'PIN', launchedTargetApp ?? 'SecureLock AI');
    } else if (next.length >= maxLen) {
      setEnteredPin('');
      recordAttempt(false, 'PIN', launchedTargetApp ?? 'SecureLock AI');
    }
  };

  const handlePatternTap = (node: number) => {
    if (enteredPattern.includes(node)) return;
    setEnteredPattern([...enteredPattern, node]);
  };

  const submitPattern = () => {
    if (enteredPattern.length < 4) {
      setAuthError(true);
      setAuthStatus('Connect at least 4 nodes');
      return;
    }
    const matches =
      enteredPattern.length === savedPattern.length &&
      enteredPattern.every((v, i) => v === savedPattern[i]);
    setEnteredPattern([]);
    recordAttempt(matches, 'Pattern', launchedTargetApp ?? 'SecureLock AI');
  };

  const handleVaultFilesSelected = (e: ChangeEvent<HTMLInputElement>) => {
    const fileList = e.target.files;
    if (!fileList || fileList.length === 0) {
      setVaultNotice('Import cancelled.');
      return;
    }
    const added: SimVaultItem[] = Array.from(fileList).map((f, idx) => ({
      id: `vm-${Date.now()}-${idx}`,
      albumId: activeAlbumId,
      name: f.name,
      type: f.type.startsWith('video/') ? 'VIDEO' : 'PHOTO',
      sizeBytes: f.size || 450_000,
      previewUrl: f.type.startsWith('image/') ? URL.createObjectURL(f) : undefined,
      addedAt: 'Just now',
    }));
    setVaultItems((prev) => [...added, ...prev]);
    setVaultNotice(`Encrypted ${added.length} item(s) into Keystore vault.`);
    e.target.value = '';
  };

  const renderCoverScreen = (targetLabel: string, isPreview: boolean) => {
    if (decoyMode === 'BLANK_SCREEN') {
      return (
        <div
          className="h-full flex flex-col justify-end items-center p-6"
          style={{ backgroundColor: coverBg }}
        >
          <button
            type="button"
            onClick={() => {
              setShowingCoverPreview(false);
              onScreenChange('unlock');
            }}
            className="text-[11px] text-white/40 hover:text-white/80"
          >
            Authenticate to Exit Decoy
          </button>
        </div>
      );
    }

    const CoverIcon =
      decoyMode === 'NETWORK_ERROR'
        ? CloudOff
        : decoyMode === 'APP_STOPPED_SIMULATED'
          ? ShieldAlert
          : coverIcon === 'lock'
            ? Lock
            : coverIcon === 'hidden'
              ? VisibilityOff
              : coverIcon === 'pause'
                ? PauseCircle
                : Shield;

    return (
      <div
        className="h-full flex items-center justify-center p-5"
        style={{ backgroundColor: coverBg }}
      >
        <div className="bg-white rounded-3xl p-6 w-full text-center shadow-xl">
          <div
            className="w-14 h-14 rounded-full mx-auto flex items-center justify-center mb-4"
            style={{ backgroundColor: `${activeTheme.accent}1A`, color: activeTheme.accent }}
          >
            <CoverIcon className="w-7 h-7" />
          </div>
          <h4 className="text-lg font-bold text-[#0A1628] mb-2">
            {coverTitle || DECOY_PRESETS[decoyMode].title}
          </h4>
          <p className="text-xs text-slate-600 leading-relaxed mb-2">
            {coverMessage || DECOY_PRESETS[decoyMode].message} ({targetLabel})
          </p>
          <div className="text-[10px] font-semibold mb-4" style={{ color: activeTheme.accent }}>
            Simulated SecureLock Decoy
          </div>
          <button
            type="button"
            onClick={() => {
              setShowingCoverPreview(false);
              onScreenChange('unlock');
            }}
            className="w-full py-2.5 px-4 rounded-xl text-white text-xs font-semibold transition-opacity hover:opacity-90 mb-2"
            style={{ backgroundColor: activeTheme.accent }}
          >
            Authenticate to Unlock
          </button>
          <button
            type="button"
            onClick={() => {
              setShowingCoverPreview(false);
              setLaunchedTargetApp(null);
              if (!isPreview) onScreenChange('dashboard');
            }}
            className="w-full py-2 px-4 rounded-xl border border-slate-200 text-slate-700 text-xs font-medium hover:bg-slate-50"
          >
            Close
          </button>
        </div>
      </div>
    );
  };

  const dashboardModules: Array<{
    id: SimulatorScreenId;
    title: string;
    subtitle: string;
    metric: string;
    icon: typeof Lock;
  }> = [
    {
      id: 'app_lock',
      title: 'App Lock',
      subtitle: 'Per-app auth & relock',
      metric: `${lockedCount} Locked`,
      icon: Lock,
    },
    {
      id: 'vault',
      title: 'Secret Gallery Vault',
      subtitle: 'Encrypted photos & videos',
      metric: `${vaultItems.length} Items`,
      icon: PhotoIcon,
    },
    {
      id: 'intruder_selfie',
      title: 'Intruder Detection',
      subtitle: `${intruderThreshold}-try alert & camera`,
      metric: `${intruders.length} Events`,
      icon: Camera,
    },
    {
      id: 'lock_notifications',
      title: 'Lock Notifications',
      subtitle: 'Local privacy masking',
      metric: lockNotificationsEnabled ? `${maskedNotifs.length} Masked` : 'Paused',
      icon: NotificationsOff,
    },
    {
      id: 'fake_crash',
      title: 'Fake Crash Screen',
      subtitle: '6 simulated decoy modes',
      metric: coverEnabled ? 'Active' : 'Off',
      icon: ShieldAlert,
    },
    {
      id: 'camera_monitor',
      title: 'Camera Privacy',
      subtitle: 'Sensors & live self-test',
      metric: cameraGranted ? 'Ready' : 'Denied',
      icon: UserCheck,
    },
    {
      id: 'private_browser',
      title: 'Private Browser',
      subtitle: 'Zero-history WebView',
      metric: `${tabs.length} Tab`,
      icon: Globe,
    },
    {
      id: 'statistics',
      title: 'Statistics',
      subtitle: 'Weekly activity & logs',
      metric: `${stats.total} Tries`,
      icon: BarChart3,
    },
    {
      id: 'theme_customization',
      title: 'Theme Customization',
      subtitle: '8 themes & app icons',
      metric: themeId.split(' ')[0],
      icon: Palette,
    },
  ];

  const filteredApps = apps.filter(
    (a) =>
      a.name.toLowerCase().includes(appSearch.toLowerCase()) ||
      a.packageName.toLowerCase().includes(appSearch.toLowerCase())
  );

  const filteredVaultItems = vaultItems.filter(
    (it) =>
      it.albumId === activeAlbumId &&
      (vaultSearch.trim() === '' ||
        it.name.toLowerCase().includes(vaultSearch.toLowerCase()) ||
        it.type.toLowerCase().includes(vaultSearch.toLowerCase()))
  );

  const currentTab = tabs[activeTabIdx] ?? tabs[0];

  const navigateBrowser = (raw: string) => {
    const trimmed = raw.trim();
    const resolved =
      trimmed.startsWith('https://') || trimmed.startsWith('http://')
        ? trimmed
        : trimmed.includes('.') && !trimmed.includes(' ')
          ? `https://${trimmed}`
          : `https://duckduckgo.com/?q=${encodeURIComponent(trimmed)}`;

    if (resolved.startsWith('http://')) {
      setHttpWarningUrl(resolved);
      return;
    }
    setTabs((prev) =>
      prev.map((tab, i) =>
        i === activeTabIdx
          ? {
              ...tab,
              title: resolved.replace(/^https?:\/\//, '').split('/')[0] || 'Private Tab',
              url: resolved,
              history: [...tab.history.slice(0, tab.historyIdx + 1), resolved],
              historyIdx: tab.historyIdx + 1,
            }
          : tab
      )
    );
    setUrlInput(resolved);
  };

  return (
    <div className="w-full max-w-[390px] mx-auto bg-[#070F1D] rounded-[42px] p-3 shadow-2xl border border-slate-800">
      <div
        className="rounded-[34px] overflow-hidden h-[740px] flex flex-col relative transition-colors"
        style={{ backgroundColor: activeTheme.bg, color: activeTheme.fg }}
      >
        {/* Android Status Bar */}
        <div className="px-6 pt-3 pb-2 flex items-center justify-between text-[11px] font-medium opacity-80 shrink-0">
          <span className="tabular-nums">11:08</span>
          <div className="flex items-center gap-2">
            {cameraTestActive && (
              <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-semibold">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                CAM
              </span>
            )}
            <span>5G</span>
            <span className="tabular-nums">96%</span>
          </div>
        </div>

        {/* Screen Body */}
        <div className="flex-1 overflow-y-auto">
          {showingCoverPreview ? (
            renderCoverScreen(launchedTargetApp ?? 'WhatsApp', true)
          ) : activeScreen === 'unlock' ? (
            <div className="p-5 flex flex-col justify-between min-h-full">
              <div className="text-center">
                <button
                  type="button"
                  onClick={() => {
                    if (biometricEnabled) {
                      recordAttempt(true, 'Biometric', launchedTargetApp ?? 'SecureLock AI');
                    }
                  }}
                  title="Tap to test BiometricPrompt success"
                  className="w-20 h-20 rounded-full mx-auto flex items-center justify-center transition-transform hover:scale-105"
                  style={{
                    backgroundColor: activeTheme.surface,
                    border: `1px solid ${activeTheme.secondary}55`,
                  }}
                >
                  <Fingerprint className="w-11 h-11" style={{ color: activeTheme.secondary }} />
                </button>
                <h3 className="mt-3 text-lg font-bold">
                  {launchedTargetApp ? `Unlock ${launchedTargetApp}` : 'SecureLock AI'}
                </h3>
                <p
                  className={`mt-1 text-xs ${
                    authError ? 'text-rose-400' : 'opacity-75'
                  }`}
                >
                  {authStatus}
                </p>

                <div
                  className="mt-3 inline-flex p-1 rounded-xl gap-1"
                  style={{ backgroundColor: activeTheme.surface }}
                >
                  {(
                    [
                      ['PIN_4', `PIN (${masterPin})`],
                      ['PATTERN', 'Pattern'],
                      ['PASSWORD', 'Password'],
                    ] as Array<[SimLockMethod, string]>
                  ).map(([m, label]) => (
                    <button
                      key={m}
                      type="button"
                      onClick={() => {
                        setUnlockMethod(m);
                        setAuthError(false);
                      }}
                      className="px-2.5 py-1 rounded-lg text-[11px] font-medium transition-colors"
                      style={
                        unlockMethod === m
                          ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                          : { color: activeTheme.fg }
                      }
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>

              {unlockMethod === 'PATTERN' ? (
                <div className="my-4 text-center">
                  <p className="text-[11px] opacity-70 mb-2">
                    Tap nodes in order (default: 1 → 2 → 5 → 8)
                  </p>
                  <div
                    className="grid grid-cols-3 gap-4 max-w-[180px] mx-auto p-4 rounded-2xl"
                    style={{ backgroundColor: activeTheme.surface }}
                  >
                    {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((node) => {
                      const activeIdx = enteredPattern.indexOf(node);
                      return (
                        <button
                          key={node}
                          type="button"
                          onClick={() => handlePatternTap(node)}
                          className="w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold transition-colors"
                          style={
                            activeIdx >= 0
                              ? { backgroundColor: activeTheme.secondary, color: '#0A1628' }
                              : { backgroundColor: 'rgba(255,255,255,0.14)', color: activeTheme.fg }
                          }
                        >
                          {activeIdx >= 0 ? activeIdx + 1 : node + 1}
                        </button>
                      );
                    })}
                  </div>
                  <div className="flex justify-center gap-2 mt-3">
                    <button
                      type="button"
                      onClick={() => setEnteredPattern([])}
                      className="px-3 py-1.5 rounded-lg bg-white/10 text-xs"
                    >
                      Clear
                    </button>
                    <button
                      type="button"
                      onClick={submitPattern}
                      className="px-4 py-1.5 rounded-lg text-white text-xs font-semibold"
                      style={{ backgroundColor: activeTheme.accent }}
                    >
                      Verify Pattern
                    </button>
                  </div>
                </div>
              ) : unlockMethod === 'PASSWORD' ? (
                <div className="my-6 space-y-3">
                  <input
                    type="password"
                    value={enteredPassword}
                    onChange={(e) => setEnteredPassword(e.target.value)}
                    placeholder={`Enter password (default: ${masterPassword})`}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-white text-[#0A1628] text-xs"
                  />
                  <button
                    type="button"
                    onClick={() =>
                      recordAttempt(
                        enteredPassword === masterPassword,
                        'Password',
                        launchedTargetApp ?? 'SecureLock AI'
                      )
                    }
                    className="w-full py-2.5 rounded-xl text-white text-xs font-semibold"
                    style={{ backgroundColor: activeTheme.accent }}
                  >
                    Unlock with Password
                  </button>
                </div>
              ) : (
                <div className="my-4">
                  <div className="flex justify-center gap-2.5 mb-5">
                    {Array.from({ length: Math.max(masterPin.length, 4) }).map((_, idx) => (
                      <span
                        key={idx}
                        className="w-3 h-3 rounded-full transition-colors"
                        style={{
                          backgroundColor:
                            idx < enteredPin.length ? activeTheme.secondary : 'rgba(148,163,184,0.35)',
                        }}
                      />
                    ))}
                  </div>
                  <div className="grid grid-cols-3 gap-3 max-w-[232px] mx-auto">
                    {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => (
                      <button
                        key={d}
                        type="button"
                        onClick={() => handlePinDigit(d)}
                        className="h-14 rounded-full text-lg font-semibold transition-opacity hover:opacity-80"
                        style={{ backgroundColor: activeTheme.surface }}
                      >
                        {d}
                      </button>
                    ))}
                    <button
                      type="button"
                      onClick={() =>
                        biometricEnabled &&
                        recordAttempt(true, 'Biometric', launchedTargetApp ?? 'SecureLock AI')
                      }
                      disabled={!biometricEnabled}
                      className="h-14 rounded-full disabled:opacity-40 flex items-center justify-center"
                      style={{ backgroundColor: `${activeTheme.accent}33` }}
                      aria-label="BiometricPrompt unlock"
                    >
                      <Fingerprint className="w-6 h-6" style={{ color: activeTheme.secondary }} />
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePinDigit('0')}
                      className="h-14 rounded-full text-lg font-semibold transition-opacity hover:opacity-80"
                      style={{ backgroundColor: activeTheme.surface }}
                    >
                      0
                    </button>
                    <button
                      type="button"
                      onClick={() => setEnteredPin((p) => p.slice(0, -1))}
                      className="h-14 rounded-full flex items-center justify-center"
                      style={{ backgroundColor: activeTheme.surface }}
                      aria-label="Backspace"
                    >
                      <Delete className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              )}

              <div className="flex flex-col gap-2">
                <button
                  type="button"
                  onClick={() =>
                    recordAttempt(false, unlockMethod, launchedTargetApp ?? 'SecureLock AI')
                  }
                  className="w-full py-2 rounded-xl bg-rose-500/20 text-rose-300 text-xs font-medium hover:bg-rose-500/30"
                >
                  Simulate Failed Attempt (Threshold: {intruderThreshold})
                </button>
                {showForegroundCapturePrompt && (
                  <div className="bg-white text-[#0A1628] rounded-2xl p-3 text-left shadow-lg">
                    <div className="text-xs font-bold mb-1">Foreground Camera Prompt</div>
                    <p className="text-[11px] text-slate-600 mb-2">
                      Threshold of {intruderThreshold} failed attempts reached. Capture an encrypted foreground snapshot?
                    </p>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setIntruders((prev) =>
                            prev.map((item, idx) => (idx === 0 ? { ...item, hasPhoto: true } : item))
                          );
                          setShowForegroundCapturePrompt(false);
                        }}
                        className="px-3 py-1.5 rounded-lg text-white text-[11px] font-semibold"
                        style={{ backgroundColor: activeTheme.accent }}
                      >
                        Capture &amp; Encrypt
                      </button>
                      <button
                        type="button"
                        onClick={() => setShowForegroundCapturePrompt(false)}
                        className="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-700 text-[11px]"
                      >
                        Skip
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : activeScreen === 'dashboard' ? (
            <div className="p-4 space-y-3">
              <div className="flex items-center justify-between px-1">
                <div>
                  <h3 className="text-base font-bold">SecureLock AI</h3>
                  <p className="text-[11px]" style={{ color: activeTheme.secondary }}>
                    {themeId} · {appIconStyle}
                  </p>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => onScreenChange('permissions')}
                    className="p-2 rounded-xl"
                    style={{ backgroundColor: activeTheme.surface, color: activeTheme.secondary }}
                    aria-label="Permissions Manager"
                  >
                    <Shield className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onScreenChange('settings')}
                    className="p-2 rounded-xl"
                    style={{ backgroundColor: activeTheme.surface, color: activeTheme.secondary }}
                    aria-label="Settings"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div
                className="rounded-2xl p-3.5 flex items-center justify-between"
                style={{ backgroundColor: activeTheme.surface }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center"
                    style={{ backgroundColor: `${activeTheme.accent}33`, color: activeTheme.secondary }}
                  >
                    <Shield className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold">Keystore AES-256-GCM</div>
                    <div className="text-[11px] opacity-80 tabular-nums">
                      {lockedCount} apps · {vaultItems.length} vault files · {stats.failed} fails
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onScreenChange('unlock')}
                  className="text-xs font-semibold"
                  style={{ color: activeTheme.secondary }}
                >
                  Lock
                </button>
              </div>

              <div className="grid grid-cols-2 gap-2.5">
                {dashboardModules.map((mod) => {
                  const Icon = mod.icon;
                  return (
                    <button
                      key={mod.id}
                      type="button"
                      onClick={() => onScreenChange(mod.id)}
                      className={`${cardClasses} p-3 text-left transition-transform active:scale-[0.99] flex flex-col justify-between min-h-[112px]`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="flex items-center justify-between w-full mb-2">
                        <span
                          className="w-8 h-8 rounded-xl flex items-center justify-center"
                          style={{
                            backgroundColor: `${activeTheme.accent}1F`,
                            color: activeTheme.accent,
                          }}
                        >
                          <Icon className="w-4 h-4" />
                        </span>
                        <span
                          className="text-[10px] font-semibold tabular-nums"
                          style={{ color: activeTheme.accent }}
                        >
                          {mod.metric}
                        </span>
                      </div>
                      <div>
                        <div className="text-xs font-bold leading-snug">{mod.title}</div>
                        <div className="text-[11px] opacity-70 mt-0.5 leading-tight">{mod.subtitle}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="flex flex-col h-full">
              {/* Inner Top App Bar */}
              <div className="px-4 py-3 border-b border-white/10 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() => onScreenChange('dashboard')}
                    className="p-1.5 rounded-lg hover:bg-white/10"
                    aria-label="Back to dashboard"
                  >
                    <ArrowLeft className="w-4 h-4" />
                  </button>
                  <div className="text-sm font-semibold capitalize">
                    {activeScreen.replace('_', ' ')}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onScreenChange('unlock')}
                  className="text-[11px] font-medium"
                  style={{ color: activeTheme.secondary }}
                >
                  Lock Now
                </button>
              </div>

              <div className="p-4 space-y-3 flex-1 overflow-y-auto">
                {/* SECRET GALLERY VAULT */}
                {activeScreen === 'vault' && (
                  <>
                    <input
                      ref={fileInputRef}
                      type="file"
                      multiple
                      accept="image/*,video/*"
                      onChange={handleVaultFilesSelected}
                      className="hidden"
                    />
                    {!vaultUnlocked ? (
                      <div
                        className={`${cardClasses} p-5 space-y-3 text-center`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <Lock className="w-9 h-9 mx-auto" style={{ color: activeTheme.accent }} />
                        <div className="text-sm font-bold">Vault Locked</div>
                        <p className="text-xs opacity-75">
                          Enter PIN ({masterPin}) or use fingerprint to decrypt private albums.
                        </p>
                        <input
                          type="password"
                          value={vaultPinInput}
                          onChange={(e) => setVaultPinInput(e.target.value)}
                          placeholder="Enter Master PIN"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs text-[#0A1628] bg-white"
                        />
                        {vaultPinError && (
                          <div className="text-xs text-rose-500 font-medium">{vaultPinError}</div>
                        )}
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => {
                              if (vaultPinInput === masterPin) {
                                setVaultUnlocked(true);
                                setVaultPinInput('');
                                setVaultPinError(null);
                              } else {
                                setVaultPinError('Incorrect PIN');
                              }
                            }}
                            className="flex-1 py-2 rounded-xl text-white text-xs font-semibold"
                            style={{ backgroundColor: activeTheme.accent }}
                          >
                            Unlock PIN
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setVaultUnlocked(true);
                              setVaultPinError(null);
                            }}
                            className="px-3 py-2 rounded-xl bg-slate-200 text-[#0A1628] text-xs font-semibold inline-flex items-center gap-1"
                          >
                            <Fingerprint className="w-3.5 h-3.5" />
                            <span>Bio</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div
                          className={`${cardClasses} p-3.5 space-y-2.5`}
                          style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                        >
                          <div className="flex items-center justify-between text-xs">
                            <span className="font-bold">AES-256-GCM Storage</span>
                            <span className="text-emerald-600 font-semibold tabular-nums">
                              {(totalVaultUsed / (1024 * 1024)).toFixed(1)} MB used · 48.2 GB free
                            </span>
                          </div>
                          <div className="flex flex-wrap gap-1.5">
                            <button
                              type="button"
                              onClick={() => fileInputRef.current?.click()}
                              className="px-3 py-1.5 rounded-xl text-white text-[11px] font-semibold inline-flex items-center gap-1"
                              style={{ backgroundColor: activeTheme.accent }}
                            >
                              <Plus className="w-3.5 h-3.5" />
                              <span>Import Photos/Videos</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => setVaultGridView(!vaultGridView)}
                              className="p-1.5 rounded-xl bg-slate-200 text-[#0A1628]"
                              title="Toggle Grid/List"
                            >
                              {vaultGridView ? <List className="w-4 h-4" /> : <Grid className="w-4 h-4" />}
                            </button>
                            <button
                              type="button"
                              onClick={() =>
                                setVaultNotice(
                                  `Encrypted .slvbak backup prepared (${vaultItems.length} files).`
                                )
                              }
                              className="px-2.5 py-1.5 rounded-xl bg-slate-200 text-[#0A1628] text-[11px] font-medium"
                            >
                              Backup
                            </button>
                          </div>
                          {vaultNotice && (
                            <div className="text-[11px]" style={{ color: activeTheme.accent }}>
                              {vaultNotice}
                            </div>
                          )}
                        </div>

                        {/* Album Bar + Create / Rename / Delete */}
                        <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
                          {vaultAlbums.map((alb) => (
                            <button
                              key={alb.id}
                              type="button"
                              onClick={() => setActiveAlbumId(alb.id)}
                              className="px-2.5 py-1 rounded-lg text-[11px] font-medium shrink-0"
                              style={
                                alb.id === activeAlbumId
                                  ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                  : { backgroundColor: activeTheme.surface, color: activeTheme.fg }
                              }
                            >
                              {alb.name}
                            </button>
                          ))}
                          <button
                            type="button"
                            onClick={() => {
                              const next = {
                                id: `alb-${Date.now()}`,
                                name: `Album ${vaultAlbums.length + 1}`,
                              };
                              setVaultAlbums((prev) => [...prev, next]);
                              setActiveAlbumId(next.id);
                            }}
                            className="p-1.5 rounded-lg"
                            style={{ backgroundColor: activeTheme.surface, color: activeTheme.secondary }}
                            title="New Album"
                          >
                            <FolderPlus className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              const current = vaultAlbums.find((a) => a.id === activeAlbumId);
                              setNewAlbumName(current?.name ?? '');
                              setRenamingAlbum(!renamingAlbum);
                            }}
                            className="p-1.5 rounded-lg"
                            style={{ backgroundColor: activeTheme.surface, color: activeTheme.secondary }}
                            title="Rename Album"
                          >
                            <Edit3 className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        {renamingAlbum && (
                          <div className="flex gap-1.5">
                            <input
                              type="text"
                              value={newAlbumName}
                              onChange={(e) => setNewAlbumName(e.target.value)}
                              className="flex-1 px-2.5 py-1.5 rounded-lg bg-white text-[#0A1628] text-xs"
                            />
                            <button
                              type="button"
                              onClick={() => {
                                if (newAlbumName.trim()) {
                                  setVaultAlbums((prev) =>
                                    prev.map((a) =>
                                      a.id === activeAlbumId ? { ...a, name: newAlbumName.trim() } : a
                                    )
                                  );
                                }
                                setRenamingAlbum(false);
                              }}
                              className="px-2.5 py-1.5 rounded-lg text-white text-xs"
                              style={{ backgroundColor: activeTheme.accent }}
                            >
                              Save
                            </button>
                            {vaultAlbums.length > 1 && (
                              <button
                                type="button"
                                onClick={() => {
                                  const fallback = vaultAlbums.find((a) => a.id !== activeAlbumId)?.id ?? 'alb-1';
                                  setVaultItems((prev) =>
                                    prev.map((it) =>
                                      it.albumId === activeAlbumId ? { ...it, albumId: fallback } : it
                                    )
                                  );
                                  setVaultAlbums((prev) => prev.filter((a) => a.id !== activeAlbumId));
                                  setActiveAlbumId(fallback);
                                  setRenamingAlbum(false);
                                }}
                                className="px-2.5 py-1.5 rounded-lg bg-rose-600 text-white text-xs"
                              >
                                Delete
                              </button>
                            )}
                          </div>
                        )}

                        <div className="relative">
                          <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                          <input
                            type="text"
                            value={vaultSearch}
                            onChange={(e) => setVaultSearch(e.target.value)}
                            placeholder="Search private media..."
                            className="w-full pl-8 pr-3 py-2 rounded-xl bg-white text-[#0A1628] text-xs"
                          />
                        </div>

                        {filteredVaultItems.length === 0 ? (
                          <div
                            className={`${cardClasses} p-4 text-xs opacity-75`}
                            style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                          >
                            No items in this album. Tap Import Photos/Videos to encrypt local files.
                          </div>
                        ) : vaultGridView ? (
                          <div className="grid grid-cols-2 gap-2">
                            {filteredVaultItems.map((it) => (
                              <div
                                key={it.id}
                                className={`${cardClasses} p-2.5 space-y-1.5`}
                                style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                              >
                                <button
                                  type="button"
                                  onClick={() => setViewingVaultItem(it)}
                                  className="w-full h-20 rounded-xl bg-slate-900/10 flex items-center justify-center overflow-hidden"
                                >
                                  {it.previewUrl ? (
                                    <img
                                      src={it.previewUrl}
                                      alt={it.name}
                                      className="w-full h-full object-cover"
                                    />
                                  ) : it.type === 'VIDEO' ? (
                                    <PlayCircle className="w-7 h-7" style={{ color: activeTheme.accent }} />
                                  ) : (
                                    <PhotoIcon className="w-7 h-7" style={{ color: activeTheme.accent }} />
                                  )}
                                </button>
                                <div className="flex items-center justify-between gap-1">
                                  <span className="text-[11px] font-semibold truncate">{it.name}</span>
                                  <button
                                    type="button"
                                    onClick={() =>
                                      setVaultItems((prev) => prev.filter((x) => x.id !== it.id))
                                    }
                                    className="text-rose-500 p-1"
                                    aria-label="Delete media"
                                  >
                                    <Trash2 className="w-3.5 h-3.5" />
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="space-y-2">
                            {filteredVaultItems.map((it) => (
                              <div
                                key={it.id}
                                className={`${cardClasses} p-3 flex items-center justify-between`}
                                style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                              >
                                <button
                                  type="button"
                                  onClick={() => setViewingVaultItem(it)}
                                  className="text-left min-w-0 flex-1"
                                >
                                  <div className="text-xs font-bold truncate">{it.name}</div>
                                  <div className="text-[10px] opacity-70">
                                    {it.type} · {(it.sizeBytes / 1024).toFixed(0)} KB · {it.addedAt}
                                  </div>
                                </button>
                                <button
                                  type="button"
                                  onClick={() =>
                                    setVaultItems((prev) => prev.filter((x) => x.id !== it.id))
                                  }
                                  className="text-rose-500 p-1.5"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            ))}
                          </div>
                        )}

                        {viewingVaultItem && (
                          <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2 shadow-xl">
                            <div className="flex justify-between items-center">
                              <span className="text-xs font-bold truncate">{viewingVaultItem.name}</span>
                              <button
                                type="button"
                                onClick={() => setViewingVaultItem(null)}
                                className="p-1"
                              >
                                <X className="w-4 h-4" />
                              </button>
                            </div>
                            <div className="h-32 rounded-xl bg-slate-900 flex items-center justify-center overflow-hidden text-white">
                              {viewingVaultItem.previewUrl ? (
                                <img
                                  src={viewingVaultItem.previewUrl}
                                  alt={viewingVaultItem.name}
                                  className="w-full h-full object-contain"
                                />
                              ) : (
                                <div className="text-center text-xs">
                                  {viewingVaultItem.type === 'VIDEO'
                                    ? 'Encrypted Video Stream (Wiped on Close)'
                                    : 'Decrypted In-Memory Preview'}
                                </div>
                              )}
                            </div>
                            <button
                              type="button"
                              onClick={() => {
                                setVaultNotice(`Authenticated export complete: ${viewingVaultItem.name}`);
                                setViewingVaultItem(null);
                              }}
                              className="w-full py-1.5 rounded-lg text-white text-xs font-semibold inline-flex items-center justify-center gap-1"
                              style={{ backgroundColor: activeTheme.accent }}
                            >
                              <Download className="w-3.5 h-3.5" />
                              <span>Authenticated Export</span>
                            </button>
                          </div>
                        )}
                      </>
                    )}
                  </>
                )}

                {/* APP LOCK */}
                {activeScreen === 'app_lock' && (
                  <>
                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold">Lock on Screen Off</div>
                          <div className="text-[11px] opacity-70">Delay: {relockTimeout}s</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setLockOnScreenOff(!lockOnScreenOff)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                          style={
                            lockOnScreenOff
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                          }
                        >
                          {lockOnScreenOff ? 'On' : 'Off'}
                        </button>
                      </div>
                      <div className="flex gap-1.5">
                        {[0, 15, 30, 120, 300].map((sec) => (
                          <button
                            key={sec}
                            type="button"
                            onClick={() => setRelockTimeout(sec)}
                            className="px-2 py-1 rounded-lg text-[10px] font-medium"
                            style={
                              relockTimeout === sec
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {sec === 0 ? 'Now' : `${sec}s`}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="relative">
                      <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={appSearch}
                        onChange={(e) => setAppSearch(e.target.value)}
                        placeholder="Search installed apps..."
                        className="w-full pl-8 pr-3 py-2 rounded-xl bg-white text-[#0A1628] text-xs"
                      />
                    </div>

                    {filteredApps.map((app) => (
                      <div
                        key={app.packageName}
                        className={`${cardClasses} p-3 space-y-2`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="flex items-center justify-between gap-2">
                          <div className="min-w-0">
                            <div className="text-xs font-bold truncate">{app.name}</div>
                            <div className="text-[10px] opacity-60 font-mono truncate">
                              {app.packageName}
                            </div>
                          </div>
                          <div className="flex items-center gap-1.5 shrink-0">
                            {app.locked && (
                              <button
                                type="button"
                                onClick={() => {
                                  setLaunchedTargetApp(app.name);
                                  setUnlockMethod(app.overrideMethod ?? defaultLockMethod);
                                  if (coverEnabled) setShowingCoverPreview(true);
                                  else onScreenChange('unlock');
                                }}
                                className="px-2 py-1 rounded-lg bg-slate-200 text-[#0A1628] text-[10px] font-medium"
                              >
                                Test
                              </button>
                            )}
                            <button
                              type="button"
                              onClick={() =>
                                setApps((prev) =>
                                  prev.map((a) =>
                                    a.packageName === app.packageName ? { ...a, locked: !a.locked } : a
                                  )
                                )
                              }
                              className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                              style={
                                app.locked
                                  ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                  : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                              }
                            >
                              {app.locked ? 'Locked' : 'Open'}
                            </button>
                          </div>
                        </div>

                        {app.locked && (
                          <div className="flex items-center gap-1 flex-wrap">
                            {(
                              [
                                [undefined, 'Global'],
                                ['PIN_4', 'PIN'],
                                ['PATTERN', 'Pattern'],
                                ['PASSWORD', 'Password'],
                              ] as Array<[SimLockMethod | undefined, string]>
                            ).map(([m, label]) => (
                              <button
                                key={label}
                                type="button"
                                onClick={() =>
                                  setApps((prev) =>
                                    prev.map((a) =>
                                      a.packageName === app.packageName ? { ...a, overrideMethod: m } : a
                                    )
                                  )
                                }
                                className="px-2 py-0.5 rounded text-[10px]"
                                style={
                                  app.overrideMethod === m
                                    ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                    : { backgroundColor: 'rgba(148,163,184,0.2)' }
                                }
                              >
                                {label}
                              </button>
                            ))}
                          </div>
                        )}
                      </div>
                    ))}
                  </>
                )}

                {/* INTRUDER DETECTION */}
                {activeScreen === 'intruder_selfie' && (
                  <>
                    <div
                      className={`${cardClasses} p-3.5 space-y-2.5`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-bold">Foreground Intruder Capture</div>
                        <button
                          type="button"
                          onClick={() => setForegroundPromptEnabled(!foregroundPromptEnabled)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                          style={
                            foregroundPromptEnabled
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                          }
                        >
                          {foregroundPromptEnabled ? 'On' : 'Off'}
                        </button>
                      </div>
                      <div className="text-[11px] opacity-75">Configurable Attempt Threshold</div>
                      <div className="flex gap-1.5">
                        {[1, 2, 3, 5].map((cnt) => (
                          <button
                            key={cnt}
                            type="button"
                            onClick={() => setIntruderThreshold(cnt)}
                            className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                            style={
                              intruderThreshold === cnt
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {cnt} {cnt === 1 ? 'try' : 'tries'}
                          </button>
                        ))}
                      </div>
                      <button
                        type="button"
                        onClick={() =>
                          setIntruders((prev) => [
                            {
                              id: `int-${Date.now()}`,
                              time: `Today · ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`,
                              attempts: 1,
                              target: 'Foreground Self-Test',
                              method: 'CameraX',
                              hasPhoto: true,
                            },
                            ...prev,
                          ])
                        }
                        className="w-full py-2 rounded-xl text-white text-xs font-semibold"
                        style={{ backgroundColor: activeTheme.accent }}
                      >
                        Capture Encrypted Snapshot
                      </button>
                    </div>

                    {intruders.map((item) => (
                      <div
                        key={item.id}
                        className={`${cardClasses} p-3.5 flex items-center justify-between`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div>
                          <div className="text-xs font-bold text-rose-500">
                            {item.attempts} Failed Attempt(s) · {item.method}
                          </div>
                          <div className="text-[11px] opacity-70">{item.time}</div>
                          <div className="text-[11px] opacity-80 mt-0.5">
                            {item.target} · {item.hasPhoto ? 'Encrypted Photo Saved' : 'Log Only'}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setIntruders((prev) => prev.filter((x) => x.id !== item.id))}
                          className="p-2 text-rose-500 rounded-lg"
                          aria-label="Delete intruder record"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </>
                )}

                {/* LOCK NOTIFICATIONS */}
                {activeScreen === 'lock_notifications' && (
                  <>
                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-bold">Notification Privacy Vault</div>
                        <button
                          type="button"
                          onClick={() => setLockNotificationsEnabled(!lockNotificationsEnabled)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                          style={
                            lockNotificationsEnabled
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                          }
                        >
                          {lockNotificationsEnabled ? 'Active' : 'Paused'}
                        </button>
                      </div>
                      <button
                        type="button"
                        onClick={() =>
                          setMaskedNotifs((prev) => [
                            {
                              id: `n-${Date.now()}`,
                              app: 'WhatsApp',
                              maskedTitle: 'Jo••••',
                              maskedBody: 'Sensitive content hidden by SecureLock AI',
                              time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                            },
                            ...prev,
                          ])
                        }
                        className="w-full py-2 rounded-xl text-white text-xs font-semibold"
                        style={{ backgroundColor: activeTheme.accent }}
                      >
                        Simulate Protected Notification
                      </button>
                    </div>
                    {maskedNotifs.map((n) => (
                      <div
                        key={n.id}
                        className={`${cardClasses} p-3.5`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="flex justify-between text-xs font-bold">
                          <span>{n.app}</span>
                          <span className="opacity-60 font-normal">{n.time}</span>
                        </div>
                        <div className="text-xs font-semibold mt-1" style={{ color: activeTheme.accent }}>
                          {n.maskedTitle}
                        </div>
                        <div className="text-[11px] opacity-75">{n.maskedBody}</div>
                      </div>
                    ))}
                  </>
                )}

                {/* FAKE CRASH / DECOY SCREEN */}
                {activeScreen === 'fake_crash' && (
                  <div
                    className={`${cardClasses} p-4 space-y-3`}
                    style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-xs font-bold">Simulated Decoy Screen</div>
                        <div className="text-[11px] opacity-70">Requires auth to exit</div>
                      </div>
                      <button
                        type="button"
                        onClick={() => setCoverEnabled(!coverEnabled)}
                        className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                        style={
                          coverEnabled
                            ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                            : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                        }
                      >
                        {coverEnabled ? 'Enabled' : 'Off'}
                      </button>
                    </div>

                    <div className="text-[11px] font-semibold opacity-80">Select Simulated Template</div>
                    <div className="flex flex-wrap gap-1.5">
                      {(Object.keys(DECOY_PRESETS) as SimDecoyMode[]).map((mode) => (
                        <button
                          key={mode}
                          type="button"
                          onClick={() => {
                            setDecoyMode(mode);
                            setCoverTitle(DECOY_PRESETS[mode].title);
                            setCoverMessage(DECOY_PRESETS[mode].message);
                          }}
                          className="px-2.5 py-1 rounded-lg text-[10px] font-medium"
                          style={
                            decoyMode === mode
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: 'rgba(148,163,184,0.2)' }
                          }
                        >
                          {DECOY_PRESETS[mode].label}
                        </button>
                      ))}
                    </div>

                    {decoyMode !== 'BLANK_SCREEN' && (
                      <>
                        <input
                          type="text"
                          value={coverTitle}
                          onChange={(e) => setCoverTitle(e.target.value)}
                          placeholder="Simulated Heading"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs text-[#0A1628] bg-white"
                        />
                        <textarea
                          rows={2}
                          value={coverMessage}
                          onChange={(e) => setCoverMessage(e.target.value)}
                          placeholder="Simulated Message"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs text-[#0A1628] bg-white"
                        />
                      </>
                    )}

                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-semibold opacity-80">Backdrop</span>
                      <div className="flex gap-2">
                        {['#0A1628', '#000000', '#1E293B', '#112240'].map((hex) => (
                          <button
                            key={hex}
                            type="button"
                            onClick={() => setCoverBg(hex)}
                            className={`w-6 h-6 rounded-full border-2 ${
                              coverBg === hex ? 'border-sky-400' : 'border-transparent'
                            }`}
                            style={{ backgroundColor: hex }}
                          />
                        ))}
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => setShowingCoverPreview(true)}
                      className="w-full py-2.5 rounded-xl text-white text-xs font-semibold"
                      style={{ backgroundColor: activeTheme.accent }}
                    >
                      Preview Simulated Decoy Screen
                    </button>
                  </div>
                )}

                {/* CAMERA MONITOR */}
                {activeScreen === 'camera_monitor' && (
                  <>
                    <div
                      className={`${cardClasses} p-4 space-y-2.5`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold">CAMERA Permission</span>
                        <span className="text-xs font-semibold text-emerald-500">
                          {cameraGranted ? 'Granted (Foreground Only)' : 'Denied'}
                        </span>
                      </div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => setCameraGranted(!cameraGranted)}
                          className="px-3 py-1.5 rounded-xl bg-slate-200 text-[#0A1628] text-xs font-medium"
                        >
                          {cameraGranted ? 'Revoke Test' : 'Grant Camera'}
                        </button>
                        {cameraGranted && (
                          <button
                            type="button"
                            onClick={() => setCameraTestActive(!cameraTestActive)}
                            className="px-3 py-1.5 rounded-xl text-white text-xs font-semibold"
                            style={{ backgroundColor: activeTheme.accent }}
                          >
                            {cameraTestActive ? 'Stop Test' : 'Run Camera Test'}
                          </button>
                        )}
                      </div>
                    </div>
                    {cameraTestActive && cameraGranted && (
                      <div
                        className={`${cardClasses} p-3.5`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="text-xs font-bold mb-1">Foreground CameraX Preview Active</div>
                        <div className="h-28 rounded-xl bg-[#0A1628] flex flex-col items-center justify-center text-sky-300">
                          <Camera className="w-7 h-7 mb-1" />
                          <span className="text-[11px]">System privacy indicator visible</span>
                        </div>
                      </div>
                    )}
                  </>
                )}

                {/* PRIVATE BROWSER */}
                {activeScreen === 'private_browser' && (
                  <div className="space-y-2.5">
                    <div className="flex gap-1.5">
                      <input
                        type="text"
                        value={urlInput}
                        onChange={(e) => setUrlInput(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && navigateBrowser(urlInput)}
                        className="flex-1 px-3 py-2 rounded-xl bg-white text-[#0A1628] text-xs"
                        placeholder="Search or enter URL..."
                      />
                      <button
                        type="button"
                        onClick={() => navigateBrowser(urlInput)}
                        className="px-3 py-2 rounded-xl text-white text-xs font-semibold"
                        style={{ backgroundColor: activeTheme.accent }}
                      >
                        Go
                      </button>
                    </div>
                    <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
                      {tabs.map((t, idx) => (
                        <div
                          key={t.id}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] shrink-0"
                          style={
                            idx === activeTabIdx
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: activeTheme.surface, color: activeTheme.fg }
                          }
                        >
                          <button
                            type="button"
                            onClick={() => {
                              setActiveTabIdx(idx);
                              setUrlInput(t.url);
                            }}
                          >
                            {t.title.slice(0, 14)}
                          </button>
                          {tabs.length > 1 && (
                            <button
                              type="button"
                              onClick={() => {
                                const next = tabs.filter((_, i) => i !== idx);
                                setTabs(next);
                                setActiveTabIdx(0);
                                setUrlInput(next[0].url);
                              }}
                              aria-label="Close tab"
                            >
                              <X className="w-3 h-3" />
                            </button>
                          )}
                        </div>
                      ))}
                      <button
                        type="button"
                        onClick={() => {
                          const fresh: BrowserTab = {
                            id: `tab-${Date.now()}`,
                            title: 'New Private Tab',
                            url: 'https://duckduckgo.com',
                            history: ['https://duckduckgo.com'],
                            historyIdx: 0,
                          };
                          setTabs((prev) => [...prev, fresh]);
                          setActiveTabIdx(tabs.length);
                          setUrlInput(fresh.url);
                        }}
                        className="p-1.5 rounded-lg"
                        style={{ backgroundColor: activeTheme.surface, color: activeTheme.secondary }}
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>
                    {httpWarningUrl && (
                      <div className="bg-amber-50 text-[#0A1628] rounded-2xl p-3 text-xs">
                        <div className="font-bold text-amber-800 mb-1">Safe Browsing Warning</div>
                        <p className="text-[11px] mb-2">Unencrypted HTTP blocked: {httpWarningUrl}</p>
                        <button
                          type="button"
                          onClick={() => setHttpWarningUrl(null)}
                          className="px-3 py-1 rounded-lg bg-amber-600 text-white text-[11px]"
                        >
                          Dismiss
                        </button>
                      </div>
                    )}
                    <div
                      className={`${cardClasses} p-4 min-h-[210px] flex flex-col justify-between`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div>
                        <div className="text-[11px] text-emerald-500 font-semibold mb-1">
                          Incognito WebView · Zero History
                        </div>
                        <div className="text-xs font-bold break-all">{currentTab.url}</div>
                      </div>
                      <div className="flex items-center justify-between pt-3 border-t border-slate-200/30">
                        <div className="flex gap-1">
                          <button
                            type="button"
                            disabled={currentTab.historyIdx <= 0}
                            onClick={() => {
                              const nextIdx = currentTab.historyIdx - 1;
                              const nextUrl = currentTab.history[nextIdx];
                              setTabs((prev) =>
                                prev.map((t, i) =>
                                  i === activeTabIdx ? { ...t, url: nextUrl, historyIdx: nextIdx } : t
                                )
                              );
                              setUrlInput(nextUrl);
                            }}
                            className="p-2 rounded-lg bg-slate-200 text-[#0A1628] disabled:opacity-40"
                          >
                            <ArrowLeft className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            disabled={currentTab.historyIdx >= currentTab.history.length - 1}
                            onClick={() => {
                              const nextIdx = currentTab.historyIdx + 1;
                              const nextUrl = currentTab.history[nextIdx];
                              setTabs((prev) =>
                                prev.map((t, i) =>
                                  i === activeTabIdx ? { ...t, url: nextUrl, historyIdx: nextIdx } : t
                                )
                              );
                              setUrlInput(nextUrl);
                            }}
                            className="p-2 rounded-lg bg-slate-200 text-[#0A1628] disabled:opacity-40"
                          >
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => navigateBrowser(urlInput)}
                            className="p-2 rounded-lg bg-slate-200 text-[#0A1628]"
                          >
                            <RefreshCw className="w-3.5 h-3.5" />
                          </button>
                        </div>
                        <button
                          type="button"
                          onClick={() => {
                            setTabs([
                              {
                                id: 'tab-reset',
                                title: 'DuckDuckGo Privacy',
                                url: 'https://duckduckgo.com',
                                history: ['https://duckduckgo.com'],
                                historyIdx: 0,
                              },
                            ]);
                            setActiveTabIdx(0);
                            setUrlInput('https://duckduckgo.com');
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-rose-500/15 text-rose-500 text-[11px] font-semibold"
                        >
                          Wipe Session
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* STATISTICS */}
                {activeScreen === 'statistics' && (
                  <>
                    <div className="grid grid-cols-2 gap-2.5">
                      <div
                        className={`${cardClasses} p-3`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="text-[11px] opacity-70">Protected Apps</div>
                        <div className="text-xl font-bold tabular-nums" style={{ color: activeTheme.accent }}>
                          {lockedCount}
                        </div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="text-[11px] opacity-70">Total Attempts</div>
                        <div className="text-xl font-bold tabular-nums">{stats.total}</div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="text-[11px] opacity-70">Successful</div>
                        <div className="text-xl font-bold text-emerald-500 tabular-nums">{stats.success}</div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="text-[11px] opacity-70">Failed</div>
                        <div className="text-xl font-bold text-rose-500 tabular-nums">{stats.failed}</div>
                      </div>
                    </div>
                    <div
                      className={`${cardClasses} p-3.5`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold mb-2">Weekly Activity</div>
                      <div className="flex items-end justify-between h-24 gap-2 pt-2">
                        {stats.weekly.map((w) => {
                          const maxTotal = Math.max(...stats.weekly.map((d) => d.ok + d.fail), 1);
                          const okH = Math.round((w.ok / maxTotal) * 72);
                          const failH = Math.round((w.fail / maxTotal) * 72);
                          return (
                            <div key={w.day} className="flex-1 flex flex-col items-center gap-1">
                              <div className="w-4 flex flex-col justify-end h-[72px] gap-0.5">
                                {failH > 0 && (
                                  <div className="w-full bg-rose-500 rounded-sm" style={{ height: `${failH}px` }} />
                                )}
                                {okH > 0 && (
                                  <div
                                    className="w-full rounded-sm"
                                    style={{ height: `${okH}px`, backgroundColor: activeTheme.accent }}
                                  />
                                )}
                              </div>
                              <span className="text-[10px] opacity-70">{w.day}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                    <div className="space-y-2">
                      {events.slice(0, 4).map((ev) => (
                        <div
                          key={ev.id}
                          className={`${cardClasses} p-3`}
                          style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                        >
                          <div className="flex justify-between text-xs font-semibold">
                            <span className={ev.success ? '' : 'text-rose-500'}>{ev.title}</span>
                            <span className="text-[10px] opacity-60 tabular-nums">{ev.time}</span>
                          </div>
                          <div className="text-[11px] opacity-70">{ev.detail}</div>
                        </div>
                      ))}
                    </div>
                  </>
                )}

                {/* THEME CUSTOMIZATION (8 THEMES + CUSTOM ACCENT + APP ICONS) */}
                {activeScreen === 'theme_customization' && (
                  <div className="space-y-3">
                    <div
                      className={`${cardClasses} p-3.5 space-y-2.5`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold">8 Live Themes</div>
                      <div className="grid grid-cols-2 gap-1.5">
                        {(Object.keys(THEME_MAP) as SimThemeId[]).map((t) => {
                          const tok = THEME_MAP[t];
                          const selected = themeId === t;
                          return (
                            <button
                              key={t}
                              type="button"
                              onClick={() => {
                                setThemeId(t);
                                setCustomAccent(null);
                              }}
                              className="p-2 rounded-xl flex items-center justify-between text-[11px] font-semibold border"
                              style={{
                                backgroundColor: tok.bg,
                                color: t === 'Pure White' ? '#0A1628' : '#FFFFFF',
                                borderColor: selected ? tok.accent : 'rgba(148,163,184,0.3)',
                              }}
                            >
                              <span className="flex items-center gap-1.5 truncate">
                                <span
                                  className="w-2.5 h-2.5 rounded-full shrink-0"
                                  style={{ backgroundColor: tok.accent }}
                                />
                                <span className="truncate">{t}</span>
                              </span>
                              {selected && <Check className="w-3 h-3 shrink-0" />}
                            </button>
                          );
                        })}
                      </div>
                    </div>

                    <div
                      className={`${cardClasses} p-3.5 space-y-2.5`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold">Custom Accent &amp; Card Style</div>
                      <div className="flex gap-2">
                        {['#2563EB', '#D4AF37', '#7C3AED', '#10B981', '#0284C7', '#E11D48'].map((hex) => (
                          <button
                            key={hex}
                            type="button"
                            onClick={() => setCustomAccent(hex)}
                            className="w-6 h-6 rounded-full border-2"
                            style={{
                              backgroundColor: hex,
                              borderColor: customAccent === hex ? activeTheme.cardText : 'transparent',
                            }}
                          />
                        ))}
                      </div>
                      <div className="flex gap-1.5">
                        {([null, true, false] as Array<boolean | null>).map((mode, idx) => (
                          <button
                            key={idx}
                            type="button"
                            onClick={() => setForceLightCards(mode)}
                            className="px-2.5 py-1 rounded-lg text-[10px] font-medium"
                            style={
                              forceLightCards === mode
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {mode === null ? 'Auto' : mode ? 'Light Cards' : 'Dark Cards'}
                          </button>
                        ))}
                      </div>
                      <div className="flex gap-1.5">
                        {(['Rounded', 'Outlined', 'Compact'] as const).map((cs) => (
                          <button
                            key={cs}
                            type="button"
                            onClick={() => setCardStyle(cs)}
                            className="px-2.5 py-1 rounded-lg text-[10px] font-medium"
                            style={
                              cardStyle === cs
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {cs}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold">Custom App Icon (&lt;activity-alias&gt;)</div>
                      <div className="grid grid-cols-2 gap-1.5">
                        {[
                          'SecureLock Shield',
                          'Midnight Vault',
                          'Minimalist Lock',
                          'Utility Disguise',
                        ].map((ic) => (
                          <button
                            key={ic}
                            type="button"
                            onClick={() => setAppIconStyle(ic)}
                            className="px-2.5 py-1.5 rounded-lg text-[10px] font-semibold"
                            style={
                              appIconStyle === ic
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {ic}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>
                )}

                {/* SETTINGS */}
                {activeScreen === 'settings' && (
                  <div className="space-y-2.5">
                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold">Lock Systems (4/6/Custom PIN, Pattern, Password)</div>
                      <div className="flex flex-wrap gap-1">
                        {(
                          [
                            ['PIN_4', '4-Digit PIN'],
                            ['PIN_6', '6-Digit PIN'],
                            ['PIN_CUSTOM', 'Custom PIN'],
                            ['PATTERN', 'Pattern'],
                            ['PASSWORD', 'Password'],
                          ] as Array<[SimLockMethod, string]>
                        ).map(([m, label]) => (
                          <button
                            key={m}
                            type="button"
                            onClick={() => {
                              setDefaultLockMethod(m);
                              setUnlockMethod(m);
                            }}
                            className="px-2 py-1 rounded-lg text-[10px] font-semibold"
                            style={
                              defaultLockMethod === m
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        <button
                          type="button"
                          onClick={() => setPinEditorOpen(!pinEditorOpen)}
                          className="px-2.5 py-1.5 rounded-lg text-white text-[11px] font-semibold"
                          style={{ backgroundColor: activeTheme.accent }}
                        >
                          Change PIN
                        </button>
                        <button
                          type="button"
                          onClick={() => setPatternEditorOpen(!patternEditorOpen)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px]"
                        >
                          Set Pattern
                        </button>
                        <button
                          type="button"
                          onClick={() => setPasswordEditorOpen(!passwordEditorOpen)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px]"
                        >
                          Set Password
                        </button>
                      </div>
                      {pinEditorOpen && (
                        <div className="space-y-1.5 pt-1">
                          <input
                            type="password"
                            maxLength={12}
                            value={newPinDraft}
                            onChange={(e) => setNewPinDraft(e.target.value.replace(/\D/g, ''))}
                            placeholder="New 4–12 digit PIN"
                            className="w-full px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs text-[#0A1628] bg-white"
                          />
                          <input
                            type="password"
                            maxLength={12}
                            value={confirmPinDraft}
                            onChange={(e) => setConfirmPinDraft(e.target.value.replace(/\D/g, ''))}
                            placeholder="Confirm PIN"
                            className="w-full px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs text-[#0A1628] bg-white"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              if (newPinDraft.length >= 4 && newPinDraft === confirmPinDraft) {
                                setMasterPin(newPinDraft);
                                setNewPinDraft('');
                                setConfirmPinDraft('');
                                setPinEditorOpen(false);
                              }
                            }}
                            className="px-3 py-1.5 rounded-lg text-white text-xs"
                            style={{ backgroundColor: activeTheme.accent }}
                          >
                            Confirm &amp; Save PIN
                          </button>
                        </div>
                      )}
                      {patternEditorOpen && (
                        <div className="pt-1 space-y-2">
                          <div className="grid grid-cols-3 gap-1.5 max-w-[140px]">
                            {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((n) => (
                              <button
                                key={n}
                                type="button"
                                onClick={() =>
                                  !newPatternDraft.includes(n) && setNewPatternDraft([...newPatternDraft, n])
                                }
                                className="h-8 rounded-lg text-xs font-semibold"
                                style={
                                  newPatternDraft.includes(n)
                                    ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                    : { backgroundColor: 'rgba(148,163,184,0.2)' }
                                }
                              >
                                {n + 1}
                              </button>
                            ))}
                          </div>
                          <button
                            type="button"
                            onClick={() => {
                              if (newPatternDraft.length >= 4) {
                                setSavedPattern(newPatternDraft);
                                setNewPatternDraft([]);
                                setPatternEditorOpen(false);
                              }
                            }}
                            className="px-3 py-1 rounded-lg text-white text-xs"
                            style={{ backgroundColor: activeTheme.accent }}
                          >
                            Save Pattern ({newPatternDraft.length}/4+)
                          </button>
                        </div>
                      )}
                      {passwordEditorOpen && (
                        <div className="flex gap-1.5 pt-1">
                          <input
                            type="password"
                            value={newPasswordDraft}
                            onChange={(e) => setNewPasswordDraft(e.target.value)}
                            placeholder="Alphanumeric password (6+)"
                            className="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs text-[#0A1628] bg-white"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              if (newPasswordDraft.length >= 6) {
                                setMasterPassword(newPasswordDraft);
                                setNewPasswordDraft('');
                                setPasswordEditorOpen(false);
                              }
                            }}
                            className="px-3 py-1.5 rounded-lg text-white text-xs"
                            style={{ backgroundColor: activeTheme.accent }}
                          >
                            Save
                          </button>
                        </div>
                      )}

                      <div className="flex items-center justify-between pt-1">
                        <span className="text-xs">Fingerprint BiometricPrompt</span>
                        <button
                          type="button"
                          onClick={() => setBiometricEnabled(!biometricEnabled)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                          style={
                            biometricEnabled
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                          }
                        >
                          {biometricEnabled ? 'On' : 'Off'}
                        </button>
                      </div>
                    </div>

                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                    >
                      <div className="text-xs font-bold">Emergency Recovery &amp; Theme Studio</div>
                      <div className="text-[11px] font-mono bg-slate-200/70 text-[#0A1628] px-2.5 py-1.5 rounded-lg">
                        {recoveryCode}
                      </div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
                            const seg = () =>
                              Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
                            setRecoveryCode(`${seg()}-${seg()}-${seg()}`);
                          }}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px] font-medium"
                        >
                          Rotate Key
                        </button>
                        <button
                          type="button"
                          onClick={() => onScreenChange('theme_customization')}
                          className="px-2.5 py-1.5 rounded-lg text-white text-[11px] font-semibold"
                          style={{ backgroundColor: activeTheme.accent }}
                        >
                          Theme Studio
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {/* PERMISSIONS */}
                {activeScreen === 'permissions' && (
                  <div className="space-y-2.5">
                    {[
                      { name: 'Display Over Other Apps', api: 'SYSTEM_ALERT_WINDOW', state: 'Granted', ok: true },
                      { name: 'Usage Access', api: 'PACKAGE_USAGE_STATS', state: 'Granted', ok: true },
                      { name: 'Accessibility Service', api: 'Optional Alternative', state: 'Optional', ok: false },
                      {
                        name: 'Notification Listener',
                        api: 'BIND_NOTIFICATION_LISTENER',
                        state: lockNotificationsEnabled ? 'Granted' : 'Off',
                        ok: lockNotificationsEnabled,
                      },
                      {
                        name: 'Foreground Camera',
                        api: 'CAMERA (User-Controlled)',
                        state: cameraGranted ? 'Granted' : 'Denied',
                        ok: cameraGranted,
                      },
                    ].map((p) => (
                      <div
                        key={p.name}
                        className={`${cardClasses} p-3.5`}
                        style={{ backgroundColor: activeTheme.card, color: activeTheme.cardText }}
                      >
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-bold">{p.name}</span>
                          <span className={`text-[11px] font-semibold ${p.ok ? 'text-emerald-500' : 'text-amber-500'}`}>
                            {p.state}
                          </span>
                        </div>
                        <div className="text-[10px] font-mono opacity-65 mt-0.5">{p.api}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Android Gesture Bar */}
        <div className="py-2.5 flex justify-center shrink-0">
          <button
            type="button"
            onClick={() => onScreenChange('dashboard')}
            className="w-28 h-1 rounded-full bg-white/35 hover:bg-white/60 transition-colors"
            aria-label="Return to Dashboard"
          />
        </div>
      </div>
    </div>
  );
}
