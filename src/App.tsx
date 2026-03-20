/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Plus, 
  Monitor, 
  Gamepad2, 
  Server, 
  Terminal, 
  Cpu, 
  Mic, 
  Bolt, 
  Power, 
  Settings, 
  CheckCircle2, 
  X,
  Save,
  Info,
  Wifi
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Device, LogEntry, Screen } from './types';

// Mock Data
const INITIAL_DEVICES: Device[] = [
  { id: '1', name: 'Work Station', ip: '192.168.1.45', mac: '00:25:96:FF:FE:12', status: 'online', type: 'workstation' },
  { id: '2', name: 'Gaming PC', ip: '192.168.1.102', mac: '4A:33:C2:11:00:88', status: 'offline', type: 'gaming' },
  { id: '3', name: 'Media Server', ip: '192.168.1.10', mac: 'E4:D5:31:99:A1:BC', status: 'online', type: 'server' },
  { id: '4', name: 'Lab Node 01', ip: '10.0.0.15', mac: 'AA:BB:CC:DD:EE:FF', status: 'offline', type: 'lab' },
];

const INITIAL_LOGS: LogEntry[] = [
  { id: 'l1', timestamp: '14:22:01', message: 'INITIALIZING MAGIC PACKET BROADCAST...', type: 'info' },
  { id: 'l2', timestamp: '14:22:01', message: 'TARGET: 4A:33:C2:11:00:88 (GAMING PC)', type: 'info' },
  { id: 'l3', timestamp: '14:22:02', message: 'SEQUENCE SENT TO 192.168.1.255:9', type: 'success' },
  { id: 'l4', timestamp: '14:23:45', message: 'NODE_UP: WORK STATION DETECTED AT 192.168.1.45', type: 'action' },
  { id: 'l5', timestamp: '14:25:12', message: 'APP_ACTION: VOICE COMMAND RECEIVED - "WAKE LAB NODE"', type: 'info' },
];

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('dashboard');
  const [devices, setDevices] = useState<Device[]>(INITIAL_DEVICES);
  const [logs, setLogs] = useState<LogEntry[]>(INITIAL_LOGS);
  const [editingDevice, setEditingDevice] = useState<Device | null>(null);

  const handleWake = (device: Device) => {
    const newLog: LogEntry = {
      id: Math.random().toString(36).substr(2, 9),
      timestamp: new Date().toLocaleTimeString([], { hour12: false }),
      message: `WAKE COMMAND SENT TO ${device.name} (${device.mac})`,
      type: 'success',
      deviceId: device.id
    };
    setLogs([newLog, ...logs]);
  };

  const handleSaveDevice = (deviceData: Partial<Device>) => {
    if (editingDevice) {
      setDevices(devices.map(d => d.id === editingDevice.id ? { ...d, ...deviceData } as Device : d));
    } else {
      const newDevice: Device = {
        id: Math.random().toString(36).substr(2, 9),
        status: 'offline',
        type: 'workstation',
        ...deviceData
      } as Device;
      setDevices([...devices, newDevice]);
    }
    setEditingDevice(null);
    setCurrentScreen('dashboard');
  };

  const handleDeleteDevice = (id: string) => {
    setDevices(devices.filter(d => d.id !== id));
    setEditingDevice(null);
    setCurrentScreen('dashboard');
  };

  const handleEditClick = (device: Device) => {
    setEditingDevice(device);
    setCurrentScreen('register');
  };

  const handleAddClick = () => {
    setEditingDevice(null);
    setCurrentScreen('register');
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="fixed top-0 w-full z-50 glass-panel h-16 flex items-center justify-between px-6">
        <div className="flex items-center gap-3">
          <div className="p-1.5 bg-primary/10 rounded-lg">
            <Wifi className="w-5 h-5 text-primary" />
          </div>
          <h1 className="text-xl font-headline font-bold tracking-widest text-primary uppercase">
            KINETIC WOL
          </h1>
        </div>
        <button 
          onClick={currentScreen === 'register' ? () => setCurrentScreen('dashboard') : handleAddClick}
          className="p-2 text-on-surface-variant hover:text-primary transition-colors active:scale-95"
        >
          {currentScreen === 'register' ? <X className="w-6 h-6" /> : <Plus className="w-6 h-6" />}
        </button>
      </header>

      {/* Main Content */}
      <main className="flex-grow pt-24 pb-12 px-6 max-w-5xl mx-auto w-full">
        <AnimatePresence mode="wait">
          {currentScreen === 'dashboard' && (
            <DashboardScreen 
              devices={devices} 
              logs={logs} 
              onWake={handleWake} 
              onEdit={handleEditClick}
            />
          )}
          {currentScreen === 'register' && (
            <DeviceConfigScreen 
              device={editingDevice}
              onSave={handleSaveDevice}
              onDelete={handleDeleteDevice}
              onCancel={() => setCurrentScreen('dashboard')} 
            />
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}

function DashboardScreen({ devices, logs, onWake, onEdit }: { 
  devices: Device[], 
  logs: LogEntry[], 
  onWake: (d: Device) => void,
  onEdit: (d: Device) => void
}) {
  return (
    <motion.div 
      key="dashboard"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="space-y-12"
    >
      <section>
        <p className="font-headline text-primary uppercase tracking-widest text-xs mb-2 font-bold">Network Infrastructure</p>
        <h2 className="font-headline text-6xl md:text-8xl font-bold tracking-tight text-on-background">
          {devices.length.toString().padStart(2, '0')} <span className="text-on-surface-variant font-light">Nodes</span>
        </h2>
      </section>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {devices.map(device => (
          <div key={device.id}>
            <DeviceCard device={device} onWake={() => onWake(device)} onEdit={() => onEdit(device)} />
          </div>
        ))}
      </div>

      <section className="bg-surface-container-lowest border border-primary/10 p-8 rounded-2xl flex flex-col md:flex-row items-center gap-8">
        <div className="relative">
          <div className="w-20 h-20 rounded-full kinetic-gradient flex items-center justify-center shadow-2xl shadow-primary/40">
            <Mic className="w-10 h-10 text-on-primary-fixed fill-current" />
          </div>
          <div className="absolute -inset-2 border border-primary/20 rounded-full animate-ping"></div>
        </div>
        <div className="text-center md:text-left">
          <h4 className="font-headline text-2xl font-bold text-primary mb-2">Voice Command: App Action</h4>
          <p className="text-on-surface-variant leading-relaxed max-w-lg mb-4">
            Bypass the interface using system-level voice commands. Your devices are indexed for immediate acoustic activation.
          </p>
          <div className="bg-surface-high px-4 py-3 rounded-lg border border-white/5 inline-block">
            <code className="text-primary font-mono text-sm">"Hey Google, wake up my Gaming PC in Kinetic WOL"</code>
          </div>
        </div>
      </section>

      <section className="bg-surface/50 backdrop-blur-md rounded-2xl overflow-hidden border border-white/5">
        <div className="px-6 py-4 border-b border-white/5 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <Terminal className="w-4 h-4 text-on-surface-variant" />
            <h3 className="font-headline text-xs uppercase font-bold tracking-widest text-on-surface-variant">Live Sequence Logs</h3>
          </div>
        </div>
        <div className="p-6 font-mono text-[11px] text-on-surface-variant/60 space-y-1 max-h-40 overflow-y-auto no-scrollbar">
          {logs.slice(0, 5).map(log => (
            <p key={log.id}>
              <span className="text-primary/60">[{log.timestamp}]</span> {log.message}
            </p>
          ))}
        </div>
      </section>
    </motion.div>
  );
}

function DeviceCard({ device, onWake, onEdit }: { device: Device, onWake: () => void, onEdit: () => void }) {
  const isOnline = device.status === 'online';
  
  const getIcon = () => {
    switch (device.type) {
      case 'workstation': return <Monitor className="w-6 h-6" />;
      case 'gaming': return <Gamepad2 className="w-6 h-6" />;
      case 'server': return <Server className="w-6 h-6" />;
      case 'lab': return <Cpu className="w-6 h-6" />;
      default: return <Monitor className="w-6 h-6" />;
    }
  };

  return (
    <div className="group bg-surface hover:bg-surface-high transition-all duration-300 p-6 relative overflow-hidden flex flex-col justify-between min-h-[240px] rounded-xl border border-white/5">
      <div className={`absolute left-0 top-0 bottom-0 w-1 ${isOnline ? 'bg-tertiary' : 'bg-error'}`}></div>
      <div>
        <div className="flex justify-between items-start mb-4">
          <div className={`${isOnline ? 'text-tertiary' : 'text-on-surface-variant'}`}>{getIcon()}</div>
          <div className="flex items-center gap-2">
            <span className={`text-[10px] font-bold uppercase px-2 py-1 rounded-full ${
              isOnline ? 'bg-tertiary/10 text-tertiary' : 'bg-error/10 text-error'
            }`}>
              {device.status}
            </span>
            <button 
              onClick={(e) => { e.stopPropagation(); onEdit(); }}
              className="p-1 text-on-surface-variant hover:text-primary transition-colors"
            >
              <Settings className="w-4 h-4" />
            </button>
          </div>
        </div>
        <h3 className="font-headline text-xl font-bold text-on-surface mb-1">{device.name}</h3>
        <div className="text-[10px] text-on-surface-variant font-mono space-y-0.5 opacity-80">
          <p>IP: {device.ip}</p>
          <p>MAC: {device.mac}</p>
        </div>
      </div>
      <button 
        onClick={onWake}
        className="mt-6 w-full kinetic-gradient py-3 px-4 rounded-xl text-on-primary-fixed font-headline font-bold uppercase tracking-widest text-xs flex items-center justify-center gap-2 active:scale-95 transition-transform shadow-lg shadow-primary/20"
      >
        {isOnline ? (
          <>
            <Bolt className="w-4 h-4" />
            Pulse Signal
          </>
        ) : (
          <>
            <Power className="w-4 h-4" />
            Wake Command
          </>
        )}
      </button>
    </div>
  );
}

function DeviceConfigScreen({ device, onSave, onDelete, onCancel }: { 
  device: Device | null, 
  onSave: (data: Partial<Device>) => void,
  onDelete: (id: string) => void,
  onCancel: () => void 
}) {
  const [formData, setFormData] = useState({
    name: device?.name || '',
    mac: device?.mac || '',
    ip: device?.ip || '255.255.255.255',
    port: '9'
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <motion.div 
      key="register"
      initial={{ opacity: 0, y: 50 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: 50 }}
      className="max-w-xl mx-auto"
    >
      <div className="mb-10 flex flex-col gap-2">
        <span className="font-headline text-primary uppercase tracking-widest text-xs font-bold">Network Node Configuration</span>
        <h1 className="font-headline text-5xl font-bold text-on-surface tracking-tight">
          {device ? 'Edit Node' : 'Register Node'}
        </h1>
        <p className="text-on-surface-variant text-sm max-w-md leading-relaxed">
          {device 
            ? 'Modify the existing hardware mapping for this network infrastructure node.' 
            : 'Initialize a new Magic Packet destination by mapping its physical hardware address to your local infrastructure.'}
        </p>
      </div>

      <form className="space-y-8" onSubmit={handleSubmit}>
        <section className="bg-surface p-8 rounded-2xl relative overflow-hidden border border-white/5">
          <div className="absolute top-0 left-0 w-1 h-full bg-primary opacity-40"></div>
          <h2 className="font-headline text-lg font-bold mb-6 text-on-surface flex items-center gap-2">
            <Cpu className="w-4 h-4 text-primary" />
            Identity & Logical Mapping
          </h2>
          <div className="space-y-6">
            <div className="group">
              <label className="block text-[10px] uppercase tracking-widest text-on-surface-variant mb-2 font-bold group-focus-within:text-primary transition-colors">Device Name</label>
              <input 
                className="w-full bg-surface-high border border-white/5 rounded-xl p-4 text-on-surface placeholder:text-white/10 focus:ring-1 focus:ring-primary/40 transition-all outline-none font-sans" 
                placeholder="e.g. CORE-MAIN-WORKSTATION" 
                type="text" 
                value={formData.name}
                onChange={e => setFormData({ ...formData, name: e.target.value })}
                required
              />
            </div>
            <div className="group">
              <label className="block text-[10px] uppercase tracking-widest text-on-surface-variant mb-2 font-bold group-focus-within:text-primary transition-colors">Physical Address (MAC)</label>
              <input 
                className="w-full bg-surface-high border border-white/5 rounded-xl p-4 text-on-surface placeholder:text-white/10 focus:ring-1 focus:ring-primary/40 transition-all outline-none font-mono" 
                placeholder="00:00:00:00:00:00" 
                type="text" 
                value={formData.mac}
                onChange={e => setFormData({ ...formData, mac: e.target.value })}
                required
              />
              <div className="mt-2 flex items-center gap-2 text-[10px] text-tertiary/70 font-bold uppercase tracking-widest">
                <CheckCircle2 className="w-3 h-3" />
                Valid hex format detected: 6 octets
              </div>
            </div>
          </div>
        </section>

        <section className="bg-surface p-8 rounded-2xl border border-white/5">
          <h2 className="font-headline text-lg font-bold mb-6 text-on-surface flex items-center gap-2">
            <Wifi className="w-4 h-4 text-primary" />
            Transmission Protocol
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="md:col-span-2 group">
              <label className="block text-[10px] uppercase tracking-widest text-on-surface-variant mb-2 font-bold group-focus-within:text-primary transition-colors">IP / Broadcast</label>
              <input 
                className="w-full bg-surface-high border border-white/5 rounded-xl p-4 text-on-surface focus:ring-1 focus:ring-primary/40 transition-all outline-none font-mono" 
                type="text" 
                value={formData.ip}
                onChange={e => setFormData({ ...formData, ip: e.target.value })}
                required
              />
            </div>
            <div className="group">
              <label className="block text-[10px] uppercase tracking-widest text-on-surface-variant mb-2 font-bold group-focus-within:text-primary transition-colors">Port</label>
              <input 
                className="w-full bg-surface-high border border-white/5 rounded-xl p-4 text-on-surface focus:ring-1 focus:ring-primary/40 transition-all outline-none font-mono" 
                type="number" 
                value={formData.port}
                onChange={e => setFormData({ ...formData, port: e.target.value })}
                required
              />
            </div>
          </div>
        </section>

        <div className="p-4 rounded-xl border border-white/5 bg-surface-high/50 flex gap-4">
          <Info className="w-5 h-5 text-primary shrink-0" />
          <p className="text-xs text-on-surface-variant leading-relaxed">
            Ensure the target device has <span className="text-primary font-bold">Wake-on-LAN (WOL)</span> enabled in the BIOS/UEFI settings and the network adapter driver. Broadcast address <span className="font-mono">255.255.255.255</span> is recommended for global network visibility.
          </p>
        </div>

        <div className="flex flex-col gap-3 pt-4">
          <button type="submit" className="kinetic-gradient w-full py-4 rounded-xl font-headline font-bold text-on-primary-fixed uppercase tracking-widest active:scale-[0.98] transition-all shadow-2xl shadow-primary/20 flex items-center justify-center gap-2">
            <Save className="w-5 h-5" />
            {device ? 'Update Node' : 'Commit Configuration'}
          </button>
          
          {device && (
            <button 
              type="button"
              onClick={() => onDelete(device.id)}
              className="w-full py-4 rounded-xl font-headline font-bold text-error uppercase tracking-widest hover:bg-error/10 transition-colors flex items-center justify-center gap-2 border border-error/20"
            >
              <Power className="w-5 h-5" />
              Delete Node
            </button>
          )}

          <button 
            type="button"
            onClick={onCancel}
            className="w-full py-4 rounded-xl font-headline font-bold text-on-surface-variant uppercase tracking-widest hover:bg-white/5 transition-colors flex items-center justify-center gap-2"
          >
            Discard Changes
          </button>
        </div>
      </form>
    </motion.div>
  );
}
