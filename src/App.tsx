import { useState } from 'react';
import { Code2, Copy, Check, Download, ExternalLink } from 'lucide-react';
import { MobileSimulator } from './components/MobileSimulator';
import { KotlinSourceExplorer } from './components/KotlinSourceExplorer';
import { PermissionsAndPolicyView, SetupAndVerificationView } from './components/PermissionsAndSetupViews';
import {
  ANDROID_PROJECT_FILES,
  downloadAndroidProjectZip,
  SCREEN_SPECS,
  SimulatorScreenId,
} from './data/androidProject';

type TopSection = 'preview' | 'source' | 'permissions' | 'setup';

const SIMULATOR_NAV_LIST: Array<{ id: SimulatorScreenId; label: string }> = [
  { id: 'calendar', label: 'Calendar' },
  { id: 'dashboard', label: '9-Card Dashboard' },
  { id: 'app_lock', label: 'App Lock' },
  { id: 'vault', label: 'Secret Vault' },
  { id: 'intruder_selfie', label: 'Intruder Detection' },
  { id: 'lock_notifications', label: 'Lock Notifications' },
  { id: 'fake_crash', label: 'Fake Crash' },
  { id: 'camera_monitor', label: 'Camera Privacy' },
  { id: 'private_browser', label: 'Private Browser' },
  { id: 'statistics', label: 'Statistics' },
  { id: 'theme_customization', label: 'Themes (8)' },
  { id: 'settings', label: 'Settings' },
  { id: 'permissions', label: 'Permissions' },
];

