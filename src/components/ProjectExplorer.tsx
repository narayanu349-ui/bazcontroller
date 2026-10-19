import React, { useState } from 'react';
import {
  Folder,
  FileCode,
  FileText,
  Copy,
  Check,
  Download,
  Search,
  ExternalLink,
  Smartphone,
  Monitor
} from 'lucide-react';
import JSZip from 'jszip';
import { PROJECT_FILES, ProjectFile } from '../data/projectFiles';

export const ProjectExplorer: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<ProjectFile>(PROJECT_FILES[0]);
  const [copied, setCopied] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<'ALL' | 'Android' | 'Windows'>('ALL');
  const [isDownloading, setIsDownloading] = useState(false);

  const filteredFiles = PROJECT_FILES.filter((f) => {
    const matchesCategory = activeCategory === 'ALL' || f.category === activeCategory;
    const matchesSearch =
      f.path.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const handleCopyCode = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Generate and download ZIP file
  const handleDownloadZip = async (targetCategory?: 'Android' | 'Windows') => {
    setIsDownloading(true);
    try {
      const zip = new JSZip();
      const filesToZip = targetCategory
        ? PROJECT_FILES.filter((f) => f.category === targetCategory)
        : PROJECT_FILES;

      filesToZip.forEach((file) => {
        zip.file(file.path, file.content);
      });

      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = targetCategory
        ? `BazController_${targetCategory}_Project.zip`
        : 'BazController_Complete_Suite.zip';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('ZIP generation failed:', err);
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto flex flex-col gap-4 p-4 lg:p-6">
      {/* Top Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-neutral-900 border border-neutral-800 rounded-xl px-5 py-3">
        <div className="flex items-center gap-3">
          <Folder className="w-4 h-4 text-amber-500" />
          <span className="font-mono text-xs font-bold text-neutral-200">
            PROJECT SOURCE BROWSER &amp; EXPORTER
          </span>
          <span className="text-neutral-600">·</span>
          <span className="text-xs text-neutral-400">
            Complete Android Studio &amp; Windows .NET 8 Solutions
          </span>
        </div>

        {/* Download Buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => handleDownloadZip('Android')}
            disabled={isDownloading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-800 bg-neutral-950 text-neutral-300 hover:text-white hover:border-neutral-700 text-xs font-mono transition-colors"
          >
            <Smartphone className="w-3.5 h-3.5 text-emerald-400" />
            Download Android (.zip)
          </button>

          <button
            onClick={() => handleDownloadZip('Windows')}
            disabled={isDownloading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-800 bg-neutral-950 text-neutral-300 hover:text-white hover:border-neutral-700 text-xs font-mono transition-colors"
          >
            <Monitor className="w-3.5 h-3.5 text-cyan-400" />
            Download Windows (.zip)
          </button>

          <button
            onClick={() => handleDownloadZip()}
            disabled={isDownloading}
            className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-neutral-950 font-mono font-bold text-xs transition-colors"
          >
            <Download className="w-3.5 h-3.5" />
            {isDownloading ? 'Zipping...' : 'Download Full Suite (.zip)'}
          </button>
        </div>
      </div>

      {/* Main Explorer Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: File Tree & Filter (4 cols) */}
        <div className="lg:col-span-4 bg-neutral-900 border border-neutral-800 rounded-2xl p-4 flex flex-col gap-3">
          {/* Category Tabs */}
          <div className="flex items-center p-1 bg-neutral-950 border border-neutral-800 rounded-lg text-xs font-mono">
            {(['ALL', 'Android', 'Windows'] as const).map((cat) => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={`flex-1 py-1 rounded text-center transition-colors ${
                  activeCategory === cat
                    ? 'bg-amber-500 text-neutral-950 font-bold'
                    : 'text-neutral-400 hover:text-white'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Search Bar */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
            <input
              type="text"
              placeholder="Search files or classes..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-lg pl-9 pr-3 py-1.5 font-mono text-xs text-neutral-300 placeholder-neutral-600 focus:outline-none focus:border-amber-500"
            />
          </div>

          {/* File List */}
          <div className="flex flex-col gap-1 overflow-y-auto max-h-[560px] pr-1">
            {filteredFiles.map((file) => {
              const isSelected = file.path === selectedFile.path;
              return (
                <button
                  key={file.path}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left p-2.5 rounded-lg border font-mono text-xs transition-all flex flex-col gap-0.5 ${
                    isSelected
                      ? 'bg-neutral-800 border-amber-500/60 text-white'
                      : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:text-white hover:border-neutral-700'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-bold truncate text-white">{file.path.split('/').pop()}</span>
                    <span
                      className={`text-[9px] px-1.5 py-0.2 rounded font-mono ${
                        file.category === 'Android'
                          ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-800/40'
                          : 'bg-cyan-950/60 text-cyan-400 border border-cyan-800/40'
                      }`}
                    >
                      {file.category}
                    </span>
                  </div>
                  <span className="text-[10px] text-neutral-500 truncate">{file.path}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Right Column: Code Viewer (8 cols) */}
        <div className="lg:col-span-8 bg-neutral-900 border border-neutral-800 rounded-2xl p-5 flex flex-col gap-3">
          {/* File Info & Copy Button */}
          <div className="flex flex-wrap items-center justify-between gap-2 pb-3 border-b border-neutral-800">
            <div>
              <div className="font-mono text-sm font-bold text-white flex items-center gap-2">
                <FileCode className="w-4 h-4 text-amber-500" />
                {selectedFile.path}
              </div>
              <div className="text-xs text-neutral-400 mt-0.5">{selectedFile.description}</div>
            </div>

            <button
              onClick={handleCopyCode}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 border border-neutral-700 font-mono text-xs text-white transition-colors"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? 'Copied!' : 'Copy File Content'}
            </button>
          </div>

          {/* Syntax-Clean Code Box with Line Numbers */}
          <div className="bg-neutral-950 border border-neutral-800 rounded-xl p-4 overflow-x-auto max-h-[580px] font-mono text-xs leading-relaxed text-neutral-300">
            <pre className="table">
              {selectedFile.content.split('\n').map((line, idx) => (
                <div key={idx} className="table-row hover:bg-neutral-900/50">
                  <span className="table-cell select-none pr-4 text-right text-neutral-600 text-[11px] w-10">
                    {idx + 1}
                  </span>
                  <span className="table-cell whitespace-pre">{line}</span>
                </div>
              ))}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
};
