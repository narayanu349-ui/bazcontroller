// TypeScript implementation of the 64-Byte BazPacket Protocol

export interface ClientControllerState {
  steering: number; // -1.0 to 1.0
  throttle: number; // 0.0 to 1.0
  brake: number; // 0.0 to 1.0
  clutch: number; // 0.0 to 1.0
  handbrake: number; // 0.0 to 1.0
  joyX: number; // -1.0 to 1.0
  joyY: number; // -1.0 to 1.0
  gear: number; // -1=R, 0=N, 1..6
  indicatorLeft: boolean;
  indicatorRight: boolean;
  hazards: boolean;
  headlights: number; // 0=Off, 1=Low, 2=High
  horn: boolean;
  wipers: number; // 0=Off, 1=Int, 2=Norm, 3=Fast
  cruiseControl: boolean;
  diffLock: boolean;
  engineStarted: boolean;
  beacon: boolean;
  retarder: number;
  camera: number; // 1..4
  dpadUp: boolean;
  dpadDown: boolean;
  dpadLeft: boolean;
  dpadRight: boolean;
  customButtons: boolean[];
}

export const BTN_MASKS = {
  DPAD_UP: 1n << 0n,
  DPAD_DOWN: 1n << 1n,
  DPAD_LEFT: 1n << 2n,
  DPAD_RIGHT: 1n << 3n,
  GEAR_R: 1n << 4n,
  GEAR_1: 1n << 5n,
  GEAR_2: 1n << 6n,
  GEAR_3: 1n << 7n,
  GEAR_4: 1n << 8n,
  GEAR_5: 1n << 9n,
  GEAR_6: 1n << 10n,
  SHIFT_UP: 1n << 11n,
  SHIFT_DOWN: 1n << 12n,
  INDICATOR_LEFT: 1n << 13n,
  INDICATOR_RIGHT: 1n << 14n,
  HAZARDS: 1n << 15n,
  HEADLIGHTS: 1n << 16n,
  HIGH_BEAM: 1n << 17n,
  HORN: 1n << 18n,
  WIPERS: 1n << 19n,
  CRUISE_SET: 1n << 20n,
  CRUISE_RES: 1n << 21n,
  CRUISE_PLUS: 1n << 22n,
  CRUISE_MINUS: 1n << 23n,
  CAM_COCKPIT: 1n << 24n,
  CAM_CHASE: 1n << 25n,
  CAM_FREE: 1n << 26n,
  CAM_TOP: 1n << 27n,
  DIFF_LOCK: 1n << 28n,
  ENGINE_START: 1n << 29n,
  BEACON: 1n << 30n,
  RETARDER: 1n << 31n,
  CUSTOM_1: 1n << 32n,
  CUSTOM_2: 1n << 33n,
  CUSTOM_3: 1n << 34n,
  CUSTOM_4: 1n << 35n
};

// IEEE 802.3 CRC32 table
const CRC_TABLE = new Uint32Array(256);
for (let i = 0; i < 256; i++) {
  let c = i;
  for (let j = 0; j < 8; j++) {
    c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
  }
  CRC_TABLE[i] = c >>> 0;
}

export function computeCrc32(data: Uint8Array, length: number): number {
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < length; i++) {
    crc = (crc >>> 8) ^ CRC_TABLE[(crc ^ data[i]) & 0xFF];
  }
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

export function serializeControllerPacket(
  state: ClientControllerState,
  sequenceNumber: number
): Uint8Array {
  const buffer = new ArrayBuffer(64);
  const view = new DataView(buffer);
  const bytes = new Uint8Array(buffer);

  // Magic 0xBA 0x5C
  view.setUint8(0, 0xBA);
  view.setUint8(1, 0x5C);
  view.setUint8(2, 0x01); // Version
  view.setUint8(3, 0x01); // PacketType: INPUT_FRAME

  // Sequence
  view.setUint32(4, sequenceNumber, true);

  // Timestamp microseconds
  const nowUs = BigInt(Math.floor(performance.now() * 1000));
  view.setBigUint64(8, nowUs, true);

  // Axes
  const steerRaw = Math.round(state.steering * 32767);
  view.setInt16(16, steerRaw, true);
  view.setUint16(18, Math.round(state.throttle * 65535), true);
  view.setUint16(20, Math.round(state.brake * 65535), true);
  view.setUint16(22, Math.round(state.clutch * 65535), true);
  view.setUint16(24, Math.round(state.handbrake * 65535), true);
  view.setInt16(26, Math.round(state.joyX * 32767), true);
  view.setInt16(28, Math.round(state.joyY * 32767), true);

  // Build Bitmask
  let b = 0n;
  if (state.dpadUp) b |= BTN_MASKS.DPAD_UP;
  if (state.dpadDown) b |= BTN_MASKS.DPAD_DOWN;
  if (state.dpadLeft) b |= BTN_MASKS.DPAD_LEFT;
  if (state.dpadRight) b |= BTN_MASKS.DPAD_RIGHT;
  if (state.gear === -1) b |= BTN_MASKS.GEAR_R;
  if (state.gear === 1) b |= BTN_MASKS.GEAR_1;
  if (state.gear === 2) b |= BTN_MASKS.GEAR_2;
  if (state.gear === 3) b |= BTN_MASKS.GEAR_3;
  if (state.gear === 4) b |= BTN_MASKS.GEAR_4;
  if (state.gear === 5) b |= BTN_MASKS.GEAR_5;
  if (state.gear === 6) b |= BTN_MASKS.GEAR_6;
  if (state.indicatorLeft) b |= BTN_MASKS.INDICATOR_LEFT;
  if (state.indicatorRight) b |= BTN_MASKS.INDICATOR_RIGHT;
  if (state.hazards) b |= BTN_MASKS.HAZARDS;
  if (state.headlights > 0) b |= BTN_MASKS.HEADLIGHTS;
  if (state.headlights === 2) b |= BTN_MASKS.HIGH_BEAM;
  if (state.horn) b |= BTN_MASKS.HORN;
  if (state.wipers > 0) b |= BTN_MASKS.WIPERS;
  if (state.cruiseControl) b |= BTN_MASKS.CRUISE_SET;
  if (state.diffLock) b |= BTN_MASKS.DIFF_LOCK;
  if (state.engineStarted) b |= BTN_MASKS.ENGINE_START;
  if (state.beacon) b |= BTN_MASKS.BEACON;

  view.setBigUint64(30, b, true);

  // Aux
  view.setInt8(38, state.gear);
  view.setUint8(39, state.wipers);
  view.setUint8(40, state.headlights);
  view.setUint8(41, state.cruiseControl ? 1 : 0);

  // Auth Tag (12 bytes)
  for (let i = 42; i < 54; i++) {
    view.setUint8(i, 0xA5); // Dummy authenticated MAC
  }

  // Checksum over bytes 0..59
  const crc = computeCrc32(bytes, 60);
  view.setUint32(60, crc, true);

  return bytes;
}
