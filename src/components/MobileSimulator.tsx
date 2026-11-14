import { useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BarChart3,
  Camera,
  Check,
  Delete,
  Fingerprint,
  Globe,
  Lock,
  NotificationsOff,
  PauseCircle,
  Plus,
  RefreshCw,
  Search,
  Settings,
  Shield,
  ShieldAlert,
  Trash2,
  Unlock,
  UserCheck,
  VisibilityOff,
  X,
} from './SimulatorIcons';
import { SimulatorScreenId } from '../data/androidProject';

interface MobileSimulatorProps {
  activeScreen: SimulatorScreenId;
  onScreenChange: (screen: SimulatorScreenId) => void;
}

interface SimApp {
  packageName: string;
  name: string;
  category: string;
  locked: boolean;
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
  { packageName: 'com.whatsapp', name: 'WhatsApp', category: 'Messaging', locked: true },
  { packageName: 'com.google.android.gm', name: 'Gmail', category: 'Email', locked: true },
  { packageName: 'com.google.android.apps.photos', name: 'Google Photos', category: 'Media', locked: true },
  { packageName: 'com.android.settings', name: 'System Settings', category: 'System', locked: true },
  { packageName: 'com.chase.sig.android', name: 'Banking Vault', category: 'Finance', locked: false },
  { packageName: 'org.mozilla.firefox', name: 'Firefox', category: 'Browser', locked: false },
];

