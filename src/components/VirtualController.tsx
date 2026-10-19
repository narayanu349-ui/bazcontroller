import React, { useState, useRef, useEffect, useCallback } from 'react';
import {
  Volume2,
  VolumeX,
  RotateCcw,
  Maximize2,
  Minimize2,
  Radio,
  Sliders,
  Power,
  Camera,
  Layers,
  Sparkles,
  Flame,
  Settings,
  ChevronLeft,
  ChevronRight,
  ShieldCheck,
  Disc,
  Play
} from 'lucide-react';
import { ClientControllerState } from '../utils/packetProtocol';
import { soundManager } from '../utils/audioSynthesizer';

export type ControllerTemplateId = 'TRUCK_RIG' | 'HIGHWAY_HAUL' | 'GT_RACING' | 'PRO_GAMEPAD';

interface VirtualControllerProps {
  state: ClientControllerState;
  setState: React.Dispatch<React.SetStateAction<ClientControllerState>>;
  onOpenSettings?: () => void;
  onOpenStudio?: () => void;
}

export const VirtualController: React.FC<VirtualControllerProps> = ({
  state,
  setState,
  onOpenSettings,
  onOpenStudio
}) => {
  const [template, setTemplate] = useState<ControllerTemplateId>('TRUCK_RIG');
  const [wheelAngle, setWheelAngle] = useState(0); // -360° to +360°
  const [maxWheelRange, setMaxWheelRange] = useState(360);
  const [sensitivity, setSensitivity] = useState(1.0);
  const [autoCenter, setAutoCenter] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [isPhoneFrame, setIsPhoneFrame] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // Turn signal blinker timer
  const [blinkerPhase, setBlinkerPhase] = useState(false);

  // Simulated telemetry
  const simulatedSpeed = Math.round(Math.abs(state.throttle - state.brake) * (state.engineStarted ? 112 : 0));
  const simulatedRpm = state.engineStarted ? 0.15 + state.throttle * 0.85 : 0;

  useEffect(() => {
    soundManager.enabled = soundEnabled;
  }, [soundEnabled]);

  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;
    if (state.indicatorLeft || state.indicatorRight || state.hazards) {
      interval = setInterval(() => {
        setBlinkerPhase((prev) => {
          const next = !prev;
          if (next) soundManager.playIndicatorClick(false);
          else soundManager.playIndicatorClick(true);
          return next;
        });
      }, 360);
    } else {
      setBlinkerPhase(false);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [state.indicatorLeft, state.indicatorRight, state.hazards]);

  // Engine sound loop
  useEffect(() => {
    soundManager.updateEngineSound(state.throttle, simulatedRpm, state.engineStarted);
  }, [state.throttle, simulatedRpm, state.engineStarted]);

  // 360° Steering Wheel Touch & Drag Handler
  const wheelRef = useRef<HTMLDivElement>(null);
  const isDraggingWheel = useRef(false);
  const startAngleRef = useRef(0);
  const startWheelRef = useRef(0);

  const handleWheelStart = (e: React.MouseEvent | React.TouchEvent) => {
    e.preventDefault();
    isDraggingWheel.current = true;
    const rect = wheelRef.current?.getBoundingClientRect();
    if (!rect) return;
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    startAngleRef.current = Math.atan2(clientY - centerY, clientX - centerX) * (180 / Math.PI);
    startWheelRef.current = wheelAngle;
  };

  const handleWheelMove = useCallback(
    (e: MouseEvent | TouchEvent) => {
      if (!isDraggingWheel.current || !wheelRef.current) return;
      const rect = wheelRef.current.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
      const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
      const currentAngle = Math.atan2(clientY - centerY, clientX - centerX) * (180 / Math.PI);

      let delta = currentAngle - startAngleRef.current;
      while (delta > 180) delta -= 360;
      while (delta < -180) delta += 360;

      const newAngle = Math.max(
        -maxWheelRange,
        Math.min(maxWheelRange, startWheelRef.current + delta)
      );
      setWheelAngle(newAngle);

      const normalized = Math.max(-1, Math.min(1, (newAngle / maxWheelRange) * sensitivity));
      setState((prev) => ({ ...prev, steering: normalized }));
    },
    [maxWheelRange, sensitivity, setState]
  );

  const handleWheelEnd = useCallback(() => {
    if (!isDraggingWheel.current) return;
    isDraggingWheel.current = false;
    if (autoCenter) {
      setWheelAngle(0);
      setState((prev) => ({ ...prev, steering: 0 }));
      soundManager.playTick();
    }
  }, [autoCenter, setState]);

  useEffect(() => {
    window.addEventListener('mousemove', handleWheelMove);
    window.addEventListener('mouseup', handleWheelEnd);
    window.addEventListener('touchmove', handleWheelMove, { passive: false });
    window.addEventListener('touchend', handleWheelEnd);
    return () => {
      window.removeEventListener('mousemove', handleWheelMove);
      window.removeEventListener('mouseup', handleWheelEnd);
      window.removeEventListener('touchmove', handleWheelMove);
      window.removeEventListener('touchend', handleWheelEnd);
    };
  }, [handleWheelMove, handleWheelEnd]);

  // Touch Pedal Handler
  const handlePedalTouch = (type: 'throttle' | 'brake' | 'clutch', e: React.TouchEvent | React.MouseEvent) => {
    e.preventDefault();
    const target = e.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY;
    const pressure = Math.max(0, Math.min(1, 1 - (clientY - rect.top) / rect.height));
    setState((prev) => ({ ...prev, [type]: pressure }));
    soundManager.playTick();
  };

  const handlePedalRelease = (type: 'throttle' | 'brake' | 'clutch') => {
    setState((prev) => ({ ...prev, [type]: 0 }));
  };

  // Fullscreen Toggle
  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
      setIsFullscreen(true);
    } else {
      document.exitFullscreen().catch(() => {});
      setIsFullscreen(false);
    }
  };

  return (
    <div
      className={`relative w-full h-full flex flex-col items-center justify-center select-none overflow-hidden ${
        isPhoneFrame
          ? 'p-2 md:p-6 bg-neutral-950 min-h-screen'
          : 'fixed inset-0 bg-[#0f1115] z-30'
      }`}
    >
      {/* Phone Mockup Frame wrapper if enabled */}
      <div
        className={`relative w-full h-full flex flex-col justify-between overflow-hidden transition-all duration-300 ${
          isPhoneFrame
            ? 'max-w-[96vw] max-h-[92vh] aspect-[19.5/9] rounded-[44px] border-[10px] border-[#222630] shadow-[0_0_60px_rgba(0,0,0,0.9)] bg-[#13151b]'
            : 'w-full h-full bg-[#13151b]'
        }`}
      >
        {/* Subtle camera punch-hole cutout for realistic smartphone appearance */}
        {isPhoneFrame && (
          <div className="absolute top-1/2 left-3 -translate-y-1/2 w-3.5 h-3.5 rounded-full bg-black border border-neutral-800 z-50 pointer-events-none" />
        )}

        {/* ----------------- TOP ULTRA-COMPACT FLOATING STATUS BAR ----------------- */}
        <div className="relative z-40 w-full px-4 py-1.5 flex items-center justify-between bg-[#181a22]/90 backdrop-blur border-b border-[#262a36] text-[11px] font-mono">
          {/* Left: Template Switcher Tabs */}
          <div className="flex items-center gap-1.5">
            <span className="text-amber-500 font-black tracking-wider text-xs hidden sm:inline mr-1">
              ROUX·BAZ
            </span>
            {[
              { id: 'TRUCK_RIG', label: 'Truck Cockpit' },
              { id: 'HIGHWAY_HAUL', label: 'Highway Cruiser' },
              { id: 'GT_RACING', label: 'GT Racing' },
              { id: 'PRO_GAMEPAD', label: 'Pro Gamepad' }
            ].map((t) => (
              <button
                key={t.id}
                onClick={() => {
                  setTemplate(t.id as ControllerTemplateId);
                  soundManager.playTick();
                }}
                className={`px-2.5 py-1 rounded transition-colors whitespace-nowrap ${
                  template === t.id
                    ? 'bg-amber-500 text-neutral-950 font-bold shadow-sm'
                    : 'text-neutral-400 hover:text-white bg-[#101217] border border-[#222632]'
                }`}
              >
                {t.label}
              </button>
            ))}
          </div>

          {/* Right: Telemetry & Controls */}
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 text-emerald-400 font-bold bg-[#101217] px-2 py-0.5 rounded border border-[#222632]">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>UDP &lt;8ms (120 FPS)</span>
            </div>

            <button
              onClick={() => setSoundEnabled(!soundEnabled)}
              className="p-1 rounded bg-[#101217] border border-[#222632] text-neutral-400 hover:text-white"
              title="Toggle Audio Feedback"
            >
              {soundEnabled ? <Volume2 className="w-3.5 h-3.5 text-emerald-400" /> : <VolumeX className="w-3.5 h-3.5 text-neutral-500" />}
            </button>

            <button
              onClick={() => setIsPhoneFrame(!isPhoneFrame)}
              className="px-2 py-0.5 rounded bg-[#101217] border border-[#222632] text-neutral-400 hover:text-white hidden sm:inline"
            >
              {isPhoneFrame ? 'Edge-to-Edge' : 'Phone Frame'}
            </button>

            <button
              onClick={toggleFullscreen}
              className="p-1 rounded bg-[#101217] border border-[#222632] text-neutral-400 hover:text-white"
              title="Toggle Fullscreen"
            >
              {isFullscreen ? <Minimize2 className="w-3.5 h-3.5" /> : <Maximize2 className="w-3.5 h-3.5" />}
            </button>
          </div>
        </div>

        {/* ----------------- MAIN GAMING COCKPIT CANVAS (100% SCREEN FILL) ----------------- */}
        <div className="relative flex-1 w-full h-full flex items-center justify-between px-3 py-2 gap-2 overflow-hidden bg-gradient-to-b from-[#15171e] to-[#0f1116]">
          {/* =========================================================================
              TEMPLATE 1 & 2: TRUCK COCKPIT & HIGHWAY CRUISER (MATCHES USER IMAGES)
             ========================================================================= */}
          {(template === 'TRUCK_RIG' || template === 'HIGHWAY_HAUL') && (
            <>
              {/* ------------ LEFT THUMB ZONE: 360° WHEEL & PADDLES ------------ */}
              <div className="relative h-full flex flex-col items-center justify-center flex-1 max-w-[36%] z-10">
                {/* Turn signal stalk arrows */}
                <div className="w-full flex items-center justify-between px-6 mb-1">
                  <button
                    onClick={() => {
                      setState((prev) => ({
                        ...prev,
                        indicatorLeft: !prev.indicatorLeft,
                        indicatorRight: false
                      }));
                      soundManager.playPaddleClick();
                    }}
                    className={`w-9 h-7 rounded-lg border font-black text-sm flex items-center justify-center transition-all ${
                      state.indicatorLeft && blinkerPhase
                        ? 'bg-emerald-500 text-neutral-950 border-emerald-400 shadow-lg shadow-emerald-500/40'
                        : 'bg-[#1b1e27] border-[#2c3240] text-neutral-400 active:scale-95'
                    }`}
                  >
                    ◀
                  </button>

                  <button
                    onClick={() => {
                      setState((prev) => ({
                        ...prev,
                        indicatorRight: !prev.indicatorRight,
                        indicatorLeft: false
                      }));
                      soundManager.playPaddleClick();
                    }}
                    className={`w-9 h-7 rounded-lg border font-black text-sm flex items-center justify-center transition-all ${
                      state.indicatorRight && blinkerPhase
                        ? 'bg-emerald-500 text-neutral-950 border-emerald-400 shadow-lg shadow-emerald-500/40'
                        : 'bg-[#1b1e27] border-[#2c3240] text-neutral-400 active:scale-95'
                    }`}
                  >
                    ▶
                  </button>
                </div>

                {/* Steering Wheel Container with Hugging Paddle Shifters */}
                <div className="relative w-full aspect-square max-h-[75vh] flex items-center justify-center">
                  {/* Left Paddle Shifter (-) */}
                  <button
                    onClick={() => {
                      soundManager.playPaddleClick();
                      if (state.gear > -1) setState((prev) => ({ ...prev, gear: prev.gear - 1 }));
                    }}
                    className="absolute -left-1 sm:left-1 top-1/2 -translate-y-1/2 w-6 sm:w-8 h-28 sm:h-36 rounded-l-xl bg-gradient-to-r from-[#202532] to-[#161922] border-l-2 border-y-2 border-[#333a4d] flex flex-col items-center justify-center shadow-lg active:scale-95 transition-transform z-0"
                  >
                    <span className="font-mono text-base font-black text-neutral-400">−</span>
                    <span className="text-[8px] font-mono text-neutral-500 mt-1">DOWN</span>
                  </button>

                  {/* 360° Rotatable Steering Wheel */}
                  <div
                    ref={wheelRef}
                    onMouseDown={handleWheelStart}
                    onTouchStart={handleWheelStart}
                    className="relative w-[82%] h-[82%] rounded-full cursor-grab active:cursor-grabbing touch-none select-none z-10 transition-transform"
                    style={{
                      transform: `rotate(${wheelAngle}deg)`,
                      transition: isDraggingWheel.current ? 'none' : 'transform 0.15s ease-out'
                    }}
                  >
                    {/* Outer Wheel Rim */}
                    <div className="absolute inset-0 rounded-full border-[18px] sm:border-[24px] border-[#202532] shadow-[inset_0_4px_12px_rgba(0,0,0,0.8),0_10px_30px_rgba(0,0,0,0.9)] flex items-center justify-center bg-transparent">
                      {/* 12 O'Clock Racing Center Stripe */}
                      <div className="absolute -top-[18px] sm:-top-[24px] left-1/2 -translate-x-1/2 w-3 sm:w-4 h-[18px] sm:h-[24px] bg-amber-500 rounded-t shadow-md" />

                      {/* 3 Spokes (Left, Right, Bottom) */}
                      <div className="absolute top-1/2 -left-1 -translate-y-1/2 w-[38%] h-5 sm:h-6 bg-[#282e3d] border-t border-b border-[#3e475d] rounded-l" />
                      <div className="absolute top-1/2 -right-1 -translate-y-1/2 w-[38%] h-5 sm:h-6 bg-[#282e3d] border-t border-b border-[#3e475d] rounded-r" />
                      <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-5 sm:w-6 h-[38%] bg-[#282e3d] border-l border-r border-[#3e475d] rounded-b" />

                      {/* Center Horn Button Hub */}
                      <button
                        type="button"
                        onMouseDown={(e) => {
                          e.stopPropagation();
                          setState((prev) => ({ ...prev, horn: true }));
                          soundManager.playHorn(true);
                        }}
                        onMouseUp={(e) => {
                          e.stopPropagation();
                          setState((prev) => ({ ...prev, horn: false }));
                        }}
                        onTouchStart={(e) => {
                          e.stopPropagation();
                          setState((prev) => ({ ...prev, horn: true }));
                          soundManager.playHorn(true);
                        }}
                        onTouchEnd={(e) => {
                          e.stopPropagation();
                          setState((prev) => ({ ...prev, horn: false }));
                        }}
                        className={`w-20 sm:w-24 h-20 sm:h-24 rounded-full border-2 flex flex-col items-center justify-center shadow-2xl active:scale-95 transition-all z-20 ${
                          state.horn
                            ? 'bg-amber-500 border-amber-300 text-neutral-950 font-black'
                            : 'bg-gradient-to-b from-[#222734] to-[#151922] border-[#363f54] text-amber-500'
                        }`}
                      >
                        <span className="font-mono text-xs sm:text-sm font-black tracking-widest">BAZ</span>
                        <span className="text-[8px] font-mono text-neutral-400">HORN</span>
                      </button>
                    </div>
                  </div>

                  {/* Right Paddle Shifter (+) */}
                  <button
                    onClick={() => {
                      soundManager.playPaddleClick();
                      if (state.gear < 6) setState((prev) => ({ ...prev, gear: prev.gear + 1 }));
                    }}
                    className="absolute -right-1 sm:right-1 top-1/2 -translate-y-1/2 w-6 sm:w-8 h-28 sm:h-36 rounded-r-xl bg-gradient-to-r from-[#161922] to-[#202532] border-r-2 border-y-2 border-[#333a4d] flex flex-col items-center justify-center shadow-lg active:scale-95 transition-transform z-0"
                  >
                    <span className="font-mono text-base font-black text-neutral-400">+</span>
                    <span className="text-[8px] font-mono text-neutral-500 mt-1">UP</span>
                  </button>
                </div>

                {/* Fine Calibration Slider directly below wheel (Image 2) */}
                <div className="w-[85%] flex items-center gap-2 mt-1 px-3 py-1 rounded-full bg-[#181b24] border border-[#262c3b]">
                  <RotateCcw
                    className="w-3 h-3 text-neutral-500 cursor-pointer hover:text-amber-400"
                    onClick={() => {
                      setWheelAngle(0);
                      setState((prev) => ({ ...prev, steering: 0 }));
                      soundManager.playTick();
                    }}
                  />
                  <input
                    type="range"
                    min="0.5"
                    max="2.0"
                    step="0.05"
                    value={sensitivity}
                    onChange={(e) => setSensitivity(parseFloat(e.target.value))}
                    className="flex-1 accent-amber-500 h-1.5 cursor-pointer bg-neutral-800 rounded-lg"
                  />
                  <span className="text-[9px] font-mono text-amber-400 font-bold tabular-nums">
                    {sensitivity.toFixed(2)}x
                  </span>
                </div>
              </div>

              {/* ------------ CENTER CONSOLE: ROCKERS, START BUTTON & ORANGE LCD ------------ */}
              <div className="relative h-full flex flex-col items-center justify-between flex-1 max-w-[34%] px-2 py-1 z-20">
                {/* 1. Top Rocker Switches Row (Headlights, Hazard, Wipers, Diff Lock, Beacon, Cruise) */}
                <div className="grid grid-cols-6 gap-1 w-full">
                  {/* Headlights 3-state */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, headlights: (prev.headlights + 1) % 3 }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.headlights > 0
                        ? 'bg-[#222838] border-cyan-500/80 text-cyan-400'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-cyan-400 mb-1" style={{ opacity: state.headlights > 0 ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">
                      {state.headlights === 0 ? 'LIGHTS' : state.headlights === 1 ? 'LOW' : 'HIGH'}
                    </span>
                  </button>

                  {/* High Beam */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, headlights: prev.headlights === 2 ? 1 : 2 }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.headlights === 2
                        ? 'bg-[#222838] border-cyan-400 text-cyan-300'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-cyan-400 mb-1" style={{ opacity: state.headlights === 2 ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">BEAM</span>
                  </button>

                  {/* Beacon Light */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, beacon: !prev.beacon }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.beacon
                        ? 'bg-[#2f271a] border-amber-500 text-amber-400'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-amber-400 mb-1" style={{ opacity: state.beacon ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">BEACON</span>
                  </button>

                  {/* Wipers */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, wipers: (prev.wipers + 1) % 4 }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.wipers > 0
                        ? 'bg-[#222838] border-amber-500/80 text-amber-400'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-amber-400 mb-1" style={{ opacity: state.wipers > 0 ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">
                      {state.wipers === 0 ? 'WIPER' : `WIP:${state.wipers}`}
                    </span>
                  </button>

                  {/* Differential Lock */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, diffLock: !prev.diffLock }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.diffLock
                        ? 'bg-[#2e201b] border-orange-500 text-orange-400'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-orange-400 mb-1" style={{ opacity: state.diffLock ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">DIFF</span>
                  </button>

                  {/* Cruise Control */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, cruiseControl: !prev.cruiseControl }));
                      soundManager.playTick();
                    }}
                    className={`h-12 rounded-lg border flex flex-col items-center justify-center p-0.5 transition-all ${
                      state.cruiseControl
                        ? 'bg-[#1b2b22] border-emerald-500 text-emerald-400'
                        : 'bg-[#181a24] border-[#292e3d] text-neutral-500'
                    }`}
                  >
                    <div className="w-4 h-1 rounded-full bg-emerald-400 mb-1" style={{ opacity: state.cruiseControl ? 1 : 0.2 }} />
                    <span className="text-[8px] font-mono font-bold leading-tight">CRUISE</span>
                  </button>
                </div>

                {/* 2. Middle Action Controls: Red Engine Start Push Button & Hazard Warning */}
                <div className="flex items-center justify-center gap-6 my-1">
                  {/* Big Red Engine Start/Stop Pushbutton (Exact style from Image 2 & 4) */}
                  <button
                    onClick={() => {
                      const nextState = !state.engineStarted;
                      setState((prev) => ({ ...prev, engineStarted: nextState }));
                      if (nextState) soundManager.playEngineStart();
                      else soundManager.playTick();
                    }}
                    className={`relative w-16 sm:w-18 h-16 sm:h-18 rounded-full border-4 flex flex-col items-center justify-center shadow-2xl active:scale-95 transition-all ${
                      state.engineStarted
                        ? 'bg-gradient-to-b from-[#8f1d1d] to-[#450a0a] border-[#e11d48] shadow-[0_0_25px_rgba(225,29,72,0.5)]'
                        : 'bg-gradient-to-b from-[#3a1515] to-[#1c0b0b] border-[#5e2020]'
                    }`}
                  >
                    <Power className={`w-5 h-5 ${state.engineStarted ? 'text-white animate-pulse' : 'text-neutral-500'}`} />
                    <span className="text-[7px] font-mono font-black text-white leading-tight mt-0.5 text-center">
                      ENGINE<br />START/STOP
                    </span>
                  </button>

                  {/* Hazard Warning Triangle Button */}
                  <button
                    onClick={() => {
                      setState((prev) => ({ ...prev, hazards: !prev.hazards }));
                      soundManager.playTick();
                    }}
                    className={`w-14 sm:w-16 h-12 rounded-xl border-2 flex flex-col items-center justify-center transition-all ${
                      state.hazards && blinkerPhase
                        ? 'bg-red-600 border-red-400 shadow-[0_0_20px_rgba(239,68,68,0.7)] text-white'
                        : 'bg-[#1b1c24] border-[#353947] text-red-500'
                    }`}
                  >
                    <span className="text-lg font-black leading-none">▲</span>
                    <span className="text-[7px] font-mono font-black mt-0.5">HAZARD</span>
                  </button>
                </div>

                {/* 3. Bottom Signature Glowing Amber LCD Truck Radio Display (Image 2, 3, 4) */}
                <div className="w-full bg-[#1b1406] border-2 border-[#3d2e12] rounded-xl p-2.5 shadow-[inset_0_2px_8px_rgba(0,0,0,0.8),0_4px_15px_rgba(245,158,11,0.15)] flex items-center justify-between gap-2">
                  {/* Left Rotary Knob */}
                  <div className="w-7 h-7 rounded-full bg-[#292215] border border-[#524124] flex items-center justify-center shadow-inner">
                    <div className="w-1 h-3 bg-[#e09b1f] rounded-full" />
                  </div>

                  {/* Glowing Amber LCD Matrix */}
                  <div className="flex-1 bg-[#261702] border border-[#6b4709] rounded p-1.5 flex flex-col items-center justify-center font-mono">
                    <div className="text-[12px] sm:text-[14px] font-black text-[#f59e0b] tracking-wider text-center drop-shadow-[0_0_6px_rgba(245,158,11,0.8)] leading-tight">
                      [ 8 ALBI L18 ] · CH 19
                    </div>
                    <div className="w-full flex items-center justify-between text-[9px] text-[#f59e0b]/90 font-bold pt-0.5 px-1 border-t border-[#6b4709]/50 mt-0.5">
                      <span>SPD: {String(simulatedSpeed).padStart(3, '0')} KM/H</span>
                      <span>GEAR: {state.gear === -1 ? 'R' : state.gear === 0 ? 'N' : `D${state.gear}`}</span>
                      <span>8ms UDP</span>
                    </div>
                  </div>

                  {/* Right Rotary Knob */}
                  <div className="w-7 h-7 rounded-full bg-[#292215] border border-[#524124] flex items-center justify-center shadow-inner">
                    <div className="w-1 h-3 bg-[#e09b1f] rounded-full" />
                  </div>
                </div>
              </div>

              {/* ------------ RIGHT THUMB ZONE: PEDALS & ROTARY GEAR KNOB ------------ */}
              <div className="relative h-full flex flex-col items-center justify-between flex-1 max-w-[34%] z-10">
                {/* Top Gear Knob & Camera Angles */}
                <div className="w-full flex items-center justify-between px-2 gap-2">
                  {/* Rotary Gear Selector (P - R - N - D) */}
                  <div className="flex items-center gap-1 bg-[#181a24] border border-[#2b3040] p-1 rounded-xl">
                    {[-1, 0, 1, 2, 3, 4, 5, 6].slice(0, 4).map((g) => {
                      const label = g === -1 ? 'R' : g === 0 ? 'N' : g === 1 ? 'D' : 'M';
                      const isSelected = (g === -1 && state.gear === -1) ||
                                         (g === 0 && state.gear === 0) ||
                                         (g === 1 && state.gear >= 1);
                      return (
                        <button
                          key={g}
                          onClick={() => {
                            setState((prev) => ({ ...prev, gear: g }));
                            soundManager.playGearShift();
                          }}
                          className={`w-7 sm:w-8 h-7 sm:h-8 rounded-lg font-mono font-black text-xs transition-all ${
                            isSelected
                              ? 'bg-amber-500 text-neutral-950 shadow-md font-black'
                              : 'text-neutral-400 hover:text-white bg-[#101217]'
                          }`}
                        >
                          {label}
                        </button>
                      );
                    })}
                  </div>

                  {/* Air Brake / Handbrake Switch */}
                  <button
                    onClick={() => {
                      const next = state.handbrake > 0 ? 0 : 1;
                      setState((prev) => ({ ...prev, handbrake: next }));
                      soundManager.playAirBrakeHiss();
                    }}
                    className={`px-3 py-1.5 rounded-xl border text-[10px] font-mono font-black transition-all flex items-center gap-1.5 ${
                      state.handbrake > 0
                        ? 'bg-amber-500 text-neutral-950 border-amber-400 shadow-md'
                        : 'bg-[#181a24] border-[#2c3240] text-neutral-400 hover:text-white'
                    }`}
                  >
                    <span>(P)</span>
                    <span>{state.handbrake > 0 ? 'BRAKE ENGAGED' : 'E-BRAKE'}</span>
                  </button>
                </div>

                {/* Realistic Vertical Pedals: Brake & Accelerator */}
                <div className="w-full flex-1 flex items-end justify-center gap-3 sm:gap-6 px-4 py-1">
                  {/* Brake Pedal (Wide, rubber grip studs) */}
                  <div className="flex flex-col items-center flex-1 max-w-[130px] h-full justify-end">
                    <span className="font-mono text-[10px] font-black text-red-400 mb-1">
                      BRAKE {Math.round(state.brake * 100)}%
                    </span>
                    <div
                      onMouseDown={(e) => handlePedalTouch('brake', e)}
                      onMouseUp={() => handlePedalRelease('brake')}
                      onMouseLeave={() => handlePedalRelease('brake')}
                      onTouchStart={(e) => handlePedalTouch('brake', e)}
                      onTouchEnd={() => handlePedalRelease('brake')}
                      onTouchMove={(e) => handlePedalTouch('brake', e)}
                      className="relative w-full h-[82%] rounded-2xl bg-gradient-to-b from-[#2a2e3b] to-[#171a22] border-2 border-[#434b5f] p-2 flex flex-col justify-end shadow-2xl cursor-pointer active:scale-[0.98] transition-transform overflow-hidden"
                    >
                      {/* Active Red Fill Level */}
                      <div
                        className="absolute bottom-0 left-0 right-0 bg-red-600/60 rounded-b-xl transition-all duration-75"
                        style={{ height: `${state.brake * 100}%` }}
                      />

                      {/* Anti-slip rubber studs grid */}
                      <div className="absolute inset-0 flex flex-col justify-around py-3 px-3 pointer-events-none opacity-40">
                        {[...Array(5)].map((_, i) => (
                          <div key={i} className="flex justify-around">
                            <span className="w-2.5 h-2.5 rounded-full bg-black shadow-inner" />
                            <span className="w-2.5 h-2.5 rounded-full bg-black shadow-inner" />
                            <span className="w-2.5 h-2.5 rounded-full bg-black shadow-inner" />
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  {/* Accelerator Pedal (Tall, metallic finish with vertical slits) */}
                  <div className="flex flex-col items-center flex-1 max-w-[110px] h-full justify-end">
                    <span className="font-mono text-[10px] font-black text-emerald-400 mb-1">
                      GAS {Math.round(state.throttle * 100)}%
                    </span>
                    <div
                      onMouseDown={(e) => handlePedalTouch('throttle', e)}
                      onMouseUp={() => handlePedalRelease('throttle')}
                      onMouseLeave={() => handlePedalRelease('throttle')}
                      onTouchStart={(e) => handlePedalTouch('throttle', e)}
                      onTouchEnd={() => handlePedalRelease('throttle')}
                      onTouchMove={(e) => handlePedalTouch('throttle', e)}
                      className="relative w-full h-[92%] rounded-2xl bg-gradient-to-b from-[#313645] to-[#181a24] border-2 border-[#485269] p-2 flex flex-col justify-end shadow-2xl cursor-pointer active:scale-[0.98] transition-transform overflow-hidden"
                    >
                      {/* Active Emerald Fill Level */}
                      <div
                        className="absolute bottom-0 left-0 right-0 bg-emerald-500/60 rounded-b-xl transition-all duration-75"
                        style={{ height: `${state.throttle * 100}%` }}
                      />

                      {/* Grip slits */}
                      <div className="absolute inset-0 flex flex-col justify-around py-4 px-2 pointer-events-none opacity-30">
                        {[...Array(7)].map((_, i) => (
                          <div key={i} className="w-4/5 mx-auto h-1.5 bg-black rounded-full shadow-inner" />
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* =========================================================================
              TEMPLATE 3: GT SIM RACING (CENTER WHEEL RIG - IMAGE 3 BOTTOM-LEFT)
             ========================================================================= */}
          {template === 'GT_RACING' && (
            <div className="w-full h-full flex items-center justify-between px-4">
              {/* Left Pedals: Clutch & Loadcell Brake */}
              <div className="h-full flex items-end gap-3 flex-1 max-w-[28%] py-2">
                {/* Clutch */}
                <div
                  onMouseDown={(e) => handlePedalTouch('clutch', e)}
                  onMouseUp={() => handlePedalRelease('clutch')}
                  onTouchStart={(e) => handlePedalTouch('clutch', e)}
                  onTouchEnd={() => handlePedalRelease('clutch')}
                  className="flex-1 h-[75%] rounded-xl bg-[#1e222d] border border-[#373e52] relative p-1 cursor-pointer overflow-hidden flex flex-col justify-end"
                >
                  <div className="w-full bg-cyan-500/60 transition-all" style={{ height: `${state.clutch * 100}%` }} />
                  <span className="absolute top-2 left-2 text-[9px] font-mono font-bold text-cyan-400">CLUTCH</span>
                </div>

                {/* Brake */}
                <div
                  onMouseDown={(e) => handlePedalTouch('brake', e)}
                  onMouseUp={() => handlePedalRelease('brake')}
                  onTouchStart={(e) => handlePedalTouch('brake', e)}
                  onTouchEnd={() => handlePedalRelease('brake')}
                  className="flex-1 h-[85%] rounded-xl bg-[#242938] border-2 border-[#454e66] relative p-1 cursor-pointer overflow-hidden flex flex-col justify-end"
                >
                  <div className="w-full bg-red-500/70 transition-all" style={{ height: `${state.brake * 100}%` }} />
                  <span className="absolute top-2 left-2 text-[10px] font-mono font-bold text-red-400">BRAKE</span>
                </div>
              </div>

              {/* Center: Prominent GT Racing Steering Wheel with Dual Paddle Shifters */}
              <div className="h-full flex-1 flex flex-col items-center justify-center max-w-[44%]">
                <div className="relative w-full aspect-square max-h-[78vh] flex items-center justify-center">
                  {/* Left Paddle (-) */}
                  <button
                    onClick={() => {
                      soundManager.playPaddleClick();
                      if (state.gear > -1) setState((prev) => ({ ...prev, gear: prev.gear - 1 }));
                    }}
                    className="absolute left-2 top-1/2 -translate-y-1/2 w-8 h-36 rounded-l-2xl bg-[#222838] border border-[#3b455e] flex flex-col items-center justify-center z-0 active:scale-95"
                  >
                    <span className="font-mono text-lg font-black text-white">−</span>
                  </button>

                  {/* Wheel */}
                  <div
                    ref={wheelRef}
                    onMouseDown={handleWheelStart}
                    onTouchStart={handleWheelStart}
                    className="relative w-[82%] h-[82%] rounded-full cursor-grab active:cursor-grabbing touch-none select-none z-10"
                    style={{
                      transform: `rotate(${wheelAngle}deg)`,
                      transition: isDraggingWheel.current ? 'none' : 'transform 0.15s ease-out'
                    }}
                  >
                    <div className="absolute inset-0 rounded-full border-[22px] border-[#222838] shadow-2xl flex items-center justify-center">
                      <div className="absolute -top-[22px] left-1/2 -translate-x-1/2 w-4 h-[22px] bg-red-500 rounded-t" />
                      <div className="w-24 h-24 rounded-full bg-[#181d28] border-2 border-amber-500 flex flex-col items-center justify-center">
                        <span className="font-mono text-xs font-black text-amber-400">GT CORSA</span>
                        <span className="text-[8px] font-mono text-neutral-400">{Math.round(wheelAngle)}°</span>
                      </div>
                    </div>
                  </div>

                  {/* Right Paddle (+) */}
                  <button
                    onClick={() => {
                      soundManager.playPaddleClick();
                      if (state.gear < 6) setState((prev) => ({ ...prev, gear: prev.gear + 1 }));
                    }}
                    className="absolute right-2 top-1/2 -translate-y-1/2 w-8 h-36 rounded-r-2xl bg-[#222838] border border-[#3b455e] flex flex-col items-center justify-center z-0 active:scale-95"
                  >
                    <span className="font-mono text-lg font-black text-white">+</span>
                  </button>
                </div>
              </div>

              {/* Right: Throttle Pedal & Handbrake */}
              <div className="h-full flex items-end gap-3 flex-1 max-w-[28%] py-2">
                <div
                  onMouseDown={(e) => handlePedalTouch('throttle', e)}
                  onMouseUp={() => handlePedalRelease('throttle')}
                  onTouchStart={(e) => handlePedalTouch('throttle', e)}
                  onTouchEnd={() => handlePedalRelease('throttle')}
                  className="flex-1 h-[90%] rounded-xl bg-[#282e3f] border-2 border-[#4b5570] relative p-1 cursor-pointer overflow-hidden flex flex-col justify-end"
                >
                  <div className="w-full bg-emerald-500/70 transition-all" style={{ height: `${state.throttle * 100}%` }} />
                  <span className="absolute top-2 left-2 text-[10px] font-mono font-bold text-emerald-400">THROTTLE</span>
                </div>
              </div>
            </div>
          )}

          {/* =========================================================================
              TEMPLATE 4: PRO GAMEPAD / XINPUT (IMAGE 3 TOP-RIGHT)
             ========================================================================= */}
          {template === 'PRO_GAMEPAD' && (
            <div className="w-full h-full flex items-center justify-between px-6 py-2">
              {/* Left: L1/L2 Triggers + D-Pad + Left Thumbstick */}
              <div className="flex flex-col items-center justify-between h-full py-2">
                <div className="flex gap-2 w-full">
                  <button className="flex-1 py-2 bg-[#202534] border border-[#363f57] rounded-lg font-mono text-xs font-bold text-neutral-300">
                    L1
                  </button>
                  <button className="flex-1 py-2 bg-[#202534] border border-[#363f57] rounded-lg font-mono text-xs font-bold text-neutral-300">
                    L2
                  </button>
                </div>

                {/* Directional Pad */}
                <div className="relative w-36 h-36 flex items-center justify-center">
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, dpadUp: true }))}
                    onMouseUp={() => setState((prev) => ({ ...prev, dpadUp: false }))}
                    className="absolute top-0 w-11 h-11 bg-[#232938] border border-[#38425a] rounded-t-lg font-bold text-amber-400 flex items-center justify-center active:bg-amber-500 active:text-black"
                  >
                    ▲
                  </button>
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, dpadLeft: true }))}
                    onMouseUp={() => setState((prev) => ({ ...prev, dpadLeft: false }))}
                    className="absolute left-0 w-11 h-11 bg-[#232938] border border-[#38425a] rounded-l-lg font-bold text-amber-400 flex items-center justify-center active:bg-amber-500 active:text-black"
                  >
                    ◀
                  </button>
                  <div className="w-11 h-11 bg-[#181d28]" />
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, dpadRight: true }))}
                    onMouseUp={() => setState((prev) => ({ ...prev, dpadRight: false }))}
                    className="absolute right-0 w-11 h-11 bg-[#232938] border border-[#38425a] rounded-r-lg font-bold text-amber-400 flex items-center justify-center active:bg-amber-500 active:text-black"
                  >
                    ▶
                  </button>
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, dpadDown: true }))}
                    onMouseUp={() => setState((prev) => ({ ...prev, dpadDown: false }))}
                    className="absolute bottom-0 w-11 h-11 bg-[#232938] border border-[#38425a] rounded-b-lg font-bold text-amber-400 flex items-center justify-center active:bg-amber-500 active:text-black"
                  >
                    ▼
                  </button>
                </div>

                <span className="text-[10px] font-mono text-neutral-500">DIGITAL D-PAD</span>
              </div>

              {/* Center: Status & Quick Menu */}
              <div className="flex flex-col items-center justify-center gap-3">
                <div className="w-16 h-16 rounded-full bg-[#202534] border-2 border-emerald-500/50 flex items-center justify-center shadow-lg">
                  <span className="font-mono text-xs font-black text-emerald-400">XBOX</span>
                </div>
                <div className="text-[10px] font-mono text-neutral-400 text-center">
                  XINPUT VIRTUAL CONTROLLER
                  <br />
                  <span className="text-amber-400 font-bold">&lt;8ms ULTRA LOW LATENCY</span>
                </div>
              </div>

              {/* Right: R1/R2 Triggers + Triangle/Square/Circle/Cross (A/B/X/Y) */}
              <div className="flex flex-col items-center justify-between h-full py-2">
                <div className="flex gap-2 w-full">
                  <button className="flex-1 py-2 bg-[#202534] border border-[#363f57] rounded-lg font-mono text-xs font-bold text-neutral-300">
                    R1
                  </button>
                  <button className="flex-1 py-2 bg-[#202534] border border-[#363f57] rounded-lg font-mono text-xs font-bold text-neutral-300">
                    R2
                  </button>
                </div>

                {/* Face Buttons */}
                <div className="relative w-36 h-36 flex items-center justify-center">
                  {/* Top: Y / Triangle */}
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, headlights: 1 }))}
                    className="absolute top-0 w-11 h-11 rounded-full bg-[#252b3b] border border-[#3a445e] font-mono font-black text-sm text-yellow-400 flex items-center justify-center active:bg-yellow-400 active:text-black shadow-md"
                  >
                    Y
                  </button>
                  {/* Left: X / Square */}
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, wipers: 1 }))}
                    className="absolute left-0 w-11 h-11 rounded-full bg-[#252b3b] border border-[#3a445e] font-mono font-black text-sm text-blue-400 flex items-center justify-center active:bg-blue-400 active:text-black shadow-md"
                  >
                    X
                  </button>
                  {/* Right: B / Circle */}
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, handbrake: 1 }))}
                    className="absolute right-0 w-11 h-11 rounded-full bg-[#252b3b] border border-[#3a445e] font-mono font-black text-sm text-red-400 flex items-center justify-center active:bg-red-400 active:text-black shadow-md"
                  >
                    B
                  </button>
                  {/* Bottom: A / Cross */}
                  <button
                    onMouseDown={() => setState((prev) => ({ ...prev, horn: true }))}
                    onMouseUp={() => setState((prev) => ({ ...prev, horn: false }))}
                    className="absolute bottom-0 w-11 h-11 rounded-full bg-[#252b3b] border border-[#3a445e] font-mono font-black text-sm text-emerald-400 flex items-center justify-center active:bg-emerald-400 active:text-black shadow-md"
                  >
                    A
                  </button>
                </div>

                <span className="text-[10px] font-mono text-neutral-500">ACTION CLUSTER</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
