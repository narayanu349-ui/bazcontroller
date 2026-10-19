import React, { useState } from 'react';
import {
  Gamepad2,
  Monitor,
  ShieldCheck,
  CheckCircle,
  HardDrive,
  Cpu,
  Layers,
  Settings
} from 'lucide-react';
import { ClientControllerState } from '../utils/packetProtocol';

interface WindowsCompanionSimProps {
  state: ClientControllerState;
}

export const WindowsCompanionSim: React.FC<WindowsCompanionSimProps> = ({ state }) => {
  const [selectedProfile, setSelectedProfile] = useState('ETS2');
  const [pinCode] = useState('482910');

  const profiles = [
    {
      id: 'ETS2',
      name: 'Euro Truck Simulator 2',
      deadzone: '1.5%',
      linearity: '1.2x Progressive',
      shifter: '6-Speed Real H-Shifter'
    },
    {
      id: 'ATS',
      name: 'American Truck Simulator',
      deadzone: '2.0%',
      linearity: '1.25x Long Haul',
      shifter: '10/18-Speed Eaton Fuller'
    },
    {
      id: 'ASSETTO',
      name: 'Assetto Corsa / Sim Racing',
      deadzone: '0.0%',
      linearity: '1.0x 1:1 Direct Input',
      shifter: 'Sequential Paddle Shift'
    },
    {
      id: 'GENERIC',
      name: 'Generic Steam XInput',
      deadzone: '2.0%',
      linearity: 'Standard Xbox 360 Curve',
      shifter: 'Standard Gamepad Buttons'
    }
  ];

  const currentProfile = profiles.find((p) => p.id === selectedProfile) || profiles[0];

  return (
    <div className="w-full max-w-7xl mx-auto flex flex-col gap-4 p-4 lg:p-6">
      {/* Top Windows Companion Banner */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-neutral-900 border border-neutral-800 rounded-xl px-5 py-3">
        <div className="flex items-center gap-3">
          <Monitor className="w-4 h-4 text-cyan-400" />
          <span className="font-mono text-xs font-bold text-neutral-200">
            WINDOWS COMPANION CLIENT (C# .NET 8 WPF)
          </span>
          <span className="text-neutral-600">·</span>
          <span className="font-mono text-xs text-emerald-400 flex items-center gap-1">
            <CheckCircle className="w-3 h-3" /> ViGEmBus Kernel Hook Active
          </span>
        </div>

        {/* Pairing Security Code */}
        <div className="flex items-center gap-2 bg-neutral-950 px-3 py-1.5 rounded-lg border border-neutral-800">
          <ShieldCheck className="w-3.5 h-3.5 text-amber-500" />
          <span className="text-[10px] font-mono text-neutral-400">Pairing PIN:</span>
          <span className="font-mono text-xs font-bold text-amber-400">{pinCode}</span>
        </div>
      </div>

      {/* Main Grid: Virtual Controller Diagram & Game Profiles */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Virtual Controller Mapping Visualizer (7 cols) */}
        <div className="lg:col-span-7 bg-neutral-900 border border-neutral-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center justify-between pb-2 border-b border-neutral-800">
            <span className="font-mono text-xs font-bold text-neutral-300">
              VIRTUAL XBOX 360 CONTROLLER (XINPUT EMULATION)
            </span>
            <span className="text-[10px] font-mono text-neutral-500">ViGEmClient.CreateXbox360Controller()</span>
          </div>

          {/* Interactive Gamepad Diagram */}
          <div className="relative bg-neutral-950 border border-neutral-800 rounded-xl p-6 flex flex-col items-center justify-center min-h-[300px] overflow-hidden">
            {/* Left Trigger (Brake) */}
            <div className="absolute top-4 left-6 flex flex-col items-center">
              <span className="text-[10px] font-mono text-neutral-500">LT (BRAKE)</span>
              <div className="w-12 h-4 bg-neutral-900 border border-neutral-800 rounded overflow-hidden mt-1">
                <div className="h-full bg-red-500 transition-all" style={{ width: `${state.brake * 100}%` }} />
              </div>
              <span className="text-[10px] font-mono text-neutral-400">{Math.round(state.brake * 255)}</span>
            </div>

            {/* Right Trigger (Throttle) */}
            <div className="absolute top-4 right-6 flex flex-col items-center">
              <span className="text-[10px] font-mono text-neutral-500">RT (THROTTLE)</span>
              <div className="w-12 h-4 bg-neutral-900 border border-neutral-800 rounded overflow-hidden mt-1">
                <div className="h-full bg-emerald-500 transition-all" style={{ width: `${state.throttle * 100}%` }} />
              </div>
              <span className="text-[10px] font-mono text-neutral-400">{Math.round(state.throttle * 255)}</span>
            </div>

            {/* Center Xbox Emblem */}
            <div className="w-14 h-14 rounded-full bg-neutral-900 border-2 border-emerald-500/60 shadow-lg shadow-emerald-500/20 flex items-center justify-center mb-6">
              <Gamepad2 className="w-7 h-7 text-emerald-400" />
            </div>

            {/* Left Stick (Steering Wheel) & Right Buttons (A/B/X/Y) */}
            <div className="w-full flex items-center justify-around px-8">
              {/* Left Stick (Steering) */}
              <div className="flex flex-col items-center gap-1">
                <span className="text-[10px] font-mono text-neutral-400">LEFT THUMB (STEERING)</span>
                <div className="relative w-24 h-24 rounded-full bg-neutral-900 border border-neutral-750 flex items-center justify-center shadow-inner">
                  {/* Crosshair */}
                  <div className="absolute w-full h-[1px] bg-neutral-800" />
                  <div className="absolute h-full w-[1px] bg-neutral-800" />
                  {/* Thumb Indicator */}
                  <div
                    className="w-8 h-8 rounded-full bg-amber-500/80 border border-amber-300 shadow-md shadow-amber-500/40 transition-transform"
                    style={{
                      transform: `translate(${state.steering * 32}px, 0px)`
                    }}
                  />
                </div>
                <span className="text-[10px] font-mono text-amber-400 font-bold">
                  Axis X: {Math.round(state.steering * 32767)}
                </span>
              </div>

              {/* Action Buttons (A/B/X/Y) */}
              <div className="grid grid-cols-3 gap-2 items-center">
                <div />
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-mono font-bold text-xs border ${
                    state.headlights > 0
                      ? 'bg-yellow-500 text-neutral-950 border-yellow-300 shadow-md shadow-yellow-500/30'
                      : 'bg-neutral-900 border-neutral-800 text-neutral-500'
                  }`}
                >
                  Y
                </div>
                <div />
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-mono font-bold text-xs border ${
                    state.wipers > 0
                      ? 'bg-blue-500 text-neutral-950 border-blue-300 shadow-md shadow-blue-500/30'
                      : 'bg-neutral-900 border-neutral-800 text-neutral-500'
                  }`}
                >
                  X
                </div>
                <div className="w-4 h-4" />
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-mono font-bold text-xs border ${
                    state.handbrake > 0
                      ? 'bg-red-500 text-neutral-950 border-red-300 shadow-md shadow-red-500/30'
                      : 'bg-neutral-900 border-neutral-800 text-neutral-500'
                  }`}
                >
                  B
                </div>
                <div />
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-mono font-bold text-xs border ${
                    state.horn
                      ? 'bg-emerald-500 text-neutral-950 border-emerald-300 shadow-md shadow-emerald-500/30'
                      : 'bg-neutral-900 border-neutral-800 text-neutral-500'
                  }`}
                >
                  A
                </div>
                <div />
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Game Compatibility & Profiles (5 cols) */}
        <div className="lg:col-span-5 bg-neutral-900 border border-neutral-800 rounded-2xl p-5 flex flex-col gap-4">
          <div className="flex items-center justify-between pb-2 border-b border-neutral-800">
            <span className="font-mono text-xs font-bold text-neutral-300">GAME PROFILES</span>
            <span className="text-[10px] font-mono text-neutral-500">AUTOMATIC BINDINGS</span>
          </div>

          <div className="flex flex-col gap-2">
            {profiles.map((p) => (
              <button
                key={p.id}
                onClick={() => setSelectedProfile(p.id)}
                className={`p-3 rounded-xl border text-left transition-all ${
                  p.id === selectedProfile
                    ? 'bg-neutral-800 border-amber-500/60 shadow-lg shadow-black/40'
                    : 'bg-neutral-950 border-neutral-800 hover:border-neutral-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-xs text-white">{p.name}</span>
                  {p.id === selectedProfile && (
                    <span className="text-[10px] font-mono text-amber-400 font-bold">ACTIVE ✓</span>
                  )}
                </div>
                <div className="text-[10px] font-mono text-neutral-400 mt-1">
                  Deadzone: {p.deadzone} · Curve: {p.linearity}
                </div>
                <div className="text-[10px] font-mono text-cyan-400/80 mt-0.5">
                  Shifter: {p.shifter}
                </div>
              </button>
            ))}
          </div>

          {/* ViGEm Setup Instructions */}
          <div className="bg-neutral-950 border border-neutral-800 rounded-xl p-3.5 flex flex-col gap-2 font-mono text-xs">
            <span className="font-bold text-neutral-300 flex items-center gap-1.5">
              <HardDrive className="w-3.5 h-3.5 text-cyan-400" /> ViGEmBus Installation:
            </span>
            <p className="text-[11px] text-neutral-400 leading-relaxed">
              1. Download &amp; run <span className="text-amber-400 font-bold">ViGEmBus_Setup_x64.exe</span> (from github.com/nefarius/ViGEmBus).
              <br />
              2. Launch <span className="text-amber-400 font-bold">BazController.Windows.exe</span>.
              <br />
              3. Connect phone via USB or Wi-Fi. Games recognize it as an official Xbox 360 wheel!
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
