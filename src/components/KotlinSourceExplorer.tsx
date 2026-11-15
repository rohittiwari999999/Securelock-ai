import { useMemo, useState } from 'react';
import { Check, Copy, Download, FileCode2, FolderOpen, Search } from 'lucide-react';
import { ANDROID_PROJECT_FILES, downloadAndroidProjectZip, ProjectFileEntry } from '../data/androidProject';

interface KotlinSourceExplorerProps {
  initialSelectedPath?: string;
}

export function KotlinSourceExplorer({ initialSelectedPath }: KotlinSourceExplorerProps) {
  const [selectedPath, setSelectedPath] = useState<string>(
    initialSelectedPath && ANDROID_PROJECT_FILES.some((f) => f.path === initialSelectedPath)
      ? initialSelectedPath
      : ANDROID_PROJECT_FILES[0]?.path ?? ''
  );
  const [searchTerm, setSearchTerm] = useState('');
  const [copied, setCopied] = useState(false);

  const filteredFiles = useMemo(() => {
    const q = searchTerm.trim().toLowerCase();
    if (!q) return ANDROID_PROJECT_FILES;
    return ANDROID_PROJECT_FILES.filter(
      (f) => f.path.toLowerCase().includes(q) || f.category.toLowerCase().includes(q)
    );
  }, [searchTerm]);

  const grouped = useMemo(() => {
    const map = new Map<string, ProjectFileEntry[]>();
    for (const file of filteredFiles) {
      const list = map.get(file.category) ?? [];
      list.push(file);
      map.set(file.category, list);
    }
    return Array.from(map.entries());
  }, [filteredFiles]);

  const activeFile =
    ANDROID_PROJECT_FILES.find((f) => f.path === selectedPath) ?? ANDROID_PROJECT_FILES[0];

  const handleCopy = async () => {
    if (!activeFile) return;
    await navigator.clipboard.writeText(activeFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[320px_1fr] gap-6 items-start">
      <aside className="neuglass-card rounded-3xl p-4 lg:sticky lg:top-20">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2 text-sm font-semibold text-[#0A1628]">
            <FolderOpen className="w-4 h-4 text-blue-600" />
            <span>Project Tree ({ANDROID_PROJECT_FILES.length} files)</span>
          </div>
          <button
            type="button"
            onClick={() => downloadAndroidProjectZip()}
            className="neuglass-pill inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium text-blue-600 hover:text-blue-700 whitespace-nowrap shrink-0"
          >
            <Download className="w-3.5 h-3.5" />
            <span>ZIP</span>
          </button>
        </div>

        <div className="relative mb-4">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Filter Kotlin, XML, Gradle..."
            className="w-full pl-9 pr-3 py-2 text-sm neuglass-inset rounded-xl text-[#0A1628] placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
          />
        </div>

        <div className="space-y-4 max-h-[620px] overflow-y-auto pr-1">
          {grouped.map(([category, files]) => (
            <div key={category}>
              <div className="text-xs font-semibold text-slate-500 px-2 mb-1.5">
                {category}
              </div>
              <div className="space-y-1">
                {files.map((file) => {
                  const isActive = activeFile?.path === file.path;
                  return (
                    <button
                      key={file.path}
                      type="button"
                      onClick={() => setSelectedPath(file.path)}
                      className={`w-full text-left px-3 py-2 rounded-xl text-xs flex items-center gap-2 transition-all ${
                        isActive
                          ? 'bg-gradient-to-r from-[#0A1628] to-[#1E3A8A] text-white font-medium shadow-[3px_4px_12px_rgba(37,99,235,0.28)] border border-white/20'
                          : 'text-slate-700 hover:bg-white/60'
                      }`}
                    >
                      <FileCode2
                        className={`w-3.5 h-3.5 shrink-0 ${
                          isActive ? 'text-sky-400' : 'text-slate-400'
                        }`}
                      />
                      <span className="truncate">{file.name}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
          {grouped.length === 0 && (
            <p className="text-xs text-slate-500 px-2 py-4">No matching files found.</p>
          )}
        </div>
      </aside>

      <section className="neuglass-card-dark rounded-3xl overflow-hidden">
        {activeFile && (
          <>
            <div className="px-5 py-3.5 bg-white/5 border-b border-white/10 text-white flex flex-wrap items-center justify-between gap-4">
              <div className="min-w-0">
                <div className="text-sm font-semibold truncate">{activeFile.name}</div>
                <div className="text-xs text-slate-300 font-mono truncate">{activeFile.path}</div>
              </div>
              <div className="flex items-center gap-3 shrink-0">
                <span className="text-xs text-sky-300 tabular-nums">
                  {activeFile.content.split('\n').length} lines
                </span>
                <button
                  type="button"
                  onClick={handleCopy}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-blue-600/80 border border-white/15 text-xs font-medium text-white transition-colors whitespace-nowrap shrink-0"
                >
                  {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copied ? 'Copied' : 'Copy Code'}</span>
                </button>
              </div>
            </div>

            <div className="max-h-[700px] overflow-auto bg-[#081224]/75 text-slate-100 p-5">
              <pre className="text-xs leading-6 font-mono whitespace-pre">
                <code>{activeFile.content}</code>
              </pre>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