export function MobileSimulator({ activeScreen, onScreenChange }: MobileSimulatorProps) {
  const [masterPin, setMasterPin] = useState('2580');
  const [savedPattern, setSavedPattern] = useState<number[]>([0, 1, 4, 7]);
  const [unlockMethod, setUnlockMethod] = useState<'PIN' | 'PATTERN'>('PIN');
  const [enteredPin, setEnteredPin] = useState('');
  const [enteredPattern, setEnteredPattern] = useState<number[]>([]);
  const [authStatus, setAuthStatus] = useState('Verify identity to unlock SecureLock AI');
  const [authError, setAuthError] = useState(false);
  const [consecutiveFails, setConsecutiveFails] = useState(0);
  const [showForegroundCapturePrompt, setShowForegroundCapturePrompt] = useState(false);

  const [biometricEnabled, setBiometricEnabled] = useState(true);
  const [relockTimeout, setRelockTimeout] = useState(30);
  const [protectSettings, setProtectSettings] = useState(true);
  const [foregroundPromptEnabled, setForegroundPromptEnabled] = useState(true);
  const [lockNotificationsEnabled, setLockNotificationsEnabled] = useState(true);
  const [themeLabel, setThemeLabel] = useState('Navy + White');
  const [languageLabel, setLanguageLabel] = useState('English');
  const [recoveryCode, setRecoveryCode] = useState('7K4M-9P2X-Q8WN');

  const [apps, setApps] = useState<SimApp[]>(INITIAL_APPS);
  const [appSearch, setAppSearch] = useState('');
  const [launchedTargetApp, setLaunchedTargetApp] = useState<string | null>(null);

  const [coverEnabled, setCoverEnabled] = useState(false);
  const [coverTitle, setCoverTitle] = useState('App Unavailable');
  const [coverMessage, setCoverMessage] = useState(
    'This application is protected by SecureLock AI. Authenticate to continue.'
  );
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
  const [patternEditorOpen, setPatternEditorOpen] = useState(false);
  const [newPatternDraft, setNewPatternDraft] = useState<number[]>([]);

  const lockedCount = useMemo(() => apps.filter((a) => a.locked).length, [apps]);

  const recordAttempt = (success: boolean, method: string, target = 'SecureLock AI') => {
    const nowTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
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
      setLaunchedTargetApp(null);
      onScreenChange('dashboard');
    } else {
      const nextFails = consecutiveFails + 1;
      setConsecutiveFails(nextFails);
      setAuthError(true);
      setAuthStatus(`${method} failed (${nextFails} failed attempt${nextFails > 1 ? 's' : ''})`);
      if (nextFails >= 3 && foregroundPromptEnabled) {
        setIntruders((prev) => [
          {
            id: `int-${Date.now()}`,
            time: `Today · ${nowTime}`,
            attempts: nextFails,
            target,
            hasPhoto: false,
          },
          ...prev,
        ]);
        setShowForegroundCapturePrompt(true);
      }
    }
  };

  const handlePinDigit = (digit: string) => {
    if (enteredPin.length >= 6) return;
    const next = enteredPin + digit;
    setEnteredPin(next);
    setAuthError(false);
    if (next === masterPin) {
      recordAttempt(true, 'PIN', launchedTargetApp ?? 'SecureLock AI');
    } else if (next.length >= masterPin.length && next.length >= 4) {
      setEnteredPin('');
      recordAttempt(false, 'PIN', launchedTargetApp ?? 'SecureLock AI');
    }
  };

  const handlePatternTap = (node: number) => {
    if (enteredPattern.includes(node)) return;
    const next = [...enteredPattern, node];
    setEnteredPattern(next);
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

  const renderCoverScreen = (targetLabel: string, isPreview: boolean) => {
    const CoverIcon =
      coverIcon === 'lock'
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
          <div className="w-14 h-14 rounded-full bg-blue-50 text-blue-600 mx-auto flex items-center justify-center mb-4">
            <CoverIcon className="w-7 h-7" />
          </div>
          <h4 className="text-lg font-bold text-[#0A1628] mb-2">{coverTitle || 'App Unavailable'}</h4>
          <p className="text-xs text-slate-600 leading-relaxed mb-5">
            {coverMessage} ({targetLabel})
          </p>
          <button
            type="button"
            onClick={() => {
              setShowingCoverPreview(false);
              onScreenChange('unlock');
            }}
            className="w-full py-2.5 px-4 rounded-xl bg-blue-600 text-white text-xs font-semibold hover:bg-blue-700 transition-colors mb-2"
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
            className="w-full py-2 px-4 rounded-xl border border-slate-200 text-slate-700 text-xs font-medium hover:bg-slate-50 transition-colors"
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
    accentClass: string;
  }> = [
    {
      id: 'app_lock',
      title: 'App Lock',
      subtitle: 'Installed apps & auto-relock',
      metric: `${lockedCount} Locked`,
      icon: Lock,
      accentClass: 'text-blue-600 bg-blue-50',
    },
    {
      id: 'intruder_selfie',
      title: 'Intruder Selfie',
      subtitle: 'Foreground capture & log',
      metric: `${intruders.length} Events`,
      icon: Camera,
      accentClass: 'text-rose-600 bg-rose-50',
    },
    {
      id: 'lock_notifications',
      title: 'Lock Notifications',
      subtitle: 'Local privacy masking',
      metric: lockNotificationsEnabled ? `${maskedNotifs.length} Masked` : 'Paused',
      icon: NotificationsOff,
      accentClass: 'text-blue-600 bg-blue-50',
    },
    {
      id: 'fake_crash',
      title: 'Fake App Crash',
      subtitle: 'Neutral cover screen',
      metric: coverEnabled ? 'Active' : 'Off',
      icon: ShieldAlert,
      accentClass: 'text-amber-600 bg-amber-50',
    },
    {
      id: 'camera_monitor',
      title: 'Camera Privacy Monitor',
      subtitle: 'Sensors & permissions',
      metric: cameraGranted ? 'Ready' : 'Denied',
      icon: UserCheck,
      accentClass: 'text-emerald-600 bg-emerald-50',
    },
    {
      id: 'private_browser',
      title: 'Private Browser',
      subtitle: 'Zero-history WebView',
      metric: `${tabs.length} Tab`,
      icon: Globe,
      accentClass: 'text-blue-600 bg-blue-50',
    },
    {
      id: 'statistics',
      title: 'Statistics',
      subtitle: 'Weekly activity & logs',
      metric: `${stats.total} Tries`,
      icon: BarChart3,
      accentClass: 'text-sky-600 bg-sky-50',
    },
  ];

  const filteredApps = apps.filter(
    (a) =>
      a.name.toLowerCase().includes(appSearch.toLowerCase()) ||
      a.packageName.toLowerCase().includes(appSearch.toLowerCase())
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
      <div className="bg-[#0A1628] rounded-[34px] overflow-hidden h-[730px] flex flex-col relative text-white">
        {/* Android Status Bar */}
        <div className="px-6 pt-3 pb-2 flex items-center justify-between text-[11px] font-medium text-slate-300 shrink-0">
          <span className="tabular-nums">10:01</span>
          <div className="flex items-center gap-2">
            {cameraTestActive && (
              <span className="inline-flex items-center gap-1 text-[10px] text-emerald-400 font-semibold">
                <span className="w-2 h-2 rounded-full bg-emerald-500" />
                CAM
              </span>
            )}
            <span>5G</span>
            <span className="tabular-nums">94%</span>
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
                  className="w-20 h-20 rounded-full bg-[#112240] border border-sky-400/30 mx-auto flex items-center justify-center hover:bg-[#162B4D] transition-colors"
                >
                  <Fingerprint className="w-11 h-11 text-sky-400" />
                </button>
                <h3 className="mt-3 text-lg font-bold text-white">
                  {launchedTargetApp ? `Unlock ${launchedTargetApp}` : 'SecureLock AI'}
                </h3>
                <p
                  className={`mt-1 text-xs ${
                    authError ? 'text-rose-400' : 'text-slate-300'
                  }`}
                >
                  {authStatus}
                </p>

                <div className="mt-3 inline-flex bg-[#112240] p-1 rounded-xl gap-1">
                  {(['PIN', 'PATTERN'] as const).map((m) => (
                    <button
                      key={m}
                      type="button"
                      onClick={() => {
                        setUnlockMethod(m);
                        setAuthError(false);
                      }}
                      className={`px-3 py-1 rounded-lg text-xs font-medium transition-colors ${
                        unlockMethod === m ? 'bg-blue-600 text-white' : 'text-slate-300'
                      }`}
                    >
                      {m === 'PIN' ? 'PIN (2580)' : 'Pattern'}
                    </button>
                  ))}
                </div>
              </div>

              {unlockMethod === 'PIN' ? (
                <div className="my-4">
                  <div className="flex justify-center gap-2.5 mb-5">
                    {[0, 1, 2, 3].map((idx) => (
                      <span
                        key={idx}
                        className={`w-3 h-3 rounded-full transition-colors ${
                          idx < enteredPin.length ? 'bg-sky-400' : 'bg-white/20'
                        }`}
                      />
                    ))}
                  </div>
                  <div className="grid grid-cols-3 gap-3 max-w-[232px] mx-auto">
                    {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map((d) => (
                      <button
                        key={d}
                        type="button"
                        onClick={() => handlePinDigit(d)}
                        className="h-14 rounded-full bg-white/10 hover:bg-white/20 text-lg font-semibold transition-colors"
                      >
                        {d}
                      </button>
                    ))}
                    <button
                      type="button"
                      onClick={() => biometricEnabled && recordAttempt(true, 'Biometric', launchedTargetApp ?? 'SecureLock AI')}
                      disabled={!biometricEnabled}
                      className="h-14 rounded-full bg-blue-600/25 hover:bg-blue-600/40 disabled:opacity-40 flex items-center justify-center"
                      aria-label="BiometricPrompt unlock"
                    >
                      <Fingerprint className="w-6 h-6 text-sky-400" />
                    </button>
                    <button
                      type="button"
                      onClick={() => handlePinDigit('0')}
                      className="h-14 rounded-full bg-white/10 hover:bg-white/20 text-lg font-semibold transition-colors"
                    >
                      0
                    </button>
                    <button
                      type="button"
                      onClick={() => setEnteredPin((p) => p.slice(0, -1))}
                      className="h-14 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center"
                      aria-label="Backspace"
                    >
                      <Delete className="w-5 h-5" />
                    </button>
                  </div>
                </div>
              ) : (
                <div className="my-4 text-center">
                  <p className="text-[11px] text-slate-400 mb-2">
                    Tap nodes in order (default: 1 → 2 → 5 → 8)
                  </p>
                  <div className="grid grid-cols-3 gap-4 max-w-[180px] mx-auto bg-[#112240] p-4 rounded-2xl">
                    {[0, 1, 2, 3, 4, 5, 6, 7, 8].map((node) => {
                      const activeIdx = enteredPattern.indexOf(node);
                      return (
                        <button
                          key={node}
                          type="button"
                          onClick={() => handlePatternTap(node)}
                          className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold transition-colors ${
                            activeIdx >= 0
                              ? 'bg-sky-400 text-[#0A1628]'
                              : 'bg-white/15 text-slate-300 hover:bg-white/25'
                          }`}
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
                      className="px-4 py-1.5 rounded-lg bg-blue-600 text-xs font-semibold"
                    >
                      Verify Pattern
                    </button>
                  </div>
                </div>
              )}

              <div className="flex flex-col gap-2">
                <button
                  type="button"
                  onClick={() => recordAttempt(false, unlockMethod, launchedTargetApp ?? 'SecureLock AI')}
                  className="w-full py-2 rounded-xl bg-rose-500/20 text-rose-300 text-xs font-medium hover:bg-rose-500/30"
                >
                  Simulate Failed Attempt (Triggers Intruder Prompt at 3)
                </button>
                {showForegroundCapturePrompt && (
                  <div className="bg-white text-[#0A1628] rounded-2xl p-3 text-left">
                    <div className="text-xs font-bold mb-1">Foreground Camera Prompt</div>
                    <p className="text-[11px] text-slate-600 mb-2">
                      3 failed tries detected. Capture a visible foreground snapshot?
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
                        className="px-3 py-1.5 rounded-lg bg-blue-600 text-white text-[11px] font-semibold"
                      >
                        Capture Snapshot
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
                  <h3 className="text-base font-bold text-white">SecureLock AI</h3>
                  <p className="text-[11px] text-sky-400">Offline Privacy &amp; App Vault</p>
                </div>
                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => onScreenChange('permissions')}
                    className="p-2 rounded-xl bg-[#112240] hover:bg-blue-600/30 text-sky-300"
                    aria-label="Permissions Manager"
                  >
                    <Shield className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onScreenChange('settings')}
                    className="p-2 rounded-xl bg-[#112240] hover:bg-blue-600/30 text-sky-300"
                    aria-label="Settings"
                  >
                    <Settings className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="bg-[#112240] rounded-2xl p-3.5 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-blue-600/20 flex items-center justify-center text-sky-400">
                    <Shield className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-white">Keystore AES-256-GCM</div>
                    <div className="text-[11px] text-sky-300 tabular-nums">
                      {lockedCount} apps locked · {stats.failed} failed tries
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onScreenChange('unlock')}
                  className="text-xs font-semibold text-sky-400 hover:text-sky-300"
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
                      className="bg-white text-[#0A1628] rounded-2xl p-3.5 text-left hover:bg-slate-50 transition-colors flex flex-col justify-between min-h-[116px]"
                    >
                      <div className="flex items-center justify-between w-full mb-2">
                        <span className={`w-8 h-8 rounded-xl flex items-center justify-center ${mod.accentClass}`}>
                          <Icon className="w-4 h-4" />
                        </span>
                        <span className="text-[10px] font-semibold text-blue-600 tabular-nums">
                          {mod.metric}
                        </span>
                      </div>
                      <div>
                        <div className="text-xs font-bold leading-snug">{mod.title}</div>
                        <div className="text-[11px] text-slate-500 mt-0.5 leading-tight">{mod.subtitle}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="flex flex-col h-full">
              {/* Inner Top App Bar */}
              <div className="px-4 py-3 bg-[#0A1628] border-b border-white/10 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-2.5">
                  <button
                    type="button"
                    onClick={() => onScreenChange('dashboard')}
                    className="p-1.5 rounded-lg hover:bg-white/10"
                    aria-label="Back to dashboard"
                  >
                    <ArrowLeft className="w-4 h-4 text-white" />
                  </button>
                  <div>
                    <div className="text-sm font-semibold text-white capitalize">
                      {activeScreen.replace('_', ' ')}
                    </div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => onScreenChange('unlock')}
                  className="text-[11px] font-medium text-sky-400"
                >
                  Lock Now
                </button>
              </div>

              {/* Inner Scrollable Content */}
              <div className="p-4 space-y-3 flex-1 overflow-y-auto">
                {activeScreen === 'app_lock' && (
                  <>
                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-xs font-bold">Protect System Settings</div>
                          <div className="text-[11px] text-slate-500">Auto-relock: {relockTimeout}s</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setProtectSettings(!protectSettings)}
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                            protectSettings ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {protectSettings ? 'On' : 'Off'}
                        </button>
                      </div>
                      <div className="flex gap-1.5">
                        {[0, 30, 120, 300].map((sec) => (
                          <button
                            key={sec}
                            type="button"
                            onClick={() => setRelockTimeout(sec)}
                            className={`px-2.5 py-1 rounded-lg text-[11px] font-medium ${
                              relockTimeout === sec
                                ? 'bg-[#0A1628] text-white'
                                : 'bg-slate-100 text-slate-700'
                            }`}
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
                        className="w-full pl-8 pr-3 py-2 rounded-xl bg-white text-[#0A1628] text-xs focus:outline-none"
                      />
                    </div>

                    {filteredApps.map((app) => (
                      <div
                        key={app.packageName}
                        className="bg-white text-[#0A1628] rounded-2xl p-3 flex items-center justify-between gap-2"
                      >
                        <div className="min-w-0">
                          <div className="text-xs font-bold truncate">{app.name}</div>
                          <div className="text-[10px] text-slate-500 font-mono truncate">
                            {app.packageName}
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          {app.locked && (
                            <button
                              type="button"
                              onClick={() => {
                                setLaunchedTargetApp(app.name);
                                if (coverEnabled) {
                                  setShowingCoverPreview(true);
                                } else {
                                  onScreenChange('unlock');
                                }
                              }}
                              className="px-2 py-1 rounded-lg bg-slate-100 text-[10px] font-medium text-slate-700 hover:bg-slate-200"
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
                            className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                              app.locked ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-700'
                            }`}
                          >
                            {app.locked ? 'Locked' : 'Open'}
                          </button>
                        </div>
                      </div>
                    ))}
                  </>
                )}

                {activeScreen === 'intruder_selfie' && (
                  <>
                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-bold">Foreground Capture Prompt</div>
                        <button
                          type="button"
                          onClick={() => setForegroundPromptEnabled(!foregroundPromptEnabled)}
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                            foregroundPromptEnabled
                              ? 'bg-blue-600 text-white'
                              : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {foregroundPromptEnabled ? 'On' : 'Off'}
                        </button>
                      </div>
                      <p className="text-[11px] text-slate-600">
                        Captures only in visible foreground with camera permission. Saved privately in filesDir/intruders.
                      </p>
                      <button
                        type="button"
                        onClick={() =>
                          setIntruders((prev) => [
                            {
                              id: `int-${Date.now()}`,
                              time: `Today · ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`,
                              attempts: 1,
                              target: 'Foreground Self-Test',
                              hasPhoto: true,
                            },
                            ...prev,
                          ])
                        }
                        className="w-full py-2 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                      >
                        Capture Foreground Snapshot
                      </button>
                    </div>

                    {intruders.map((item) => (
                      <div
                        key={item.id}
                        className="bg-white text-[#0A1628] rounded-2xl p-3.5 flex items-center justify-between"
                      >
                        <div>
                          <div className="text-xs font-bold text-rose-600">
                            {item.attempts} Failed Attempt{item.attempts > 1 ? 's' : ''}
                          </div>
                          <div className="text-[11px] text-slate-500">{item.time}</div>
                          <div className="text-[11px] text-slate-600 mt-0.5">
                            {item.target} · {item.hasPhoto ? 'Private Photo Saved' : 'Log Only'}
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setIntruders((prev) => prev.filter((x) => x.id !== item.id))}
                          className="p-2 text-rose-600 hover:bg-rose-50 rounded-lg"
                          aria-label="Delete intruder record"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    ))}
                  </>
                )}

                {activeScreen === 'lock_notifications' && (
                  <>
                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="text-xs font-bold">Notification Privacy Vault</div>
                        <button
                          type="button"
                          onClick={() => setLockNotificationsEnabled(!lockNotificationsEnabled)}
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                            lockNotificationsEnabled
                              ? 'bg-blue-600 text-white'
                              : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {lockNotificationsEnabled ? 'Active' : 'Paused'}
                        </button>
                      </div>
                      <p className="text-[11px] text-slate-600">
                        Uses optional NotificationListenerService locally. Zero notification text ever leaves the device.
                      </p>
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
                        className="w-full py-2 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                      >
                        Simulate Protected Notification
                      </button>
                    </div>

                    {maskedNotifs.map((n) => (
                      <div key={n.id} className="bg-white text-[#0A1628] rounded-2xl p-3.5">
                        <div className="flex justify-between text-xs font-bold">
                          <span>{n.app}</span>
                          <span className="text-slate-400 font-normal">{n.time}</span>
                        </div>
                        <div className="text-xs font-semibold text-blue-600 mt-1">{n.maskedTitle}</div>
                        <div className="text-[11px] text-slate-500">{n.maskedBody}</div>
                      </div>
                    ))}
                  </>
                )}

                {activeScreen === 'fake_crash' && (
                  <div className="bg-white text-[#0A1628] rounded-2xl p-4 space-y-3">
                    <div className="flex items-center justify-between">
                      <div>
                        <div className="text-xs font-bold">Enable Protection Cover</div>
                        <div className="text-[11px] text-slate-500">Neutral App Unavailable gate</div>
                      </div>
                      <button
                        type="button"
                        onClick={() => setCoverEnabled(!coverEnabled)}
                        className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                          coverEnabled ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-700'
                        }`}
                      >
                        {coverEnabled ? 'Enabled' : 'Off'}
                      </button>
                    </div>

                    <div>
                      <label className="text-[11px] font-semibold text-slate-600 block mb-1">Heading</label>
                      <input
                        type="text"
                        value={coverTitle}
                        onChange={(e) => setCoverTitle(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs"
                      />
                    </div>
                    <div>
                      <label className="text-[11px] font-semibold text-slate-600 block mb-1">Neutral Message</label>
                      <textarea
                        rows={2}
                        value={coverMessage}
                        onChange={(e) => setCoverMessage(e.target.value)}
                        className="w-full px-3 py-2 rounded-xl border border-slate-200 text-xs"
                      />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-semibold text-slate-600">Icon</span>
                      <div className="flex gap-1">
                        {(['shield', 'lock', 'hidden', 'pause'] as const).map((ic) => (
                          <button
                            key={ic}
                            type="button"
                            onClick={() => setCoverIcon(ic)}
                            className={`px-2 py-1 rounded-lg text-[10px] capitalize ${
                              coverIcon === ic ? 'bg-[#0A1628] text-white' : 'bg-slate-100 text-slate-700'
                            }`}
                          >
                            {ic}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-[11px] font-semibold text-slate-600">Backdrop</span>
                      <div className="flex gap-2">
                        {['#0A1628', '#112240', '#1E293B', '#0F172A'].map((hex) => (
                          <button
                            key={hex}
                            type="button"
                            onClick={() => setCoverBg(hex)}
                            className={`w-6 h-6 rounded-full border-2 ${
                              coverBg === hex ? 'border-blue-500' : 'border-transparent'
                            }`}
                            style={{ backgroundColor: hex }}
                          />
                        ))}
                      </div>
                    </div>

                    <button
                      type="button"
                      onClick={() => setShowingCoverPreview(true)}
                      className="w-full py-2.5 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                    >
                      Preview App Protection Screen
                    </button>
                  </div>
                )}

                {activeScreen === 'camera_monitor' && (
                  <>
                    <div className="bg-white text-[#0A1628] rounded-2xl p-4 space-y-2.5">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold">CAMERA Permission</span>
                        <span className="text-xs font-semibold text-emerald-600">
                          {cameraGranted ? 'Granted (Foreground Only)' : 'Denied'}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-600">
                        Monitors sensor availability via CameraManager.AvailabilityCallback and respects Android 12–16 hardware privacy indicators.
                      </p>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => setCameraGranted(!cameraGranted)}
                          className="px-3 py-1.5 rounded-xl bg-slate-100 text-xs font-medium"
                        >
                          {cameraGranted ? 'Revoke Test' : 'Grant Camera'}
                        </button>
                        {cameraGranted && (
                          <button
                            type="button"
                            onClick={() => setCameraTestActive(!cameraTestActive)}
                            className="px-3 py-1.5 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                          >
                            {cameraTestActive ? 'Stop Camera Test' : 'Run Camera Test'}
                          </button>
                        )}
                      </div>
                    </div>

                    {cameraTestActive && cameraGranted && (
                      <div className="bg-white text-[#0A1628] rounded-2xl p-3.5">
                        <div className="text-xs font-bold mb-1">Foreground CameraX Preview Active</div>
                        <div className="h-28 rounded-xl bg-[#0A1628] flex flex-col items-center justify-center text-sky-300">
                          <Camera className="w-7 h-7 mb-1" />
                          <span className="text-[11px]">Hardware indicator visible in status bar</span>
                        </div>
                      </div>
                    )}

                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-1.5">
                      <div className="flex justify-between text-xs">
                        <span className="font-semibold">Camera Sensor #0 (Rear)</span>
                        <span className="text-emerald-600 font-medium">Idle &amp; Available</span>
                      </div>
                      <div className="flex justify-between text-xs">
                        <span className="font-semibold">Camera Sensor #1 (Front)</span>
                        <span className={cameraTestActive ? 'text-amber-600 font-medium' : 'text-emerald-600 font-medium'}>
                          {cameraTestActive ? 'In Use (Foreground Test)' : 'Idle & Available'}
                        </span>
                      </div>
                    </div>
                  </>
                )}

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
                        className="px-3 py-2 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                      >
                        Go
                      </button>
                    </div>

                    <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
                      {tabs.map((t, idx) => (
                        <div
                          key={t.id}
                          className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-[11px] shrink-0 ${
                            idx === activeTabIdx ? 'bg-blue-600 text-white' : 'bg-[#112240] text-slate-300'
                          }`}
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
                        className="p-1.5 rounded-lg bg-[#112240] text-sky-400"
                        aria-label="New tab"
                      >
                        <Plus className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    {httpWarningUrl && (
                      <div className="bg-amber-50 text-[#0A1628] border border-amber-200 rounded-2xl p-3 text-xs">
                        <div className="font-bold text-amber-800 mb-1">Safe Browsing Warning</div>
                        <p className="text-[11px] text-amber-900 mb-2">
                          Unencrypted HTTP blocked: {httpWarningUrl}
                        </p>
                        <button
                          type="button"
                          onClick={() => setHttpWarningUrl(null)}
                          className="px-3 py-1 rounded-lg bg-amber-600 text-white text-[11px] font-semibold"
                        >
                          Dismiss
                        </button>
                      </div>
                    )}

                    <div className="bg-white text-[#0A1628] rounded-2xl p-4 min-h-[240px] flex flex-col justify-between">
                      <div>
                        <div className="text-[11px] text-emerald-600 font-semibold mb-1">
                          Incognito WebView · Cookies Off · No Cache
                        </div>
                        <div className="text-sm font-bold break-all">{currentTab.url}</div>
                        <p className="text-xs text-slate-600 mt-2">
                          Try entering <span className="font-mono">http://example.com</span> to test the Safe Browsing HTTP warning, or search any term.
                        </p>
                      </div>
                      <div className="flex items-center justify-between pt-4 border-t border-slate-100">
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
                            className="p-2 rounded-lg bg-slate-100 disabled:opacity-40"
                            aria-label="Back"
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
                            className="p-2 rounded-lg bg-slate-100 disabled:opacity-40"
                            aria-label="Forward"
                          >
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() => navigateBrowser(urlInput)}
                            className="p-2 rounded-lg bg-slate-100"
                            aria-label="Reload"
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
                          className="px-2.5 py-1.5 rounded-lg bg-rose-50 text-rose-600 text-[11px] font-semibold"
                        >
                          Wipe Session
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {activeScreen === 'statistics' && (
                  <>
                    <div className="grid grid-cols-2 gap-2.5">
                      <div className="bg-white text-[#0A1628] rounded-2xl p-3">
                        <div className="text-[11px] text-slate-500">Protected Apps</div>
                        <div className="text-xl font-bold text-blue-600 tabular-nums">{lockedCount}</div>
                      </div>
                      <div className="bg-white text-[#0A1628] rounded-2xl p-3">
                        <div className="text-[11px] text-slate-500">Total Attempts</div>
                        <div className="text-xl font-bold tabular-nums">{stats.total}</div>
                      </div>
                      <div className="bg-white text-[#0A1628] rounded-2xl p-3">
                        <div className="text-[11px] text-slate-500">Successful</div>
                        <div className="text-xl font-bold text-emerald-600 tabular-nums">{stats.success}</div>
                      </div>
                      <div className="bg-white text-[#0A1628] rounded-2xl p-3">
                        <div className="text-[11px] text-slate-500">Failed</div>
                        <div className="text-xl font-bold text-rose-600 tabular-nums">{stats.failed}</div>
                      </div>
                    </div>

                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5">
                      <div className="text-xs font-bold mb-2">Weekly Activity (Local Only)</div>
                      <div className="flex items-end justify-between h-24 gap-2 pt-2">
                        {stats.weekly.map((w) => {
                          const maxTotal = Math.max(...stats.weekly.map((d) => d.ok + d.fail), 1);
                          const okH = Math.round((w.ok / maxTotal) * 72);
                          const failH = Math.round((w.fail / maxTotal) * 72);
                          return (
                            <div key={w.day} className="flex-1 flex flex-col items-center gap-1">
                              <div className="w-4 flex flex-col justify-end h-[72px] gap-0.5">
                                {failH > 0 && (
                                  <div
                                    className="w-full bg-rose-500 rounded-sm"
                                    style={{ height: `${failH}px` }}
                                  />
                                )}
                                {okH > 0 && (
                                  <div
                                    className="w-full bg-blue-600 rounded-sm"
                                    style={{ height: `${okH}px` }}
                                  />
                                )}
                              </div>
                              <span className="text-[10px] text-slate-500">{w.day}</span>
                            </div>
                          );
                        })}
                      </div>
                    </div>

                    <div className="space-y-2">
                      {events.slice(0, 5).map((ev) => (
                        <div key={ev.id} className="bg-white text-[#0A1628] rounded-2xl p-3">
                          <div className="flex justify-between text-xs font-semibold">
                            <span className={ev.success ? 'text-[#0A1628]' : 'text-rose-600'}>
                              {ev.title}
                            </span>
                            <span className="text-[10px] text-slate-400 tabular-nums">{ev.time}</span>
                          </div>
                          <div className="text-[11px] text-slate-500">{ev.detail}</div>
                        </div>
                      ))}
                    </div>
                  </>
                )}

                {activeScreen === 'settings' && (
                  <div className="space-y-2.5">
                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="text-xs font-bold">Credentials &amp; Biometrics</div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => setPinEditorOpen(!pinEditorOpen)}
                          className="px-3 py-1.5 rounded-xl bg-blue-600 text-white text-xs font-semibold"
                        >
                          Change PIN
                        </button>
                        <button
                          type="button"
                          onClick={() => setPatternEditorOpen(!patternEditorOpen)}
                          className="px-3 py-1.5 rounded-xl bg-slate-100 text-xs font-medium"
                        >
                          Change Pattern
                        </button>
                      </div>
                      {pinEditorOpen && (
                        <div className="flex gap-2 pt-1">
                          <input
                            type="text"
                            maxLength={6}
                            value={newPinDraft}
                            onChange={(e) => setNewPinDraft(e.target.value.replace(/\D/g, ''))}
                            placeholder="New 4-6 digit PIN"
                            className="flex-1 px-2.5 py-1.5 rounded-lg border border-slate-200 text-xs"
                          />
                          <button
                            type="button"
                            onClick={() => {
                              if (newPinDraft.length >= 4) {
                                setMasterPin(newPinDraft);
                                setNewPinDraft('');
                                setPinEditorOpen(false);
                              }
                            }}
                            className="px-3 py-1.5 rounded-lg bg-[#0A1628] text-white text-xs"
                          >
                            Save
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
                                className={`h-8 rounded-lg text-xs font-semibold ${
                                  newPatternDraft.includes(n)
                                    ? 'bg-blue-600 text-white'
                                    : 'bg-slate-100 text-slate-700'
                                }`}
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
                            className="px-3 py-1 rounded-lg bg-[#0A1628] text-white text-xs"
                          >
                            Save Pattern ({newPatternDraft.length}/4+)
                          </button>
                        </div>
                      )}
                      <div className="flex items-center justify-between pt-1">
                        <span className="text-xs">Fingerprint BiometricPrompt</span>
                        <button
                          type="button"
                          onClick={() => setBiometricEnabled(!biometricEnabled)}
                          className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold ${
                            biometricEnabled ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-700'
                          }`}
                        >
                          {biometricEnabled ? 'On' : 'Off'}
                        </button>
                      </div>
                    </div>

                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="text-xs font-bold">Theme &amp; Language</div>
                      <div className="flex flex-wrap gap-1.5">
                        {['Navy + White', 'Navy Dark', 'System'].map((t) => (
                          <button
                            key={t}
                            type="button"
                            onClick={() => setThemeLabel(t)}
                            className={`px-2.5 py-1 rounded-lg text-[11px] ${
                              themeLabel === t ? 'bg-[#0A1628] text-white' : 'bg-slate-100 text-slate-700'
                            }`}
                          >
                            {t}
                          </button>
                        ))}
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {['English', 'Español', 'Deutsch'].map((lang) => (
                          <button
                            key={lang}
                            type="button"
                            onClick={() => setLanguageLabel(lang)}
                            className={`px-2.5 py-1 rounded-lg text-[11px] ${
                              languageLabel === lang ? 'bg-blue-600 text-white' : 'bg-slate-100 text-slate-700'
                            }`}
                          >
                            {lang}
                          </button>
                        ))}
                      </div>
                    </div>

                    <div className="bg-white text-[#0A1628] rounded-2xl p-3.5 space-y-2">
                      <div className="text-xs font-bold">Emergency Recovery &amp; Local Backup</div>
                      <div className="text-[11px] font-mono bg-slate-100 px-2.5 py-1.5 rounded-lg text-[#0A1628]">
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
                          className="px-2.5 py-1.5 rounded-lg bg-slate-100 text-[11px] font-medium"
                        >
                          Rotate Key
                        </button>
                        <button
                          type="button"
                          onClick={() => onScreenChange('permissions')}
                          className="px-2.5 py-1.5 rounded-lg bg-blue-600 text-white text-[11px] font-semibold"
                        >
                          Permissions
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {activeScreen === 'permissions' && (
                  <div className="space-y-2.5">
                    {[
                      {
                        name: 'Display Over Other Apps',
                        api: 'SYSTEM_ALERT_WINDOW',
                        state: 'Granted',
                        ok: true,
                      },
                      {
                        name: 'Usage Access',
                        api: 'PACKAGE_USAGE_STATS',
                        state: 'Granted',
                        ok: true,
                      },
                      {
                        name: 'Accessibility Service',
                        api: 'Optional Alternative',
                        state: 'Optional',
                        ok: false,
                      },
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
                      <div key={p.name} className="bg-white text-[#0A1628] rounded-2xl p-3.5">
                        <div className="flex justify-between items-center">
                          <span className="text-xs font-bold">{p.name}</span>
                          <span
                            className={`text-[11px] font-semibold ${
                              p.ok ? 'text-emerald-600' : 'text-amber-600'
                            }`}
                          >
                            {p.state}
                          </span>
                        </div>
                        <div className="text-[10px] font-mono text-slate-500 mt-0.5">{p.api}</div>
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
