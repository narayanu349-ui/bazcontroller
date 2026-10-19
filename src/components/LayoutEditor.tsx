import React, { useState, useRef } from 'react';
import {
  Plus,
  Trash2,
  Download,
  Upload,
  Save,
  Check,
  RotateCw,
  Move,
  Maximize2
} from 'lucide-react';

export interface CanvasWidget {
  id: string;
  type: string;
  title: string;
  x: number; // percentage 0..100
  y: number; // percentage 0..100
  width: number; // percentage 5..60
  height: number; // percentage 5..80
  rotation: number; // degrees 0..360
  color: string;
}

export interface StoredTemplate {
  id: string;
  name: string;
  description: string;
  isDefault: boolean;
  widgets: CanvasWidget[];
}

const DEFAULT_TEMPLATES: StoredTemplate[] = [
  {
    id: 'ets2_rig',
    name: 'Euro Truck Simulator 2 (Full Rig)',
    description: '360° wheel, H-shifter, pedals, retarder, hazards, and cruise control',
    isDefault: true,
    widgets: [
      { id: 'w1', type: 'STEERING_WHEEL', title: '360° Steering Wheel', x: 5, y: 18, width: 38, height: 68, rotation: 0, color: '#F59E0B' },
      { id: 'w2', type: 'DASHBOARD', title: 'Telemetry HUD', x: 8, y: 3, width: 32, height: 12, rotation: 0, color: '#06B6D4' },
      { id: 'w3', type: 'SWITCHES', title: 'Indicators & Lights', x: 45, y: 5, width: 22, height: 25, rotation: 0, color: '#10B981' },
      { id: 'w4', type: 'H_SHIFTER', title: '6-Speed H-Shifter', x: 45, y: 34, width: 22, height: 55, rotation: 0, color: '#F59E0B' },
      { id: 'w5', type: 'CLUTCH_PEDAL', title: 'Clutch Pedal', x: 70, y: 25, width: 8, height: 65, rotation: 0, color: '#06B6D4' },
      { id: 'w6', type: 'BRAKE_PEDAL', title: 'Brake Pedal', x: 80, y: 20, width: 9, height: 70, rotation: 0, color: '#EF4444' },
      { id: 'w7', type: 'ACCEL_PEDAL', title: 'Accelerator Pedal', x: 90, y: 15, width: 9, height: 75, rotation: 0, color: '#10B981' }
    ]
  },
  {
    id: 'ats_highway',
    name: 'American Truck Simulator (Highway Cruiser)',
    description: 'Long-haul layout with Jake brake retarder, diff lock, and dual cruise toggles',
    isDefault: true,
    widgets: [
      { id: 'a1', type: 'STEERING_WHEEL', title: '360° Steering Wheel', x: 6, y: 15, width: 42, height: 75, rotation: 0, color: '#F59E0B' },
      { id: 'a2', type: 'DASHBOARD', title: 'Highway HUD', x: 10, y: 3, width: 34, height: 10, rotation: 0, color: '#06B6D4' },
      { id: 'a3', type: 'SWITCHES', title: 'Jake Brake & Diff', x: 50, y: 5, width: 20, height: 35, rotation: 0, color: '#F59E0B' },
      { id: 'a4', type: 'H_SHIFTER', title: 'Sequential Shifter', x: 50, y: 44, width: 20, height: 45, rotation: 0, color: '#06B6D4' },
      { id: 'a5', type: 'BRAKE_PEDAL', title: 'Brake Pedal', x: 75, y: 20, width: 10, height: 70, rotation: 0, color: '#EF4444' },
      { id: 'a6', type: 'ACCEL_PEDAL', title: 'Throttle Pedal', x: 87, y: 15, width: 10, height: 75, rotation: 0, color: '#10B981' }
    ]
  },
  {
    id: 'gt_racing',
    name: 'GT Racing & Assetto Corsa',
    description: 'Paddle sequential, direct pedals, D-pad pitstop menu, and quick camera angle keys',
    isDefault: true,
    widgets: [
      { id: 'g1', type: 'STEERING_WHEEL', title: 'GT Wheel Rim', x: 15, y: 10, width: 45, height: 80, rotation: 0, color: '#F59E0B' },
      { id: 'g2', type: 'DPAD', title: 'Pitstop D-Pad', x: 2, y: 25, width: 12, height: 45, rotation: 0, color: '#06B6D4' },
      { id: 'g3', type: 'H_SHIFTER', title: 'Paddles / Seq', x: 63, y: 10, width: 16, height: 80, rotation: 0, color: '#F59E0B' },
      { id: 'g4', type: 'BRAKE_PEDAL', title: 'Loadcell Brake', x: 81, y: 15, width: 8, height: 75, rotation: 0, color: '#EF4444' },
      { id: 'g5', type: 'ACCEL_PEDAL', title: 'Throttle', x: 90, y: 10, width: 8, height: 80, rotation: 0, color: '#10B981' }
    ]
  }
];

