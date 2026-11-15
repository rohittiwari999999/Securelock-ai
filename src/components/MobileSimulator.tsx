import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Calendar,
  Camera,
  Check,
  ChevronLeft,
  ChevronRight,
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
  { packageName: 'com.whatsapp', name: 'WhatsApp', locked: true },
  { packageName: 'com.google.android.gm', name: 'Gmail', locked: true },
  { packageName: 'com.google.android.apps.photos', name: 'Google Photos', locked: true },
  { packageName: 'com.android.settings', name: 'System Settings', locked: true },
  { packageName: 'com.chase.sig.android', name: 'Banking Vault', locked: false },
];

export function MobileSimulator({ activeScreen, onScreenChange }: MobileSimulatorProps) {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [masterPin, setMasterPin] = useState('2580');
  const [savedPattern, setSavedPattern] = useState<number[]>([0, 1, 4, 7]);
  const [defaultOverlayMode, setDefaultOverlayMode] = useState<'PATTERN' | 'PIN'>('PATTERN');
  const [activeOverlayMode, setActiveOverlayMode] = useState<'PATTERN' | 'PIN'>('PATTERN');
  const [biometricEnabled, setBiometricEnabled] = useState(true);
  const [enteredPin, setEnteredPin] = useState('');
  const [enteredPattern, setEnteredPattern] = useState<number[]>([]);
  const [authError, setAuthError] = useState(false);

  const [relockTimeout, setRelockTimeout] = useState(30);
  const [lockOnScreenOff, setLockOnScreenOff] = useState(true);
  const [foregroundPromptEnabled, setForegroundPromptEnabled] = useState(true);
  const [intruderThreshold, setIntruderThreshold] = useState(3);
  const [lockNotificationsEnabled, setLockNotificationsEnabled] = useState(true);
  const [simAccessibilityEnabled, setSimAccessibilityEnabled] = useState(false);
  const [showAccessibilityHelp, setShowAccessibilityHelp] = useState(false);
  const [recoveryCode, setRecoveryCode] = useState('7K4M-9P2X-Q8WN');

  // Calendar Launch Disguise & Secret Date State (persisted once after initial setup)
  const initialToday = useMemo(() => new Date(), []);
  const savedSimSetup = useMemo(() => {
    try {
      const raw = window.localStorage.getItem('securelock_sim_calendar_setup_v2');
      return raw ? (JSON.parse(raw) as {
        done: boolean;
        secretDay: number;
        secretMonth: number;
        secretYear: string;
        secretPin?: string;
        masterPin?: string;
        defaultMode?: 'PATTERN' | 'PIN';
        biometric?: boolean;
        pattern: number[];
      }) : null;
    } catch {
      return null;
    }
  }, []);

  const [simAppLockConfigured, setSimAppLockConfigured] = useState<boolean>(savedSimSetup?.done ?? false);
  const [pendingChosenDate, setPendingChosenDate] = useState<{ day: number; month: number; year: number } | null>(null);

  const [setupMethodChoice, setSetupMethodChoice] = useState<'PATTERN' | 'PIN'>('PATTERN');
  const [setupPinDraft, setSetupPinDraft] = useState('');
  const [setupPinConfirmDraft, setSetupPinConfirmDraft] = useState('');
  const [setupBioChoice, setSetupBioChoice] = useState(true);
  const [setupFirstPattern, setSetupFirstPattern] = useState<number[] | null>(null);
  const [setupConfirmedPattern, setSetupConfirmedPattern] = useState<number[] | null>(null);
  const [setupPatternDraft, setSetupPatternDraft] = useState<number[]>([]);
  const [setupPatternHint, setSetupPatternHint] = useState<string>('Tap dots to draw pattern (4+ nodes)');
  const [appLockModalError, setAppLockModalError] = useState<string | null>(null);

  const [secretDateEnabled, setSecretDateEnabled] = useState(true);
  const [secretDay, setSecretDay] = useState(savedSimSetup?.secretDay ?? initialToday.getDate());
  const [secretMonth, setSecretMonth] = useState(savedSimSetup?.secretMonth ?? initialToday.getMonth() + 1);
  const [secretYear, setSecretYear] = useState<string>(savedSimSetup?.secretYear ?? '');
  const [secretPin, setSecretPin] = useState(savedSimSetup?.secretPin ?? '2580');

  useEffect(() => {
    if (savedSimSetup?.done) {
      if (Array.isArray(savedSimSetup.pattern) && savedSimSetup.pattern.length >= 4) {
        setSavedPattern(savedSimSetup.pattern);
      }
      if (savedSimSetup.masterPin && savedSimSetup.masterPin.length >= 4) {
        setMasterPin(savedSimSetup.masterPin);
      }
      if (savedSimSetup.defaultMode) {
        setDefaultOverlayMode(savedSimSetup.defaultMode);
        setActiveOverlayMode(savedSimSetup.defaultMode);
      }
      if (typeof savedSimSetup.biometric === 'boolean') {
        setBiometricEnabled(savedSimSetup.biometric);
      }
    }
  }, [savedSimSetup]);

  const [calYear, setCalYear] = useState(initialToday.getFullYear());
  const [calMonth, setCalMonth] = useState(initialToday.getMonth() + 1);
  const [calSelectedDay, setCalSelectedDay] = useState(initialToday.getDate());
  const [calTapState, setCalTapState] = useState({
    day: 0,
    month: 0,
    year: 0,
    count: 0,
    lastTapAt: 0,
  });

  useEffect(() => {
    if (calTapState.count === 0) return;
    const timer = window.setTimeout(() => {
      setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
    }, 4050);
    return () => window.clearTimeout(timer);
  }, [calTapState.count, calTapState.lastTapAt]);

  useEffect(() => {
    if (activeScreen !== 'calendar') {
      setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
    }
  }, [activeScreen]);

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
  const [vaultGateMode, setVaultGateMode] = useState<'PATTERN' | 'PIN'>('PATTERN');
  const [vaultPinInput, setVaultPinInput] = useState('');
  const [vaultPatternDraft, setVaultPatternDraft] = useState<number[]>([]);
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
  const [patternOverlayTarget, setPatternOverlayTarget] = useState<string | null>(null);

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

  const [stats] = useState({
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

  const [events] = useState<SimEvent[]>([
    {
      id: 'ev-1',
      title: 'Pattern unlock succeeded',
      detail: 'Target: WhatsApp',
      time: '09:45:10',
      success: true,
    },
    {
      id: 'ev-2',
      title: 'Pattern unlock failed',
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

  const lockedCount = useMemo(() => apps.filter((a) => a.locked).length, [apps]);
  const totalVaultUsed = useMemo(
    () => vaultItems.reduce((acc, it) => acc + it.sizeBytes, 0),
    [vaultItems]
  );

  const hexToRgb = (hex: string): [number, number, number] => {
    const clean = hex.replace('#', '');
    const full = clean.length === 3 ? clean.split('').map((c) => c + c).join('') : clean;
    const num = parseInt(full, 16);
    if (Number.isNaN(num)) return [255, 255, 255];
    return [(num >> 16) & 255, (num >> 8) & 255, num & 255];
  };

  const isLightColor = (hex: string): boolean => {
    const [r, g, b] = hexToRgb(hex);
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.62;
  };

  const cardClasses =
    cardStyle === 'Outlined'
      ? 'rounded-2xl transition-all'
      : cardStyle === 'Compact'
        ? 'rounded-xl transition-all'
        : 'rounded-2xl transition-all';

  const simCardStyle = useMemo(() => {
    const [r, g, b] = hexToRgb(activeTheme.card);
    const lightCard = isLightColor(activeTheme.card);
    const lightBg = isLightColor(activeTheme.bg);
    const isOutlined = cardStyle === 'Outlined';
    const isCompact = cardStyle === 'Compact';

    if (lightCard) {
      return {
        background: `linear-gradient(145deg, rgba(${r},${g},${b},0.88) 0%, rgba(${Math.max(r - 12, 0)},${Math.max(g - 8, 0)},${b},0.68) 100%)`,
        color: activeTheme.cardText,
        backdropFilter: 'blur(18px) saturate(150%)',
        WebkitBackdropFilter: 'blur(18px) saturate(150%)',
        border: isOutlined
          ? `1.5px solid ${activeTheme.accent}66`
          : '1px solid rgba(255,255,255,0.85)',
        boxShadow: isCompact
          ? lightBg
            ? '-4px -4px 10px rgba(255,255,255,0.85), 5px 6px 14px rgba(15,23,42,0.10), inset 1px 1px 1px rgba(255,255,255,0.9)'
            : `-3px -3px 10px rgba(255,255,255,0.12), 5px 7px 16px rgba(2,6,23,0.38), inset 1px 1px 1px rgba(255,255,255,0.9)`
          : lightBg
            ? '-7px -7px 16px rgba(255,255,255,0.9), 8px 10px 22px rgba(15,23,42,0.12), inset 1px 1px 1px rgba(255,255,255,0.94)'
            : `-5px -5px 14px rgba(255,255,255,0.15), 8px 12px 24px rgba(2,6,23,0.44), inset 1px 1px 1px rgba(255,255,255,0.94)`,
      };
    }

    return {
      background: `linear-gradient(145deg, rgba(${Math.min(r + 18, 255)},${Math.min(g + 18, 255)},${Math.min(b + 24, 255)},0.82) 0%, rgba(${r},${g},${b},0.62) 100%)`,
      color: activeTheme.cardText,
      backdropFilter: 'blur(18px) saturate(155%)',
      WebkitBackdropFilter: 'blur(18px) saturate(155%)',
      border: isOutlined
        ? `1.5px solid ${activeTheme.secondary}77`
        : `1px solid rgba(255,255,255,0.16)`,
      boxShadow: isCompact
        ? `-3px -3px 8px rgba(255,255,255,0.05), 5px 6px 14px rgba(0,0,0,0.55), inset 1px 1px 1px rgba(255,255,255,0.16)`
        : `-5px -5px 14px rgba(255,255,255,0.06), 8px 10px 22px rgba(0,0,0,0.62), inset 1px 1px 1px rgba(255,255,255,0.2)`,
    };
  }, [activeTheme, cardStyle]);

  const simBackdropStyle = useMemo(() => {
    const [ar, ag, ab] = hexToRgb(activeTheme.accent);
    const [sr, sg, sb] = hexToRgb(activeTheme.secondary);
    return {
      backgroundColor: activeTheme.bg,
      color: activeTheme.fg,
      backgroundImage: `radial-gradient(circle at 12% 10%, rgba(${sr},${sg},${sb},0.22) 0%, transparent 48%), radial-gradient(circle at 88% 82%, rgba(${ar},${ag},${ab},0.26) 0%, transparent 52%)`,
    };
  }, [activeTheme]);

  const simOrbStyle = useMemo(() => {
    const lightBg = isLightColor(activeTheme.bg);
    return {
      background: lightBg
        ? 'linear-gradient(145deg, rgba(255,255,255,0.85), rgba(226,232,240,0.62))'
        : 'linear-gradient(145deg, rgba(255,255,255,0.16), rgba(255,255,255,0.05))',
      color: activeTheme.secondary,
      border: lightBg ? '1px solid rgba(255,255,255,0.85)' : '1px solid rgba(255,255,255,0.18)',
      boxShadow: lightBg
        ? '-4px -4px 10px rgba(255,255,255,0.85), 5px 5px 12px rgba(15,23,42,0.12)'
        : '-3px -3px 8px rgba(255,255,255,0.08), 5px 6px 14px rgba(0,0,0,0.45)',
    };
  }, [activeTheme]);

  const simInsetWellStyle = useMemo(() => {
    const lightCard = isLightColor(activeTheme.card);
    return lightCard
      ? {
          background: 'linear-gradient(145deg, rgba(226,234,246,0.75), rgba(248,251,255,0.82))',
          border: '1px solid rgba(255,255,255,0.7)',
          boxShadow: 'inset 3px 3px 6px rgba(15,23,42,0.10), inset -3px -3px 6px rgba(255,255,255,0.88)',
        }
      : {
          background: 'linear-gradient(145deg, rgba(4,9,20,0.58), rgba(255,255,255,0.05))',
          border: '1px solid rgba(255,255,255,0.12)',
          boxShadow: 'inset 3px 3px 7px rgba(0,0,0,0.52), inset -2px -2px 6px rgba(255,255,255,0.06)',
        };
  }, [activeTheme]);

  const handlePatternTap = (node: number) => {
    if (enteredPattern.includes(node)) return;
    setEnteredPattern([...enteredPattern, node]);
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
              setPatternOverlayTarget(launchedTargetApp ?? 'WhatsApp');
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
        style={{
          backgroundColor: coverBg,
          backgroundImage: `radial-gradient(circle at 20% 20%, ${activeTheme.accent}33 0%, transparent 50%), radial-gradient(circle at 80% 80%, ${activeTheme.secondary}26 0%, transparent 50%)`,
        }}
      >
        <div
          className="rounded-3xl p-6 w-full text-center backdrop-blur-xl border border-white/80 shadow-[0_20px_48px_rgba(2,6,23,0.48),-6px_-6px_16px_rgba(255,255,255,0.12)]"
          style={{
            background: 'linear-gradient(145deg, rgba(255,255,255,0.92), rgba(241,246,255,0.78))',
          }}
        >
          <div
            className="w-14 h-14 rounded-full mx-auto flex items-center justify-center mb-4 border border-white/80 shadow-[-4px_-4px_10px_rgba(255,255,255,0.9),5px_6px_14px_rgba(15,23,42,0.14)]"
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
              setPatternOverlayTarget(launchedTargetApp ?? 'WhatsApp');
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
      subtitle: 'Pattern, PIN & Fingerprint',
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

  const calMonthCells = useMemo(() => {
    const first = new Date(calYear, calMonth - 1, 1);
    const startOffset = first.getDay();
    const daysInMonth = new Date(calYear, calMonth, 0).getDate();
    const prevMonthDays = new Date(calYear, calMonth - 1, 0).getDate();
    const cells: Array<{ day: number; inMonth: boolean }> = [];
    for (let i = startOffset - 1; i >= 0; i--) {
      cells.push({ day: prevMonthDays - i, inMonth: false });
    }
    for (let d = 1; d <= daysInMonth; d++) {
      cells.push({ day: d, inMonth: true });
    }
    let nextD = 1;
    while (cells.length < 42) {
      cells.push({ day: nextD++, inMonth: false });
    }
    return cells;
  }, [calYear, calMonth]);

  const changeCalMonth = (delta: number) => {
    const next = new Date(calYear, calMonth - 1 + delta, 1);
    setCalYear(next.getFullYear());
    setCalMonth(next.getMonth() + 1);
    const maxDay = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate();
    setCalSelectedDay((prev) => Math.min(prev, maxDay));
    setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
  };

  const handleCalDateTap = (day: number) => {
    setCalSelectedDay(day);
    const now = Date.now();
    const sameCell =
      calTapState.day === day &&
      calTapState.month === calMonth &&
      calTapState.year === calYear &&
      now - calTapState.lastTapAt <= 4000;
    const nextCount = sameCell ? calTapState.count + 1 : 1;

    if (!simAppLockConfigured) {
      if (nextCount >= 5) {
        setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
        setSetupFirstPattern(null);
        setSetupConfirmedPattern(null);
        setSetupPatternDraft([]);
        setSetupPatternHint('Tap dots to draw pattern (4+ nodes)');
        setAppLockModalError(null);
        setPendingChosenDate({ day, month: calMonth, year: calYear });
        return;
      }
      setCalTapState({
        day,
        month: calMonth,
        year: calYear,
        count: nextCount,
        lastTapAt: now,
      });
      return;
    }

    const yearMatches = secretYear.trim() === '' || Number(secretYear.trim()) === calYear;
    const matchesConfigured =
      secretDateEnabled && day === secretDay && calMonth === secretMonth && yearMatches;

    if (nextCount >= 5 && matchesConfigured) {
      setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
      onScreenChange('dashboard');
      return;
    }

    setCalTapState({
      day,
      month: calMonth,
      year: calYear,
      count: nextCount,
      lastTapAt: now,
    });
  };

  return (
    <div className="w-full max-w-[390px] mx-auto bg-gradient-to-br from-[#0F1B31] to-[#050B16] rounded-[44px] p-3 shadow-[-10px_-10px_28px_rgba(255,255,255,0.85),16px_20px_42px_rgba(15,23,42,0.28)] border border-white/30">
      <div
        className="rounded-[34px] overflow-hidden h-[740px] flex flex-col relative transition-colors border border-white/10"
        style={simBackdropStyle}
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
          ) : activeScreen === 'calendar' ? (
            <div
              className="min-h-full text-[#1F1F1F] p-4 flex flex-col gap-3.5 relative"
              style={{
                backgroundColor: '#EBF1FA',
                backgroundImage:
                  'radial-gradient(circle at 12% 8%, rgba(26,115,232,0.18) 0%, transparent 46%), radial-gradient(circle at 90% 86%, rgba(56,189,248,0.18) 0%, transparent 48%), linear-gradient(160deg, #F3F7FE 0%, #E7EFFB 100%)',
              }}
            >
              {/* Standalone Calendar Top Bar (zero AppLock branding) */}
              <div className="flex items-center justify-between px-1 pt-1">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-full neuglass-card flex items-center justify-center">
                    <Calendar className="w-4 h-4 text-[#1A73E8]" />
                  </div>
                  <span className="text-lg font-semibold tracking-tight text-[#0A1628]">Calendar</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setCalYear(initialToday.getFullYear());
                    setCalMonth(initialToday.getMonth() + 1);
                    setCalSelectedDay(initialToday.getDate());
                    setCalTapState({ day: 0, month: 0, year: 0, count: 0, lastTapAt: 0 });
                  }}
                  className="neuglass-pill text-xs font-semibold text-[#1A73E8] px-3 py-1.5 rounded-full"
                >
                  Today
                </button>
              </div>

              {/* Visual First-Time Instruction Banner (only before AppLock setup is completed) */}
              {!simAppLockConfigured && (
                <div
                  className="rounded-3xl p-3.5 space-y-2 border border-white/80 backdrop-blur-md shadow-[-6px_-6px_14px_rgba(255,255,255,0.85),6px_8px_18px_rgba(26,115,232,0.14)]"
                  style={{
                    background: 'linear-gradient(145deg, rgba(219,234,254,0.82), rgba(239,246,255,0.65))',
                    color: '#041E49',
                  }}
                >
                  <div className="text-xs font-bold">Step 1: Select Secret Date</div>
                  <p className="text-[11px] leading-relaxed">
                    Choose any date on the calendar below and tap it 5 times to continue to AppLock setup.
                  </p>
                  <div className="flex items-center justify-between pt-0.5 neuglass-inset rounded-xl px-2.5 py-1.5">
                    <div className="flex items-center gap-1.5">
                      {[0, 1, 2, 3, 4].map((dot) => (
                        <span
                          key={dot}
                          className={`w-2.5 h-2.5 rounded-full transition-colors ${
                            dot < calTapState.count
                              ? 'bg-[#1A73E8] shadow-[0_0_8px_rgba(26,115,232,0.6)]'
                              : 'bg-[#041E49]/20'
                          }`}
                        />
                      ))}
                    </div>
                    <span className="text-[11px] font-semibold tabular-nums">
                      {calTapState.count} / 5 taps
                    </span>
                  </div>
                </div>
              )}

              {/* Monthly Grid Card */}
              <div className="neuglass-card rounded-3xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="text-base font-bold text-[#0A1628]">
                    {new Date(calYear, calMonth - 1, 1).toLocaleDateString('en-US', {
                      month: 'long',
                      year: 'numeric',
                    })}
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={() => changeCalMonth(-1)}
                      className="w-8 h-8 rounded-full neuglass-pill flex items-center justify-center text-slate-700"
                      aria-label="Previous month"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => changeCalMonth(1)}
                      className="w-8 h-8 rounded-full neuglass-pill flex items-center justify-center text-slate-700"
                      aria-label="Next month"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                <div className="grid grid-cols-7 text-center text-[11px] font-semibold">
                  {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((wd, idx) => (
                    <div
                      key={wd}
                      className={idx === 0 ? 'text-[#1A73E8] py-1' : 'text-[#5F6368] py-1'}
                    >
                      {wd}
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-7 gap-y-1 text-center">
                  {calMonthCells.map((cell, idx) => {
                    const isToday =
                      cell.inMonth &&
                      cell.day === initialToday.getDate() &&
                      calMonth === initialToday.getMonth() + 1 &&
                      calYear === initialToday.getFullYear();
                    const isSelected = cell.inMonth && cell.day === calSelectedDay;
                    return (
                      <button
                        key={idx}
                        type="button"
                        disabled={!cell.inMonth}
                        onClick={() => cell.inMonth && handleCalDateTap(cell.day)}
                        className={`aspect-square mx-auto w-9 h-9 rounded-full flex items-center justify-center text-xs transition-all ${
                          !cell.inMonth
                            ? 'text-slate-300 cursor-default'
                            : isToday
                              ? 'bg-gradient-to-br from-[#1A73E8] to-[#0284C7] text-white font-bold shadow-[0_4px_12px_rgba(26,115,232,0.4)]'
                              : isSelected
                                ? 'neuglass-inset text-[#041E49] font-bold'
                                : 'text-[#1F1F1F] hover:bg-white/60 font-medium'
                        }`}
                      >
                        {cell.day}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Standalone Day Agenda Summary */}
              <div className="neuglass-card rounded-3xl p-4 space-y-2">
                <div className="text-sm font-bold text-[#0A1628]">
                  {new Date(calYear, calMonth - 1, calSelectedDay).toLocaleDateString('en-US', {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric',
                    year: 'numeric',
                  })}
                </div>
                <div className="flex gap-2 text-[11px] text-[#5F6368]">
                  <span className="neuglass-inset px-2.5 py-1 rounded-xl font-medium text-[#0A1628]">
                    Week{' '}
                    {Math.ceil(
                      ((new Date(calYear, calMonth - 1, calSelectedDay).getTime() -
                        new Date(calYear, 0, 1).getTime()) /
                        86400000 +
                        new Date(calYear, 0, 1).getDay() +
                        1) /
                        7
                    )}
                  </span>
                  <span className="neuglass-inset px-2.5 py-1 rounded-xl">No upcoming reminders</span>
                </div>
                <p className="text-xs text-[#5F6368] pt-1">No events scheduled for this day</p>
              </div>

              {/* Step 2: Direct Pattern Lock Setup Window */}
              {pendingChosenDate && !simAppLockConfigured && (
                <div className="absolute inset-0 bg-[#0A1628]/45 backdrop-blur-md flex items-center justify-center p-4 z-30">
                  <div className="neuglass-card rounded-3xl p-5 w-full max-w-[310px] space-y-3 text-[#0A1628] max-h-[610px] overflow-y-auto">
                    <div className="text-base font-bold">Step 2: Set App Lock</div>
                    <p className="text-[11px] text-[#5F6368]">
                      Choose Pattern or PIN and enable Fingerprint unlock for your locked apps:
                    </p>

                    <div className="flex gap-1.5">
                      {(['PATTERN', 'PIN'] as const).map((m) => (
                        <button
                          key={m}
                          type="button"
                          onClick={() => {
                            setSetupMethodChoice(m);
                            setAppLockModalError(null);
                          }}
                          className={`px-3 py-1 rounded-lg text-xs font-semibold ${
                            setupMethodChoice === m
                              ? 'bg-[#1A73E8] text-white'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {m === 'PATTERN' ? 'Pattern' : 'PIN'}
                        </button>
                      ))}
                    </div>

                    {setupMethodChoice === 'PATTERN' ? (
                      <div className="space-y-2">
                        <div className="text-[11px] font-medium text-[#1A73E8]">{setupPatternHint}</div>
                        <div className="grid grid-cols-3 gap-2 max-w-[165px] mx-auto neuglass-inset p-3 rounded-2xl">
                          {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((n) => {
                            const idx = setupPatternDraft.indexOf(n);
                            return (
                              <button
                                key={n}
                                type="button"
                                onClick={() =>
                                  !setupPatternDraft.includes(n) &&
                                  setSetupPatternDraft([...setupPatternDraft, n])
                                }
                                className={`h-9 rounded-full text-xs font-bold transition-all ${
                                  idx >= 0
                                    ? 'bg-[#1A73E8] text-white shadow-[0_3px_10px_rgba(26,115,232,0.45)]'
                                    : 'neuglass-pill text-slate-700'
                                }`}
                              >
                                {idx >= 0 ? idx + 1 : n + 1}
                              </button>
                            );
                          })}
                        </div>
                        <div className="flex justify-center gap-2">
                          <button
                            type="button"
                            onClick={() => setSetupPatternDraft([])}
                            className="px-2.5 py-1 rounded-lg bg-slate-100 text-[11px]"
                          >
                            Clear
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              if (setupPatternDraft.length < 4) {
                                setSetupPatternHint('Connect at least 4 nodes');
                                return;
                              }
                              if (!setupFirstPattern) {
                                setSetupFirstPattern(setupPatternDraft);
                                setSetupPatternDraft([]);
                                setSetupPatternHint('Tap same pattern again to confirm');
                              } else if (
                                setupFirstPattern.length === setupPatternDraft.length &&
                                setupFirstPattern.every((v, i) => v === setupPatternDraft[i])
                              ) {
                                setSetupConfirmedPattern(setupPatternDraft);
                                setSetupPatternDraft([]);
                                setSetupPatternHint('Lock Pattern confirmed!');
                              } else {
                                setSetupFirstPattern(null);
                                setSetupConfirmedPattern(null);
                                setSetupPatternDraft([]);
                                setSetupPatternHint('Did not match. Draw again.');
                              }
                            }}
                            className="px-3 py-1 rounded-lg bg-[#1A73E8] text-white text-[11px] font-semibold"
                          >
                            {setupFirstPattern ? 'Confirm Pattern' : 'Set First Pattern'}
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-2">
                        <input
                          type="password"
                          maxLength={12}
                          value={setupPinDraft}
                          onChange={(e) => setSetupPinDraft(e.target.value.replace(/\D/g, ''))}
                          placeholder="Enter PIN (4–12 digits)"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs bg-white"
                        />
                        <input
                          type="password"
                          maxLength={12}
                          value={setupPinConfirmDraft}
                          onChange={(e) => setSetupPinConfirmDraft(e.target.value.replace(/\D/g, ''))}
                          placeholder="Confirm PIN"
                          className="w-full px-3 py-2 rounded-xl border border-slate-300 text-xs bg-white"
                        />
                      </div>
                    )}

                    <div className="flex items-center justify-between pt-1">
                      <span className="text-[11px] font-medium">Also unlock with Fingerprint</span>
                      <button
                        type="button"
                        onClick={() => setSetupBioChoice(!setupBioChoice)}
                        className={`px-2.5 py-1 rounded-lg text-[10px] font-semibold ${
                          setupBioChoice ? 'bg-[#1A73E8] text-white' : 'bg-slate-200 text-slate-700'
                        }`}
                      >
                        {setupBioChoice ? 'On' : 'Off'}
                      </button>
                    </div>

                    {appLockModalError && (
                      <div className="text-[11px] font-medium text-rose-600">{appLockModalError}</div>
                    )}

                    <div className="flex gap-2 pt-1">
                      <button
                        type="button"
                        onClick={() => setPendingChosenDate(null)}
                        className="px-3 py-2 rounded-xl text-xs font-medium text-[#5F6368] hover:bg-slate-100"
                      >
                        Change Date
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (setupMethodChoice === 'PATTERN') {
                            if (!setupConfirmedPattern || setupConfirmedPattern.length < 4) {
                              setAppLockModalError('Confirm your pattern twice first');
                              return;
                            }
                            setSavedPattern(setupConfirmedPattern);
                          } else {
                            if (setupPinDraft.length < 4) {
                              setAppLockModalError('PIN must be 4–12 digits');
                              return;
                            }
                            if (setupPinDraft !== setupPinConfirmDraft) {
                              setAppLockModalError('PINs do not match');
                              return;
                            }
                            setMasterPin(setupPinDraft);
                          }
                          setDefaultOverlayMode(setupMethodChoice);
                          setActiveOverlayMode(setupMethodChoice);
                          setBiometricEnabled(setupBioChoice);
                          setSecretDay(pendingChosenDate.day);
                          setSecretMonth(pendingChosenDate.month);
                          setSecretYear('');
                          setSecretDateEnabled(true);
                          setSimAppLockConfigured(true);
                          setPendingChosenDate(null);
                          setAppLockModalError(null);
                          try {
                            window.localStorage.setItem(
                              'securelock_sim_calendar_setup_v2',
                              JSON.stringify({
                                done: true,
                                secretDay: pendingChosenDate.day,
                                secretMonth: pendingChosenDate.month,
                                secretYear: '',
                                secretPin: secretPin,
                                masterPin: setupMethodChoice === 'PIN' ? setupPinDraft : masterPin,
                                defaultMode: setupMethodChoice,
                                biometric: setupBioChoice,
                                pattern: setupConfirmedPattern ?? savedPattern,
                              })
                            );
                          } catch {
                            // ignore storage errors
                          }
                        }}
                        className="flex-1 py-2 rounded-xl text-xs font-semibold bg-[#1A73E8] text-white"
                      >
                        Save &amp; Finish
                      </button>
                    </div>
                  </div>
                </div>
              )}
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
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => onScreenChange('permissions')}
                    className="w-9 h-9 rounded-full flex items-center justify-center neuglass-orb"
                    style={simOrbStyle}
                    aria-label="Permissions Manager"
                  >
                    <Shield className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onScreenChange('settings')}
                    className="w-9 h-9 rounded-full flex items-center justify-center neuglass-orb"
                    style={simOrbStyle}
                    aria-label="Settings"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div
                className="rounded-3xl p-3.5 flex items-center justify-between backdrop-blur-xl"
                style={{
                  background: `linear-gradient(135deg, ${activeTheme.surface}D9 0%, ${activeTheme.bg}B8 100%)`,
                  border: `1px solid ${activeTheme.secondary}33`,
                  boxShadow: `-5px -5px 14px rgba(255,255,255,0.08), 8px 10px 22px rgba(0,0,0,0.38), inset 1px 1px 1px rgba(255,255,255,0.2)`,
                }}
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-10 h-10 rounded-2xl flex items-center justify-center border border-white/20"
                    style={{
                      backgroundColor: `${activeTheme.accent}33`,
                      color: activeTheme.secondary,
                      boxShadow: 'inset 2px 2px 5px rgba(0,0,0,0.3)',
                    }}
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
                  onClick={() => onScreenChange('calendar')}
                  className="text-xs font-semibold px-3 py-1.5 rounded-xl border border-white/20 backdrop-blur-md"
                  style={{
                    backgroundColor: `${activeTheme.accent}2B`,
                    color: activeTheme.secondary,
                  }}
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
                      className={`${cardClasses} p-3 text-left transition-transform active:scale-[0.98] flex flex-col justify-between min-h-[112px]`}
                      style={simCardStyle}
                    >
                      <div className="flex items-center justify-between w-full mb-2">
                        <span
                          className="w-8 h-8 rounded-xl flex items-center justify-center"
                          style={{
                            ...simInsetWellStyle,
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
              <div
                className="px-4 py-3 border-b border-white/15 flex items-center justify-between shrink-0 backdrop-blur-xl"
                style={{
                  background: `linear-gradient(180deg, ${activeTheme.surface}B8 0%, ${activeTheme.bg}88 100%)`,
                }}
              >
                <div className="flex items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() => onScreenChange('dashboard')}
                    className="w-8 h-8 rounded-full flex items-center justify-center neuglass-orb"
                    style={simOrbStyle}
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
                  onClick={() => onScreenChange('calendar')}
                  className="text-[11px] font-semibold px-2.5 py-1 rounded-xl border border-white/15"
                  style={{
                    backgroundColor: `${activeTheme.accent}24`,
                    color: activeTheme.secondary,
                  }}
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
                        style={simCardStyle}
                      >
                        <Lock className="w-9 h-9 mx-auto" style={{ color: activeTheme.accent }} />
                        <div className="text-sm font-bold">Vault Locked</div>
                        <p className="text-xs opacity-75">
                          Use Pattern, PIN ({masterPin}), or Fingerprint to decrypt private albums.
                        </p>
                        <div className="flex justify-center gap-1.5">
                          {(['PATTERN', 'PIN'] as const).map((m) => (
                            <button
                              key={m}
                              type="button"
                              onClick={() => {
                                setVaultGateMode(m);
                                setVaultPinError(null);
                              }}
                              className="px-2.5 py-1 rounded-lg text-[10px] font-semibold"
                              style={
                                vaultGateMode === m
                                  ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                  : { backgroundColor: 'rgba(148,163,184,0.2)' }
                              }
                            >
                              {m === 'PATTERN' ? 'Pattern' : 'PIN'}
                            </button>
                          ))}
                        </div>
                        {vaultGateMode === 'PATTERN' ? (
                          <>
                            <div
                              className="grid grid-cols-3 gap-2 max-w-[156px] mx-auto p-3 rounded-2xl"
                              style={simInsetWellStyle}
                            >
                              {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((n) => {
                                const idx = vaultPatternDraft.indexOf(n);
                                return (
                                  <button
                                    key={n}
                                    type="button"
                                    onClick={() =>
                                      !vaultPatternDraft.includes(n) &&
                                      setVaultPatternDraft([...vaultPatternDraft, n])
                                    }
                                    className="h-9 rounded-full text-xs font-bold neuglass-orb"
                                    style={
                                      idx >= 0
                                        ? {
                                            backgroundColor: activeTheme.accent,
                                            color: '#FFFFFF',
                                            boxShadow: `0 4px 12px ${activeTheme.accent}66`,
                                          }
                                        : {
                                            backgroundColor: 'rgba(255,255,255,0.86)',
                                            color: '#0A1628',
                                            border: '1px solid rgba(255,255,255,0.9)',
                                            boxShadow: '-2px -2px 6px rgba(255,255,255,0.85), 3px 3px 8px rgba(15,23,42,0.12)',
                                          }
                                    }
                                  >
                                    {idx >= 0 ? idx + 1 : n + 1}
                                  </button>
                                );
                              })}
                            </div>
                            {vaultPinError && (
                              <div className="text-xs text-rose-500 font-medium">{vaultPinError}</div>
                            )}
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() => {
                                  setVaultPatternDraft([]);
                                  setVaultPinError(null);
                                }}
                                className="px-3 py-2 rounded-xl bg-slate-200 text-[#0A1628] text-xs font-semibold"
                              >
                                Clear
                              </button>
                              <button
                                type="button"
                                onClick={() => {
                                  const ok =
                                    vaultPatternDraft.length >= 4 &&
                                    vaultPatternDraft.length === savedPattern.length &&
                                    vaultPatternDraft.every((v, i) => v === savedPattern[i]);
                                  if (ok) {
                                    setVaultUnlocked(true);
                                    setVaultPatternDraft([]);
                                    setVaultPinError(null);
                                  } else {
                                    setVaultPatternDraft([]);
                                    setVaultPinError('Incorrect Lock Pattern');
                                  }
                                }}
                                className="flex-1 py-2 rounded-xl text-white text-xs font-semibold"
                                style={{ backgroundColor: activeTheme.accent }}
                              >
                                Unlock
                              </button>
                              {biometricEnabled && (
                                <button
                                  type="button"
                                  onClick={() => {
                                    setVaultUnlocked(true);
                                    setVaultPinError(null);
                                  }}
                                  className="px-3 py-2 rounded-xl bg-slate-200 text-[#0A1628] text-xs font-semibold inline-flex items-center gap-1"
                                >
                                  <Fingerprint className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </>
                        ) : (
                          <>
                            <input
                              type="password"
                              value={vaultPinInput}
                              onChange={(e) => setVaultPinInput(e.target.value.replace(/\D/g, ''))}
                              placeholder="Enter PIN"
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
                              {biometricEnabled && (
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
                              )}
                            </div>
                          </>
                        )}
                      </div>
                    ) : (
                      <>
                        <div
                          className={`${cardClasses} p-3.5 space-y-2.5`}
                          style={simCardStyle}
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
                            style={simCardStyle}
                          >
                            No items in this album. Tap Import Photos/Videos to encrypt local files.
                          </div>
                        ) : vaultGridView ? (
                          <div className="grid grid-cols-2 gap-2">
                            {filteredVaultItems.map((it) => (
                              <div
                                key={it.id}
                                className={`${cardClasses} p-2.5 space-y-1.5`}
                                style={simCardStyle}
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
                                style={simCardStyle}
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
                      style={simCardStyle}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <div>
                          <div className="text-xs font-bold">Real-Time Protection Engine</div>
                          <div className="text-[11px] opacity-70">
                            {simAccessibilityEnabled
                              ? 'Accessibility Active (Instant Lock)'
                              : 'Usage Access + Overlay Active (Accessibility Optional)'}
                          </div>
                        </div>
                        <span
                          className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                            simAccessibilityEnabled
                              ? 'bg-emerald-500/15 text-emerald-600'
                              : 'bg-amber-500/15 text-amber-600'
                          }`}
                        >
                          {simAccessibilityEnabled ? 'INSTANT' : 'SETUP'}
                        </span>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        <button
                          type="button"
                          onClick={() => setShowAccessibilityHelp(true)}
                          className="px-2.5 py-1.5 rounded-lg text-[11px] font-semibold text-white"
                          style={{ backgroundColor: activeTheme.accent }}
                        >
                          {simAccessibilityEnabled ? 'Accessibility Guide' : 'Open Accessibility / Help'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setSimAccessibilityEnabled(!simAccessibilityEnabled)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px] font-medium"
                        >
                          {simAccessibilityEnabled ? 'Turn Off' : 'Simulate Enable'}
                        </button>
                      </div>
                    </div>

                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={simCardStyle}
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
                        style={simCardStyle}
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
                                  setEnteredPattern([]);
                                  setEnteredPin('');
                                  setActiveOverlayMode(defaultOverlayMode);
                                  setAuthError(false);
                                  setPatternOverlayTarget(app.name);
                                }}
                                className="px-2 py-1 rounded-lg bg-slate-200 text-[#0A1628] text-[10px] font-medium"
                              >
                                Test Lock
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
                          <div className="text-[10px] font-medium text-emerald-600">
                            Protected: Pattern · PIN{biometricEnabled ? ' · Fingerprint' : ''}
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
                      style={simCardStyle}
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
                        style={simCardStyle}
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
                      style={simCardStyle}
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
                        style={simCardStyle}
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
                    style={simCardStyle}
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
                      style={simCardStyle}
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
                        style={simCardStyle}
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
                      style={simCardStyle}
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
                        style={simCardStyle}
                      >
                        <div className="text-[11px] opacity-70">Protected Apps</div>
                        <div className="text-xl font-bold tabular-nums" style={{ color: activeTheme.accent }}>
                          {lockedCount}
                        </div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={simCardStyle}
                      >
                        <div className="text-[11px] opacity-70">Total Attempts</div>
                        <div className="text-xl font-bold tabular-nums">{stats.total}</div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={simCardStyle}
                      >
                        <div className="text-[11px] opacity-70">Successful</div>
                        <div className="text-xl font-bold text-emerald-500 tabular-nums">{stats.success}</div>
                      </div>
                      <div
                        className={`${cardClasses} p-3`}
                        style={simCardStyle}
                      >
                        <div className="text-[11px] opacity-70">Failed</div>
                        <div className="text-xl font-bold text-rose-500 tabular-nums">{stats.failed}</div>
                      </div>
                    </div>
                    <div
                      className={`${cardClasses} p-3.5`}
                      style={simCardStyle}
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
                          style={simCardStyle}
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
                      style={simCardStyle}
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
                      style={simCardStyle}
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
                      style={simCardStyle}
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
                      style={simCardStyle}
                    >
                      <div className="text-xs font-bold">Lock Methods (Pattern, PIN &amp; Fingerprint)</div>
                      <p className="text-[11px] opacity-75">
                        Keep Pattern, PIN, and Fingerprint enabled together without conflicting gates.
                      </p>
                      <div className="flex gap-1.5">
                        {(['PATTERN', 'PIN'] as const).map((m) => (
                          <button
                            key={m}
                            type="button"
                            onClick={() => {
                              setDefaultOverlayMode(m);
                              setActiveOverlayMode(m);
                            }}
                            className="px-2.5 py-1 rounded-lg text-[10px] font-semibold"
                            style={
                              defaultOverlayMode === m
                                ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                                : { backgroundColor: 'rgba(148,163,184,0.2)' }
                            }
                          >
                            Default: {m === 'PATTERN' ? 'Pattern' : 'PIN'}
                          </button>
                        ))}
                      </div>
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        <button
                          type="button"
                          onClick={() => setPatternEditorOpen(!patternEditorOpen)}
                          className="px-2.5 py-1.5 rounded-lg text-white text-[11px] font-semibold"
                          style={{ backgroundColor: activeTheme.accent }}
                        >
                          {patternEditorOpen ? 'Close Pattern' : 'Change Pattern'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setPinEditorOpen(!pinEditorOpen)}
                          className="px-2.5 py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px] font-medium"
                        >
                          {pinEditorOpen ? 'Close PIN' : 'Change PIN'}
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
                            Save PIN
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
                          <div className="flex gap-1.5">
                            <button
                              type="button"
                              onClick={() => setNewPatternDraft([])}
                              className="px-2.5 py-1 rounded-lg bg-slate-200 text-[#0A1628] text-xs"
                            >
                              Clear
                            </button>
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
                      style={simCardStyle}
                    >
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-bold">Secret Date Unlock (Calendar Stealth)</div>
                        <button
                          type="button"
                          onClick={() => setSecretDateEnabled(!secretDateEnabled)}
                          className="px-2.5 py-1 rounded-lg text-[11px] font-semibold"
                          style={
                            secretDateEnabled
                              ? { backgroundColor: activeTheme.accent, color: '#FFFFFF' }
                              : { backgroundColor: '#CBD5E1', color: '#0A1628' }
                          }
                        >
                          {secretDateEnabled ? 'On' : 'Off'}
                        </button>
                      </div>
                      <div className="grid grid-cols-3 gap-1.5">
                        <select
                          value={secretMonth}
                          onChange={(e) => setSecretMonth(Number(e.target.value))}
                          className="px-2 py-1.5 rounded-lg border border-slate-300 text-[11px] text-[#0A1628] bg-white"
                        >
                          {[
                            'Jan',
                            'Feb',
                            'Mar',
                            'Apr',
                            'May',
                            'Jun',
                            'Jul',
                            'Aug',
                            'Sep',
                            'Oct',
                            'Nov',
                            'Dec',
                          ].map((m, idx) => (
                            <option key={m} value={idx + 1}>
                              {m}
                            </option>
                          ))}
                        </select>
                        <input
                          type="number"
                          min={1}
                          max={31}
                          value={secretDay}
                          onChange={(e) =>
                            setSecretDay(Math.max(1, Math.min(31, Number(e.target.value) || 1)))
                          }
                          placeholder="Day"
                          className="px-2 py-1.5 rounded-lg border border-slate-300 text-[11px] text-[#0A1628] bg-white"
                        />
                        <input
                          type="text"
                          maxLength={4}
                          value={secretYear}
                          onChange={(e) => setSecretYear(e.target.value.replace(/\D/g, ''))}
                          placeholder="Year (opt)"
                          className="px-2 py-1.5 rounded-lg border border-slate-300 text-[11px] text-[#0A1628] bg-white"
                        />
                      </div>
                      <input
                        type="password"
                        maxLength={8}
                        value={secretPin}
                        onChange={(e) => {
                          const v = e.target.value.replace(/\D/g, '');
                          if (v.length >= 4 || v.length > 0) setSecretPin(v);
                        }}
                        placeholder="Secret PIN (4–8 digits)"
                        className="w-full px-2.5 py-1.5 rounded-lg border border-slate-300 text-xs text-[#0A1628] bg-white"
                      />
                      <button
                        type="button"
                        onClick={() => {
                          try {
                            window.localStorage.removeItem('securelock_sim_calendar_setup_v2');
                          } catch {
                            // ignore
                          }
                          setSetupFirstPattern(null);
                          setSetupConfirmedPattern(null);
                          setSetupPatternDraft([]);
                          setPendingChosenDate(null);
                          setSimAppLockConfigured(false);
                          onScreenChange('calendar');
                        }}
                        className="w-full py-1.5 rounded-lg bg-slate-200 text-[#0A1628] text-[11px] font-medium"
                      >
                        Replay One-Time Startup Setup
                      </button>
                    </div>

                    <div
                      className={`${cardClasses} p-3.5 space-y-2`}
                      style={simCardStyle}
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
                      { name: 'Display Over Other Apps', api: 'SYSTEM_ALERT_WINDOW', state: 'Granted', ok: true, isAcc: false },
                      { name: 'Usage Access', api: 'PACKAGE_USAGE_STATS', state: 'Granted', ok: true, isAcc: false },
                      {
                        name: 'Accessibility Service (Instant Lock)',
                        api: 'SecureLock App Protection Monitor',
                        state: simAccessibilityEnabled ? 'Granted' : 'Tap for Help / Direct Open',
                        ok: simAccessibilityEnabled,
                        isAcc: true,
                      },
                      {
                        name: 'Notification Listener',
                        api: 'BIND_NOTIFICATION_LISTENER',
                        state: lockNotificationsEnabled ? 'Granted' : 'Off',
                        ok: lockNotificationsEnabled,
                        isAcc: false,
                      },
                      {
                        name: 'Foreground Camera',
                        api: 'CAMERA (User-Controlled)',
                        state: cameraGranted ? 'Granted' : 'Denied',
                        ok: cameraGranted,
                        isAcc: false,
                      },
                    ].map((p) => (
                      <div
                        key={p.name}
                        className={`${cardClasses} p-3.5`}
                        style={simCardStyle}
                      >
                        <div className="flex justify-between items-center gap-2">
                          <span className="text-xs font-bold">{p.name}</span>
                          <span className={`text-[11px] font-semibold ${p.ok ? 'text-emerald-500' : 'text-amber-500'}`}>
                            {p.state}
                          </span>
                        </div>
                        <div className="text-[10px] font-mono opacity-65 mt-0.5">{p.api}</div>
                        {p.isAcc && (
                          <div className="flex gap-1.5 mt-2">
                            <button
                              type="button"
                              onClick={() => setSimAccessibilityEnabled(!simAccessibilityEnabled)}
                              className="px-2.5 py-1 rounded-lg text-[10px] font-semibold text-white"
                              style={{ backgroundColor: activeTheme.accent }}
                            >
                              {simAccessibilityEnabled ? 'Review Directly' : 'Open Directly'}
                            </button>
                            <button
                              type="button"
                              onClick={() => setShowAccessibilityHelp(true)}
                              className="px-2.5 py-1 rounded-lg bg-slate-200 text-[#0A1628] text-[10px] font-semibold"
                            >
                              How to Enable (?)
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Unified App Lock Overlay Preview (Pattern + PIN + Fingerprint) */}
        {patternOverlayTarget && (
          <div
            className="absolute inset-0 z-40 p-5 flex flex-col justify-between backdrop-blur-xl"
            style={simBackdropStyle}
          >
            <div className="text-center pt-5">
              <div
                className="w-16 h-16 rounded-full mx-auto flex items-center justify-center neuglass-orb"
                style={simOrbStyle}
              >
                <Lock className="w-7 h-7" style={{ color: activeTheme.secondary }} />
              </div>
              <h3 className="mt-3 text-lg font-bold">{patternOverlayTarget}</h3>
              <p className={`mt-1 text-xs ${authError ? 'text-rose-400' : 'opacity-75'}`}>
                {authError
                  ? activeOverlayMode === 'PIN'
                    ? 'Incorrect PIN. Try again.'
                    : 'Incorrect pattern. Try again.'
                  : activeOverlayMode === 'PIN'
                    ? `Enter PIN (${masterPin}) or use Fingerprint`
                    : `Draw Pattern (${savedPattern.map((x) => x + 1).join(' → ')}) or use Fingerprint`}
              </p>

              <div className="flex justify-center gap-1.5 mt-3">
                {(['PATTERN', 'PIN'] as const).map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => {
                      setActiveOverlayMode(m);
                      setAuthError(false);
                      setEnteredPattern([]);
                      setEnteredPin('');
                    }}
                    className="px-3 py-1 rounded-xl text-[11px] font-semibold transition-all border border-white/15"
                    style={
                      activeOverlayMode === m
                        ? {
                            backgroundColor: activeTheme.accent,
                            color: '#FFFFFF',
                            boxShadow: `0 4px 12px ${activeTheme.accent}55`,
                          }
                        : {
                            backgroundColor: `${activeTheme.surface}B8`,
                            color: activeTheme.fg,
                          }
                    }
                  >
                    {m === 'PATTERN' ? 'Pattern' : 'PIN'}
                  </button>
                ))}
                {biometricEnabled && (
                  <button
                    type="button"
                    onClick={() => {
                      setEnteredPattern([]);
                      setEnteredPin('');
                      setAuthError(false);
                      setPatternOverlayTarget(null);
                      setLaunchedTargetApp(null);
                    }}
                    className="px-3 py-1 rounded-xl text-[11px] font-semibold inline-flex items-center gap-1 border border-white/15"
                    style={{ backgroundColor: `${activeTheme.secondary}26`, color: activeTheme.secondary }}
                  >
                    <Fingerprint className="w-3.5 h-3.5" />
                    <span>Fingerprint</span>
                  </button>
                )}
              </div>
            </div>

            {activeOverlayMode === 'PATTERN' ? (
              <div className="my-3 text-center">
                <div
                  className="grid grid-cols-3 gap-4 max-w-[196px] mx-auto p-4 rounded-3xl backdrop-blur-lg border border-white/15"
                  style={{
                    background: `linear-gradient(145deg, ${activeTheme.surface}99, ${activeTheme.bg}88)`,
                    boxShadow:
                      'inset 4px 4px 10px rgba(0,0,0,0.45), inset -3px -3px 8px rgba(255,255,255,0.08)',
                  }}
                >
                  {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((node) => {
                    const activeIdx = enteredPattern.indexOf(node);
                    return (
                      <button
                        key={node}
                        type="button"
                        onClick={() => handlePatternTap(node)}
                        className="w-11 h-11 rounded-full flex items-center justify-center text-xs font-bold neuglass-orb"
                        style={
                          activeIdx >= 0
                            ? {
                                backgroundColor: activeTheme.secondary,
                                color: '#0A1628',
                                border: '1px solid rgba(255,255,255,0.75)',
                                boxShadow: `0 0 14px ${activeTheme.secondary}88`,
                              }
                            : {
                                background:
                                  'linear-gradient(145deg, rgba(255,255,255,0.16), rgba(255,255,255,0.05))',
                                color: activeTheme.fg,
                                border: '1px solid rgba(255,255,255,0.2)',
                                boxShadow:
                                  '-3px -3px 7px rgba(255,255,255,0.07), 4px 5px 10px rgba(0,0,0,0.42)',
                              }
                        }
                      >
                        {activeIdx >= 0 ? activeIdx + 1 : node + 1}
                      </button>
                    );
                  })}
                </div>
                <div className="flex justify-center gap-2 mt-4">
                  <button
                    type="button"
                    onClick={() => setEnteredPattern([])}
                    className="px-3.5 py-1.5 rounded-xl bg-white/10 border border-white/15 text-xs"
                  >
                    Clear
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      const matches =
                        enteredPattern.length >= 4 &&
                        enteredPattern.length === savedPattern.length &&
                        enteredPattern.every((v, i) => v === savedPattern[i]);
                      if (matches) {
                        setEnteredPattern([]);
                        setAuthError(false);
                        setPatternOverlayTarget(null);
                        setLaunchedTargetApp(null);
                      } else {
                        setEnteredPattern([]);
                        setAuthError(true);
                      }
                    }}
                    className="px-4 py-1.5 rounded-xl text-white text-xs font-semibold shadow-lg"
                    style={{ backgroundColor: activeTheme.accent }}
                  >
                    Verify Pattern
                  </button>
                </div>
              </div>
            ) : (
              <div className="my-3 text-center space-y-4">
                <div
                  className="inline-flex justify-center gap-2.5 px-4 py-2 rounded-full border border-white/15"
                  style={{
                    backgroundColor: 'rgba(0,0,0,0.28)',
                    boxShadow: 'inset 2px 2px 6px rgba(0,0,0,0.45)',
                  }}
                >
                  {Array.from({ length: Math.max(masterPin.length, 4) }).map((_, i) => (
                    <span
                      key={i}
                      className="w-3 h-3 rounded-full transition-all"
                      style={{
                        backgroundColor:
                          i < enteredPin.length ? activeTheme.secondary : 'rgba(255,255,255,0.22)',
                        boxShadow:
                          i < enteredPin.length ? `0 0 8px ${activeTheme.secondary}` : 'none',
                      }}
                    />
                  ))}
                </div>
                <div className="grid grid-cols-3 gap-3 max-w-[220px] mx-auto">
                  {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => (
                    <button
                      key={d}
                      type="button"
                      onClick={() => {
                        const next = enteredPin + d;
                        setEnteredPin(next);
                        setAuthError(false);
                        if (next === masterPin) {
                          setEnteredPin('');
                          setPatternOverlayTarget(null);
                          setLaunchedTargetApp(null);
                        } else if (next.length >= Math.max(masterPin.length, 4)) {
                          setEnteredPin('');
                          setAuthError(true);
                        }
                      }}
                      className="h-12 rounded-full text-sm font-semibold neuglass-orb"
                      style={{
                        background:
                          'linear-gradient(145deg, rgba(255,255,255,0.16), rgba(255,255,255,0.05))',
                        border: '1px solid rgba(255,255,255,0.22)',
                        boxShadow:
                          '-3px -3px 8px rgba(255,255,255,0.08), 5px 6px 12px rgba(0,0,0,0.45)',
                      }}
                    >
                      {d}
                    </button>
                  ))}
                  {biometricEnabled ? (
                    <button
                      type="button"
                      onClick={() => {
                        setEnteredPin('');
                        setAuthError(false);
                        setPatternOverlayTarget(null);
                        setLaunchedTargetApp(null);
                      }}
                      className="h-12 rounded-full flex items-center justify-center neuglass-orb"
                      style={{
                        background: `linear-gradient(145deg, ${activeTheme.accent}66, ${activeTheme.accent}33)`,
                        color: activeTheme.secondary,
                        border: `1px solid ${activeTheme.secondary}55`,
                        boxShadow:
                          '-3px -3px 8px rgba(255,255,255,0.08), 5px 6px 12px rgba(0,0,0,0.45)',
                      }}
                      aria-label="Fingerprint unlock"
                    >
                      <Fingerprint className="w-5 h-5" />
                    </button>
                  ) : (
                    <div />
                  )}
                  <button
                    type="button"
                    onClick={() => {
                      const next = enteredPin + '0';
                      setEnteredPin(next);
                      setAuthError(false);
                      if (next === masterPin) {
                        setEnteredPin('');
                        setPatternOverlayTarget(null);
                        setLaunchedTargetApp(null);
                      } else if (next.length >= Math.max(masterPin.length, 4)) {
                        setEnteredPin('');
                        setAuthError(true);
                      }
                    }}
                    className="h-12 rounded-full text-sm font-semibold neuglass-orb"
                    style={{
                      background:
                        'linear-gradient(145deg, rgba(255,255,255,0.16), rgba(255,255,255,0.05))',
                      border: '1px solid rgba(255,255,255,0.22)',
                      boxShadow:
                        '-3px -3px 8px rgba(255,255,255,0.08), 5px 6px 12px rgba(0,0,0,0.45)',
                    }}
                  >
                    0
                  </button>
                  <button
                    type="button"
                    onClick={() => setEnteredPin(enteredPin.slice(0, -1))}
                    className="h-12 rounded-full flex items-center justify-center neuglass-orb"
                    style={{
                      background:
                        'linear-gradient(145deg, rgba(255,255,255,0.14), rgba(255,255,255,0.04))',
                      border: '1px solid rgba(255,255,255,0.18)',
                      boxShadow:
                        '-3px -3px 8px rgba(255,255,255,0.07), 5px 6px 12px rgba(0,0,0,0.42)',
                    }}
                    aria-label="Backspace"
                  >
                    <Delete className="w-4 h-4" />
                  </button>
                </div>
              </div>
            )}

            <button
              type="button"
              onClick={() => {
                setEnteredPattern([]);
                setEnteredPin('');
                setAuthError(false);
                setPatternOverlayTarget(null);
                setLaunchedTargetApp(null);
              }}
              className="pb-4 text-xs font-medium"
              style={{ color: activeTheme.secondary }}
            >
              Return to Home Screen
            </button>
          </div>
        )}

        {/* Accessibility Setup Help Prompt Preview */}
        {showAccessibilityHelp && (
          <div className="absolute inset-0 z-50 bg-[#0A1628]/60 backdrop-blur-md p-4 flex items-center justify-center">
            <div className="w-full rounded-3xl neuglass-card text-[#0A1628] p-4 space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-bold">How to Enable Accessibility</h4>
                <button
                  type="button"
                  onClick={() => setShowAccessibilityHelp(false)}
                  className="text-slate-500 p-1"
                  aria-label="Close accessibility help"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="rounded-2xl neuglass-inset p-3 text-[11px] space-y-1.5 text-slate-700">
                <div>1. Tap <strong>Open Accessibility Directly</strong> below — supported phones jump straight to <strong>SecureLock App Protection Monitor</strong>.</div>
                <div>2. Or open <strong>Downloaded apps / Installed services</strong>.</div>
                <div>3. Tap <strong>SecureLock App Protection Monitor</strong> and turn the switch <strong>ON</strong>.</div>
              </div>
              <div className="rounded-2xl bg-amber-50/90 backdrop-blur border border-amber-200 p-3 text-[11px] space-y-1 text-amber-950">
                <div className="font-bold">Toggle greyed out / Restricted setting?</div>
                <div>On Android 13–16 APK installs: tap <strong>Open App Info</strong> &rarr; top-right <strong>⋮ menu</strong> &rarr; <strong>Allow restricted settings</strong> &rarr; confirm PIN/Fingerprint.</div>
              </div>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => {
                    setSimAccessibilityEnabled(true);
                    setShowAccessibilityHelp(false);
                  }}
                  className="flex-1 py-2 rounded-xl text-xs font-semibold text-white shadow-md"
                  style={{ backgroundColor: activeTheme.accent }}
                >
                  Open Accessibility Directly
                </button>
                <button
                  type="button"
                  onClick={() => setShowAccessibilityHelp(false)}
                  className="px-3 py-2 rounded-xl neuglass-pill text-[#0A1628] text-xs font-semibold"
                >
                  Done
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Android Gesture Bar */}
        <div className="py-2.5 flex justify-center shrink-0">
          <button
            type="button"
            onClick={() =>
              onScreenChange(activeScreen === 'calendar' ? activeScreen : 'dashboard')
            }
            className="w-28 h-1 rounded-full bg-white/35 hover:bg-white/60 transition-colors"
            aria-label="Return to Dashboard"
          />
        </div>
      </div>
    </div>
  );
}
