import React, { useState } from 'react';
import {
  Gamepad2,
  Sliders,
  Terminal,
  Monitor,
  FolderCode,
  Download,
  Maximize2
} from 'lucide-react';
import JSZip from 'jszip';
import { ClientControllerState } from './utils/packetProtocol';
import { VirtualController } from './components/VirtualController';
import { LayoutEditor } from './components/LayoutEditor';
import { PacketInspector } from './components/PacketInspector';
import { WindowsCompanionSim } from './components/WindowsCompanionSim';
import { ProjectExplorer } from './components/ProjectExplorer';
import { PROJECT_FILES } from './data/projectFiles';

type ActiveTab = 'CONTROLLER' | 'LAYOUT_STUDIO' | 'PROTOCOL' | 'WINDOWS' | 'PROJECT_FILES';

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('CONTROLLER');

  // Shared live controller state across all views
  const [controllerState, setControllerState] = useState<ClientControllerState>({
    steering: 0,
    throttle: 0,
    brake: 0,
    clutch: 0,
    handbrake: 0,
    joyX: 0,
    joyY: 0,
    gear: 0,
    indicatorLeft: false,
    indicatorRight: false,
    hazards: false,
    headlights: 0,
    horn: false,
    wipers: 0,
    cruiseControl: false,
    diffLock: false,
    engineStarted: true,
    beacon: false,
    retarder: 0,
    camera: 1,
    dpadUp: false,
    dpadDown: false,
    dpadLeft: false,
    dpadRight: false,
    customButtons: [false, false, false, false]
  });

  const handleDownloadFullSuite = async () => {
    const zip = new JSZip();
    PROJECT_FILES.forEach((f) => {
      zip.file(f.path, f.content);
    });
    const blob = await zip.generateAsync({ type: 'blob' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'BazController_Complete_Source_Suite.zip';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const isControllerMode = activeTab === 'CONTROLLER';

  return (
    <div className={`w-full min-h-screen bg-[#0d0f14] text-neutral-100 flex flex-col ${isControllerMode ? 'h-screen overflow-hidden' : ''}`}>
      {/* Top Bar Navigation (Always slim, single-line, non-intrusive) */}
      <header className="flex items-center justify-between gap-4 px-4 py-2 border-b border-[#202532] bg-[#12151d]/95 backdrop-blur z-50 shrink-0">
        {/* Brand Zone */}
        <div className="flex items-center gap-2 whitespace-nowrap shrink-0">
          <button
            onClick={() => setActiveTab('CONTROLLER')}
            className="text-base font-black tracking-wider text-amber-400 hover:text-amber-300 transition-colors font-mono"
          >
            BAZCONTROLLER
          </button>
          <span className="text-[11px] text-neutral-500 hidden sm:inline">· Gaming Cockpit</span>
        </div>

        {/* Navigation Tabs (Single-line, clear, accessible) */}
        <nav className="flex items-center gap-1.5 sm:gap-4 text-xs font-mono">
          <button
            onClick={() => setActiveTab('CONTROLLER')}
            className={`px-3 py-1 rounded-md transition-colors whitespace-nowrap flex items-center gap-1.5 ${
              activeTab === 'CONTROLLER'
                ? 'bg-amber-500 text-neutral-950 font-bold'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <Gamepad2 className="w-3.5 h-3.5" />
            <span>Gaming Rig</span>
          </button>

          <button
            onClick={() => setActiveTab('LAYOUT_STUDIO')}
            className={`px-3 py-1 rounded-md transition-colors whitespace-nowrap flex items-center gap-1.5 ${
              activeTab === 'LAYOUT_STUDIO'
                ? 'bg-amber-500 text-neutral-950 font-bold'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Layout Studio</span>
            <span className="sm:hidden">Studio</span>
          </button>

          <button
            onClick={() => setActiveTab('PROTOCOL')}
            className={`px-3 py-1 rounded-md transition-colors whitespace-nowrap flex items-center gap-1.5 ${
              activeTab === 'PROTOCOL'
                ? 'bg-amber-500 text-neutral-950 font-bold'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <Terminal className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Packet Stream</span>
            <span className="sm:hidden">Packets</span>
          </button>

          <button
            onClick={() => setActiveTab('WINDOWS')}
            className={`px-3 py-1 rounded-md transition-colors whitespace-nowrap flex items-center gap-1.5 ${
              activeTab === 'WINDOWS'
                ? 'bg-amber-500 text-neutral-950 font-bold'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <Monitor className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Windows ViGEm</span>
            <span className="sm:hidden">Windows</span>
          </button>

          <button
            onClick={() => setActiveTab('PROJECT_FILES')}
            className={`px-3 py-1 rounded-md transition-colors whitespace-nowrap flex items-center gap-1.5 ${
              activeTab === 'PROJECT_FILES'
                ? 'bg-amber-500 text-neutral-950 font-bold'
                : 'text-neutral-400 hover:text-white'
            }`}
          >
            <FolderCode className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Source Files</span>
            <span className="sm:hidden">Code</span>
          </button>
        </nav>

        {/* Action Button */}
        <div className="flex items-center gap-2 shrink-0">
          <button
            onClick={handleDownloadFullSuite}
            className="flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold text-neutral-950 bg-amber-500 rounded-lg hover:bg-amber-400 transition-colors whitespace-nowrap shadow-sm"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden md:inline">Download Suite (.zip)</span>
            <span className="md:hidden">.ZIP</span>
          </button>
        </div>
      </header>

      {/* Main Viewport Content Area */}
      <main className={`flex-1 w-full ${isControllerMode ? 'h-[calc(100vh-45px)] overflow-hidden relative' : 'overflow-y-auto'}`}>
        {activeTab === 'CONTROLLER' && (
          <VirtualController
            state={controllerState}
            setState={setControllerState}
            onOpenSettings={() => setActiveTab('PROTOCOL')}
            onOpenStudio={() => setActiveTab('LAYOUT_STUDIO')}
          />
        )}
        {activeTab === 'LAYOUT_STUDIO' && <LayoutEditor />}
        {activeTab === 'PROTOCOL' && <PacketInspector state={controllerState} />}
        {activeTab === 'WINDOWS' && <WindowsCompanionSim state={controllerState} />}
        {activeTab === 'PROJECT_FILES' && <ProjectExplorer />}
      </main>
    </div>
  );
}
