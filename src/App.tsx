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
  { id: 'unlock', label: 'Unlock Gate' },
  { id: 'dashboard', label: '7-Card Dashboard' },
  { id: 'app_lock', label: 'App Lock' },
  { id: 'intruder_selfie', label: 'Intruder Selfie' },
  { id: 'lock_notifications', label: 'Lock Notifications' },
  { id: 'fake_crash', label: 'Fake App Crash' },
  { id: 'camera_monitor', label: 'Camera Monitor' },
  { id: 'private_browser', label: 'Private Browser' },
  { id: 'statistics', label: 'Statistics' },
  { id: 'settings', label: 'Settings' },
  { id: 'permissions', label: 'Permissions' },
];

export default function App() {
  const [section, setSection] = useState<TopSection>('preview');
  const [activeScreen, setActiveScreen] = useState<SimulatorScreenId>('unlock');
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
    <div className="min-h-screen bg-[#F8FAFC] text-[#0A1628] flex flex-col">
      {/* Three-Zone Top Bar Contract */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur border-b border-slate-200">
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
                className={`py-1 whitespace-nowrap shrink-0 transition-colors border-b-2 ${
                  section === key
                    ? 'border-blue-600 text-[#0A1628] font-semibold'
                    : 'border-transparent text-slate-600 hover:text-[#0A1628]'
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
              className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#0A1628] hover:bg-blue-600 text-white text-xs font-semibold transition-colors whitespace-nowrap shrink-0 disabled:opacity-60"
            >
              <Download className="w-3.5 h-3.5" />
              <span>{downloading ? 'Packaging ZIP...' : 'Download Android ZIP'}</span>
            </button>
          </div>
        </div>

        {/* Mobile navigation strip */}
        <div className="md:hidden border-t border-slate-100 px-4 py-2 flex items-center gap-4 overflow-x-auto text-xs font-medium">
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
              className={`whitespace-nowrap shrink-0 pb-1 border-b-2 ${
                section === key ? 'border-blue-600 text-[#0A1628]' : 'border-transparent text-slate-500'
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
                  Native Kotlin, Jetpack Compose, Material 3, Android Keystore AES-256-GCM, and BiometricPrompt. Test every screen on the left or inspect the corresponding Kotlin implementation on the right.
                </p>
              </div>
              <div className="text-xs text-slate-500 flex items-center gap-2 whitespace-nowrap">
                <span>Kotlin 2.1</span>
                <span aria-hidden="true">·</span>
                <span>minSdk 26 / targetSdk 36</span>
                <span aria-hidden="true">·</span>
                <span className="tabular-nums">{ANDROID_PROJECT_FILES.length} Android Studio Files</span>
              </div>
            </div>

            {/* Screen Selector Segmented Strip */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {SIMULATOR_NAV_LIST.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => setActiveScreen(item.id)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                    activeScreen === item.id
                      ? 'bg-[#0A1628] text-white'
                      : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'
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

              <div className="space-y-5">
                <div className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4">
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
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-[#0A1628] whitespace-nowrap shrink-0"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      <span>Open Full File</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2 border-t border-slate-100">
                    <div>
                      <div className="text-xs font-semibold text-slate-500 mb-1.5">
                        Android APIs Implemented
                      </div>
                      <ul className="space-y-1 text-xs font-mono text-[#0A1628]">
                        {activeSpec.apis.map((api) => (
                          <li key={api}>{api}</li>
                        ))}
                      </ul>
                    </div>
                    <div>
                      <div className="text-xs font-semibold text-slate-500 mb-1.5">
                        Android 8.0–16 Compatibility
                      </div>
                      <p className="text-xs text-slate-600 leading-relaxed">
                        {activeSpec.androidCompatibility}
                      </p>
                    </div>
                  </div>
                </div>

                {activeKotlinFile && (
                  <div className="bg-white border border-slate-200 rounded-2xl overflow-hidden">
                    <div className="px-5 py-3 bg-[#0A1628] text-white flex items-center justify-between gap-4">
                      <div className="flex items-center gap-2 min-w-0">
                        <Code2 className="w-4 h-4 text-sky-400 shrink-0" />
                        <span className="text-xs font-mono truncate">{activeKotlinFile.path}</span>
                      </div>
                      <button
                        type="button"
                        onClick={handleCopySnippet}
                        className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#112240] hover:bg-blue-600 text-xs font-medium whitespace-nowrap shrink-0"
                      >
                        {copiedCode ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                        <span>{copiedCode ? 'Copied' : 'Copy Kotlin'}</span>
                      </button>
                    </div>
                    <div className="max-h-[430px] overflow-auto bg-[#0F172A] text-slate-100 p-5">
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
            <div>
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