export default function App() {
  const [section, setSection] = useState<TopSection>('preview');
  const [activeScreen, setActiveScreen] = useState<SimulatorScreenId>('calendar');
  const [inspectorSourcePath, setInspectorSourcePath] = useState<string | undefined>(undefined);
  const [copiedCode, setCopiedCode] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const activeSpec = SCREEN_SPECS[activeScreen];
  const activeKotlinFile = ANDROID_PROJECT_FILES.find((f) => f.path === activeSpec.kotlinPath);

  const handleDownloadZip = async () => {
    setDownloading(true);
    try {
      await downloadAndroidProjectZip();
    } finally {
      setDownloading(false);
    }
  };

  const handleCopySnippet = async () => {
    if (!activeKotlinFile) return;
    await navigator.clipboard.writeText(activeKotlinFile.content);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 1800);
  };

  return (
    <div className="min-h-screen neuglass-bg text-[#0A1628] flex flex-col">
      {/* Three-Zone Top Bar Contract */}
      <header className="sticky top-0 z-40 neuglass-topbar">
        <div className="max-w-[1440px] mx-auto px-6 h-16 flex items-center justify-between gap-8">
          <button
            type="button"
            onClick={() => setSection('preview')}
            className="text-lg font-bold tracking-tight text-[#0A1628] whitespace-nowrap shrink-0"
          >
            SecureLock AI
          </button>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
            {(
              [
                ['preview', 'Interactive Preview'],
                ['source', 'Kotlin Source'],
                ['permissions', 'Permissions & Policy'],
                ['setup', 'Setup & Verification'],
              ] as Array<[TopSection, string]>
            ).map(([key, label]) => (
              <button
                key={key}
                type="button"
                onClick={() => setSection(key)}
                className={`py-1.5 px-3 rounded-xl whitespace-nowrap shrink-0 transition-all ${
                  section === key
                    ? 'neuglass-inset text-[#0A1628] font-semibold'
                    : 'text-slate-600 hover:text-[#0A1628]'
                }`}
              >
                {label}
              </button>
            ))}
          </nav>

          <div className="shrink-0">
            <button
              type="button"
              onClick={handleDownloadZip}
              disabled={downloading}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-br from-[#0A1628] to-[#1E3A8A] hover:from-[#1D4ED8] hover:to-[#0284C7] text-white text-xs font-semibold transition-all shadow-[4px_6px_16px_rgba(15,23,42,0.22),-3px_-3px_10px_rgba(255,255,255,0.75)] border border-white/20 whitespace-nowrap shrink-0 disabled:opacity-60"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{downloading ? 'Packaging ZIP...' : 'Download Android ZIP'}</span>
            </button>
          </div>
        </div>

        {/* Mobile navigation strip */}
        <div className="md:hidden border-t border-white/50 px-4 py-2 flex items-center gap-3 overflow-x-auto text-xs font-medium">
          {(
            [
              ['preview', 'Preview'],
              ['source', 'Kotlin Source'],
              ['permissions', 'Permissions'],
              ['setup', 'Setup'],
            ] as Array<[TopSection, string]>
          ).map(([key, label]) => (
            <button
              key={key}
              type="button"
              onClick={() => setSection(key)}
              className={`whitespace-nowrap shrink-0 px-3 py-1 rounded-lg ${
                section === key ? 'neuglass-inset text-[#0A1628] font-semibold' : 'text-slate-600'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </header>

      <main className="flex-1 max-w-[1440px] w-full mx-auto px-6 py-8">
        {section === 'preview' && (
          <div className="space-y-6">
            <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
              <div>
                <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-[#0A1628] text-balance">
                  App Lock &amp; Privacy Protector for Android 8.0–16
                </h1>
                <p className="text-sm text-slate-600 mt-1 max-w-2xl">
                  Native Kotlin, Jetpack Compose, Neuglass UI (Neumorphism + Glassmorphism), Keystore AES-256-GCM, and BiometricPrompt. Test every screen on the left or inspect the corresponding Kotlin implementation on the right.
                </p>
              </div>
              <div className="text-xs text-slate-600 flex items-center gap-2 whitespace-nowrap neuglass-inset px-3.5 py-2 rounded-xl">
                <span>Kotlin 2.1</span>
                <span aria-hidden="true">·</span>
                <span>minSdk 26 / targetSdk 36</span>
                <span aria-hidden="true">·</span>
                <span className="tabular-nums font-semibold text-[#0A1628]">
                  {ANDROID_PROJECT_FILES.length} Android Studio Files
                </span>
              </div>
            </div>

            {/* Screen Selector Segmented Strip */}
            <div className="flex items-center gap-2 overflow-x-auto py-2 px-1">
              {SIMULATOR_NAV_LIST.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveScreen(item.id)}
                  className={`px-3.5 py-1.5 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 transition-all ${
                    activeScreen === item.id
                      ? 'bg-gradient-to-br from-[#0A1628] to-[#1D4ED8] text-white shadow-[4px_6px_14px_rgba(37,99,235,0.28),-2px_-2px_8px_rgba(255,255,255,0.7)] border border-white/25'
                      : 'neuglass-pill text-slate-700'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>

            {/* Two-Column Workbench */}
            <div className="grid grid-cols-1 lg:grid-cols-[410px_1fr] gap-8 items-start">
              <div>
                <MobileSimulator
                  activeScreen={activeScreen}
                  onScreenChange={setActiveScreen}
                />
              </div>

              <div className="space-y-6">
                <div className="neuglass-card rounded-3xl p-6 space-y-4">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <h2 className="text-lg font-bold text-[#0A1628]">{activeSpec.title}</h2>
                      <p className="text-sm text-slate-600 mt-1 leading-relaxed">{activeSpec.summary}</p>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setInspectorSourcePath(activeSpec.kotlinPath);
                        setSection('source');
                      }}
                      className="neuglass-pill inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold text-[#0A1628] whitespace-nowrap shrink-0"
                    >
                      <ExternalLink className="w-3.5 h-3.5 text-blue-600" />
                      <span>Open Full File</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-3 border-t border-white/60">
                    <div className="neuglass-inset rounded-2xl p-4">
                      <div className="text-xs font-semibold text-slate-600 mb-1.5">
                        Android APIs Implemented
                      </div>
                      <ul className="space-y-1 text-xs font-mono text-[#0A1628]">
                        {activeSpec.apis.map((api) => (
                          <li key={api}>{api}</li>
                        ))}
                      </ul>
                    </div>
                    <div className="neuglass-inset rounded-2xl p-4">
                      <div className="text-xs font-semibold text-slate-600 mb-1.5">
                        Android 8.0–16 Compatibility
                      </div>
                      <p className="text-xs text-slate-700 leading-relaxed">
                        {activeSpec.androidCompatibility}
                      </p>
                    </div>
                  </div>
                </div>

                {activeKotlinFile && (
                  <div className="neuglass-card-dark rounded-3xl overflow-hidden">
                    <div className="px-5 py-3.5 bg-white/5 border-b border-white/10 text-white flex items-center justify-between gap-4">
                      <div className="flex items-center gap-2 min-w-0">
                        <Code2 className="w-4 h-4 text-sky-400 shrink-0" />
                        <span className="text-xs font-mono truncate">{activeKotlinFile.path}</span>
                      </div>
                      <button
                        type="button"
                        onClick={handleCopySnippet}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-blue-600/80 border border-white/15 text-xs font-medium whitespace-nowrap shrink-0 transition-colors"
                      >
                        {copiedCode ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                        <span>{copiedCode ? 'Copied' : 'Copy Kotlin'}</span>
                      </button>
                    </div>
                    <div className="max-h-[430px] overflow-auto bg-[#081224]/75 text-slate-100 p-5">
                      <pre className="text-xs leading-6 font-mono whitespace-pre">
                        <code>{activeKotlinFile.content}</code>
                      </pre>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {section === 'source' && (
          <div className="space-y-4">
            <div className="neuglass-card rounded-3xl p-6">
              <h1 className="text-2xl font-bold text-[#0A1628]">
                Android Studio Project Source Explorer
              </h1>
              <p className="text-sm text-slate-600 mt-1">
                Complete Kotlin, Jetpack Compose, AndroidManifest.xml, Gradle Version Catalog, and ProGuard files included in the downloadable Android Studio project.
              </p>
            </div>
            <KotlinSourceExplorer initialSelectedPath={inspectorSourcePath} />
          </div>
        )}

        {section === 'permissions' && <PermissionsAndPolicyView />}
        {section === 'setup' && <SetupAndVerificationView />}
      </main>
    </div>
  );
}