export const LayoutEditor: React.FC = () => {
  const [templates, setTemplates] = useState<StoredTemplate[]>(() => {
    try {
      const saved = localStorage.getItem('baz_user_templates');
      if (saved) {
        const parsed = JSON.parse(saved);
        return [...DEFAULT_TEMPLATES, ...parsed];
      }
    } catch { }
    return DEFAULT_TEMPLATES;
  });

  const [activeTemplateId, setActiveTemplateId] = useState<string>(DEFAULT_TEMPLATES[0].id);
  const activeTemplate = templates.find((t) => t.id === activeTemplateId) || templates[0];

  const [widgets, setWidgets] = useState<CanvasWidget[]>(activeTemplate.widgets);
  const [selectedWidgetId, setSelectedWidgetId] = useState<string | null>(widgets[0]?.id || null);

  const [templateName, setTemplateName] = useState(activeTemplate.name);
  const [showJsonModal, setShowJsonModal] = useState(false);
  const [jsonText, setJsonText] = useState('');
  const [isImportMode, setIsImportMode] = useState(false);
  const [saveToast, setSaveToast] = useState(false);

  const selectedWidget = widgets.find((w) => w.id === selectedWidgetId);

  // Switch template
  const handleSelectTemplate = (template: StoredTemplate) => {
    setActiveTemplateId(template.id);
    setTemplateName(template.name);
    setWidgets(template.widgets.map((w) => ({ ...w })));
    setSelectedWidgetId(template.widgets[0]?.id || null);
  };

  // Dragging in Canvas
  const canvasRef = useRef<HTMLDivElement>(null);
  const draggingId = useRef<string | null>(null);
  const dragStart = useRef<{ mouseX: number; mouseY: number; elemX: number; elemY: number }>({
    mouseX: 0,
    mouseY: 0,
    elemX: 0,
    elemY: 0
  });

  const handleWidgetMouseDown = (e: React.MouseEvent, widget: CanvasWidget) => {
    e.stopPropagation();
    setSelectedWidgetId(widget.id);
    draggingId.current = widget.id;
    dragStart.current = {
      mouseX: e.clientX,
      mouseY: e.clientY,
      elemX: widget.x,
      elemY: widget.y
    };
  };

  const handleCanvasMouseMove = (e: React.MouseEvent) => {
    if (!draggingId.current || !canvasRef.current) return;
    const rect = canvasRef.current.getBoundingClientRect();
    const deltaX = ((e.clientX - dragStart.current.mouseX) / rect.width) * 100;
    const deltaY = ((e.clientY - dragStart.current.mouseY) / rect.height) * 100;

    const newX = Math.max(0, Math.min(90, dragStart.current.elemX + deltaX));
    const newY = Math.max(0, Math.min(90, dragStart.current.elemY + deltaY));

    setWidgets((prev) =>
      prev.map((w) => (w.id === draggingId.current ? { ...w, x: Math.round(newX), y: Math.round(newY) } : w))
    );
  };

  const handleCanvasMouseUp = () => {
    draggingId.current = null;
  };

  // Add new widget
  const handleAddWidget = (type: string, title: string, color: string) => {
    const newW: CanvasWidget = {
      id: `w_${Date.now()}`,
      type,
      title,
      x: 35,
      y: 35,
      width: 18,
      height: 25,
      rotation: 0,
      color
    };
    setWidgets((prev) => [...prev, newW]);
    setSelectedWidgetId(newW.id);
  };

  // Save template to localStorage
  const handleSaveTemplate = () => {
    const updatedTemplate: StoredTemplate = {
      id: activeTemplate.isDefault ? `custom_${Date.now()}` : activeTemplate.id,
      name: templateName.includes('(Custom)') ? templateName : `${templateName} (Custom)`,
      description: `Custom layout containing ${widgets.length} gaming controls`,
      isDefault: false,
      widgets
    };

    const userTemplates = templates.filter((t) => !t.isDefault && t.id !== updatedTemplate.id);
    const newTemplatesList = [...userTemplates, updatedTemplate];
    localStorage.setItem('baz_user_templates', JSON.stringify(newTemplatesList));

    setTemplates([...DEFAULT_TEMPLATES, ...newTemplatesList]);
    setActiveTemplateId(updatedTemplate.id);
    setSaveToast(true);
    setTimeout(() => setSaveToast(false), 2000);
  };

  // Export JSON
  const handleExportJson = () => {
    const exportObj = {
      name: templateName,
      createdAt: new Date().toISOString(),
      widgets
    };
    setJsonText(JSON.stringify(exportObj, null, 2));
    setIsImportMode(false);
    setShowJsonModal(true);
  };

  // Import JSON
  const handleImportJson = () => {
    try {
      const parsed = JSON.parse(jsonText);
      if (parsed && Array.isArray(parsed.widgets)) {
        setWidgets(parsed.widgets);
        if (parsed.name) setTemplateName(`${parsed.name} (Imported)`);
        setShowJsonModal(false);
        setSaveToast(true);
        setTimeout(() => setSaveToast(false), 2000);
      } else {
        alert('Invalid JSON layout format: missing "widgets" array.');
      }
    } catch {
      alert('Failed to parse JSON string.');
    }
  };

  return (
    <div className="w-full max-w-7xl mx-auto flex flex-col gap-4 p-4 lg:p-6">
      {/* Top Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-neutral-900 border border-neutral-800 rounded-xl px-5 py-3">
        <div className="flex items-center gap-3">
          <input
            type="text"
            value={templateName}
            onChange={(e) => setTemplateName(e.target.value)}
            className="bg-neutral-950 border border-neutral-800 rounded px-2.5 py-1 text-sm font-mono font-bold text-amber-400 focus:outline-none focus:border-amber-500 max-w-xs"
          />
          <span className="text-xs text-neutral-500">
            ({widgets.length} elements · Drag &amp; rotate enabled)
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleExportJson}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-800 bg-neutral-950 text-neutral-300 hover:text-white text-xs font-mono transition-colors"
          >
            <Download className="w-3.5 h-3.5" />
            Export JSON
          </button>

          <button
            onClick={() => {
              setJsonText('');
              setIsImportMode(true);
              setShowJsonModal(true);
            }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-neutral-800 bg-neutral-950 text-neutral-300 hover:text-white text-xs font-mono transition-colors"
          >
            <Upload className="w-3.5 h-3.5" />
            Import JSON
          </button>

          <button
            onClick={handleSaveTemplate}
            className="flex items-center gap-1.5 px-4 py-1.5 rounded-lg bg-amber-500 hover:bg-amber-400 text-neutral-950 font-mono font-bold text-xs transition-colors"
          >
            <Save className="w-3.5 h-3.5" />
            Save Layout
          </button>
        </div>
      </div>

      {/* Main Workspace: Template Library, Interactive Canvas & Inspector */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
        {/* Left Column: Template Switcher & Quick Add Buttons (3 cols) */}
        <div className="lg:col-span-3 flex flex-col gap-3">
          {/* Presets List */}
          <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4 flex flex-col gap-2">
            <span className="font-mono text-xs font-bold text-neutral-300 mb-1">PRESETS &amp; SAVED</span>
            {templates.map((tpl) => (
              <button
                key={tpl.id}
                onClick={() => handleSelectTemplate(tpl)}
                className={`w-full text-left p-2.5 rounded-lg border transition-all ${
                  tpl.id === activeTemplateId
                    ? 'bg-neutral-800 border-amber-500/60 text-white'
                    : 'bg-neutral-950 border-neutral-800 text-neutral-400 hover:text-white hover:border-neutral-700'
                }`}
              >
                <div className="text-xs font-bold truncate">{tpl.name}</div>
                <div className="text-[10px] text-neutral-500 truncate">{tpl.description}</div>
              </button>
            ))}
          </div>

          {/* Add Control Buttons */}
          <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-4 flex flex-col gap-2">
            <span className="font-mono text-xs font-bold text-neutral-300 mb-1">+ ADD CONTROL WIDGET</span>
            <div className="grid grid-cols-2 gap-2 text-xs font-mono">
              <button
                onClick={() => handleAddWidget('STEERING_WHEEL', 'Steering Wheel', '#F59E0B')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + Wheel
              </button>
              <button
                onClick={() => handleAddWidget('H_SHIFTER', 'H-Shifter Gate', '#F59E0B')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + Shifter
              </button>
              <button
                onClick={() => handleAddWidget('ACCEL_PEDAL', 'Throttle Pedal', '#10B981')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + Throttle
              </button>
              <button
                onClick={() => handleAddWidget('BRAKE_PEDAL', 'Brake Pedal', '#EF4444')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + Brake
              </button>
              <button
                onClick={() => handleAddWidget('SWITCHES', 'Accessory Toggles', '#06B6D4')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + Toggles
              </button>
              <button
                onClick={() => handleAddWidget('DPAD', 'Pit D-Pad', '#06B6D4')}
                className="p-2 bg-neutral-950 hover:bg-neutral-800 border border-neutral-800 rounded text-left text-neutral-300"
              >
                + D-Pad
              </button>
            </div>
          </div>
        </div>

        {/* Center Column: Drag-and-Drop Canvas (6 cols) */}
        <div className="lg:col-span-6 flex flex-col bg-neutral-900 border border-neutral-800 rounded-2xl p-4">
          <div className="flex items-center justify-between pb-2 mb-2 border-b border-neutral-800">
            <span className="font-mono text-xs font-bold text-neutral-300">LANDSCAPE COCKPIT CANVAS (16:9)</span>
            <span className="text-[10px] font-mono text-neutral-500">CLICK &amp; DRAG CONTROLS</span>
          </div>

          <div
            ref={canvasRef}
            onMouseMove={handleCanvasMouseMove}
            onMouseUp={handleCanvasMouseUp}
            className="relative w-full aspect-[16/9] bg-neutral-950 border border-neutral-800 rounded-xl overflow-hidden select-none"
          >
            {/* Grid Lines Pattern */}
            <div
              className="absolute inset-0 opacity-10 pointer-events-none"
              style={{
                backgroundImage: 'radial-gradient(circle, #ffffff 1px, transparent 1px)',
                backgroundSize: '24px 24px'
              }}
            />

            {/* Widgets on Canvas */}
            {widgets.map((w) => {
              const isSelected = w.id === selectedWidgetId;
              return (
                <div
                  key={w.id}
                  onMouseDown={(e) => handleWidgetMouseDown(e, w)}
                  className={`absolute rounded-lg border cursor-move transition-shadow flex flex-col items-center justify-center p-1.5 ${
                    isSelected
                      ? 'border-amber-400 bg-neutral-800/90 shadow-xl shadow-amber-500/20 z-20'
                      : 'border-neutral-700 bg-neutral-900/80 hover:border-neutral-500 z-10'
                  }`}
                  style={{
                    left: `${w.x}%`,
                    top: `${w.y}%`,
                    width: `${w.width}%`,
                    height: `${w.height}%`,
                    transform: `rotate(${w.rotation}deg)`
                  }}
                >
                  <span className="font-mono text-[10px] font-bold text-center leading-tight truncate w-full px-1" style={{ color: w.color }}>
                    {w.title}
                  </span>
                  <span className="text-[8px] font-mono text-neutral-500">
                    {w.width}x{w.height}%
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Widget Inspector (Resize, Rotate, Title, Delete) (3 cols) */}
        <div className="lg:col-span-3 flex flex-col bg-neutral-900 border border-neutral-800 rounded-2xl p-4 gap-4">
          <span className="font-mono text-xs font-bold text-neutral-300">CONTROL PROPERTIES</span>

          {selectedWidget ? (
            <div className="flex flex-col gap-3 font-mono text-xs">
              <div>
                <label className="text-[10px] text-neutral-500 block mb-1">LABEL</label>
                <input
                  type="text"
                  value={selectedWidget.title}
                  onChange={(e) => {
                    const val = e.target.value;
                    setWidgets((prev) =>
                      prev.map((w) => (w.id === selectedWidget.id ? { ...w, title: val } : w))
                    );
                  }}
                  className="w-full bg-neutral-950 border border-neutral-800 rounded p-2 text-white focus:outline-none focus:border-amber-500"
                />
              </div>

              {/* Width Slider */}
              <div>
                <div className="flex justify-between text-[10px] text-neutral-500 mb-1">
                  <span>WIDTH</span>
                  <span>{selectedWidget.width}%</span>
                </div>
                <input
                  type="range"
                  min="5"
                  max="60"
                  value={selectedWidget.width}
                  onChange={(e) => {
                    const val = parseInt(e.target.value);
                    setWidgets((prev) =>
                      prev.map((w) => (w.id === selectedWidget.id ? { ...w, width: val } : w))
                    );
                  }}
                  className="w-full accent-amber-500"
                />
              </div>

              {/* Height Slider */}
              <div>
                <div className="flex justify-between text-[10px] text-neutral-500 mb-1">
                  <span>HEIGHT</span>
                  <span>{selectedWidget.height}%</span>
                </div>
                <input
                  type="range"
                  min="5"
                  max="85"
                  value={selectedWidget.height}
                  onChange={(e) => {
                    const val = parseInt(e.target.value);
                    setWidgets((prev) =>
                      prev.map((w) => (w.id === selectedWidget.id ? { ...w, height: val } : w))
                    );
                  }}
                  className="w-full accent-amber-500"
                />
              </div>

              {/* Rotation Slider */}
              <div>
                <div className="flex justify-between text-[10px] text-neutral-500 mb-1">
                  <span>ROTATION</span>
                  <span>{selectedWidget.rotation}°</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="360"
                  value={selectedWidget.rotation}
                  onChange={(e) => {
                    const val = parseInt(e.target.value);
                    setWidgets((prev) =>
                      prev.map((w) => (w.id === selectedWidget.id ? { ...w, rotation: val } : w))
                    );
                  }}
                  className="w-full accent-cyan-500"
                />
              </div>

              {/* Delete Widget */}
              <button
                onClick={() => {
                  setWidgets((prev) => prev.filter((w) => w.id !== selectedWidget.id));
                  setSelectedWidgetId(null);
                }}
                className="mt-4 flex items-center justify-center gap-1.5 py-2 px-3 rounded bg-red-950/40 border border-red-500/40 text-red-400 hover:bg-red-900/60 font-bold transition-colors"
              >
                <Trash2 className="w-3.5 h-3.5" />
                Remove Widget
              </button>
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-center text-center p-4 text-xs font-mono text-neutral-600">
              Click any control on the canvas to inspect and edit size, angle, or title.
            </div>
          )}
        </div>
      </div>

      {/* JSON Import/Export Modal */}
      {showJsonModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl w-full max-w-lg p-5 flex flex-col gap-3">
            <span className="font-mono text-sm font-bold text-white">
              {isImportMode ? 'Import Template JSON' : 'Export Template JSON'}
            </span>

            <textarea
              value={jsonText}
              onChange={(e) => setJsonText(e.target.value)}
              readOnly={!isImportMode}
              rows={12}
              className="w-full bg-neutral-950 border border-neutral-800 rounded-lg p-3 font-mono text-xs text-neutral-300 focus:outline-none focus:border-amber-500 resize-none"
              placeholder={isImportMode ? 'Paste template JSON object here...' : ''}
            />

            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowJsonModal(false)}
                className="px-4 py-2 rounded-lg bg-neutral-800 text-neutral-300 hover:text-white text-xs font-mono"
              >
                Close
              </button>
              {isImportMode ? (
                <button
                  onClick={handleImportJson}
                  className="px-4 py-2 rounded-lg bg-amber-500 text-neutral-950 font-bold text-xs font-mono hover:bg-amber-400"
                >
                  Import
                </button>
              ) : (
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(jsonText);
                    alert('Copied template JSON to clipboard!');
                  }}
                  className="px-4 py-2 rounded-lg bg-amber-500 text-neutral-950 font-bold text-xs font-mono hover:bg-amber-400"
                >
                  Copy to Clipboard
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Save Success Toast */}
      {saveToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-500 text-neutral-950 font-mono font-bold text-xs px-4 py-2.5 rounded-lg shadow-xl flex items-center gap-2">
          <Check className="w-4 h-4" /> Template Saved Successfully!
        </div>
      )}
    </div>
  );
};
