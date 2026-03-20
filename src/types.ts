export type DeviceStatus = 'online' | 'offline';

export interface Device {
  id: string;
  name: string;
  ip: string;
  mac: string;
  status: DeviceStatus;
  type: 'workstation' | 'gaming' | 'server' | 'lab' | 'iot';
}

export interface LogEntry {
  id: string;
  timestamp: string;
  message: string;
  type: 'info' | 'success' | 'error' | 'action';
  deviceId?: string;
}

export type Screen = 'dashboard' | 'register';
