const crypto = require("crypto");

const MEAL_CATEGORIES = ["breakfast", "lunch", "dinner", "beverages"];

function nowIso() {
  return new Date().toISOString();
}

function randomId(size = 8) {
  return crypto.randomBytes(size).toString("hex");
}

function defaultProperty() {
  return {
    id: `${process.env.PROPERTY_ID || "primary"}`.trim(),
    name: `${process.env.DEFAULT_PROPERTY_NAME || ""}`.trim(),
    address: `${process.env.DEFAULT_PROPERTY_ADDRESS || ""}`.trim(),
    timezone: `${process.env.DEFAULT_PROPERTY_TIMEZONE || "Asia/Dhaka"}`.trim(),
    accentColor: `${process.env.DEFAULT_ACCENT_COLOR || "#C9A84C"}`.trim(),
    environment:
      `${process.env.VERCEL_ENV || process.env.NODE_ENV || "development"}`
        .trim()
        .replace(/^./, (value) => value.toUpperCase()),
    apiKeyPreview: `${process.env.PANEL_API_KEY_PREVIEW || ""}`.trim()
  };
}

function emptyHotelBranding() {
  return {
    hotelName: "",
    shortBrand: "",
    tagline: "",
    location: "",
    supportPhone: "",
    startupLogoUrl: null,
    checkoutLabel: "Checkout",
    billLabel: "Current bill",
    billValue: "",
    loadingMessage: "Preparing your room experience"
  };
}

function emptyPopupConfig() {
  return {
    helpTitle: "",
    ratingText: "",
    callHint: "",
    callNumber: ""
  };
}

function normalizeRoom(roomNumber, source = {}, fallbackSyncTime = nowIso()) {
  const trimmedRoom = `${roomNumber || source.roomNumber || ""}`.trim();
  const guestName = `${source.guestName || ""}`.trim();
  const deviceId = `${source.deviceId || ""}`.trim();
  const archivedAt = source.archivedAt || null;
  const archivedBy = source.archivedBy || null;
  const hasSessionHistory = Boolean(source.hasSessionHistory || guestName || source.checkInAt);
  const hasBindingHistory = Boolean(source.hasBindingHistory || deviceId);
  const status = archivedAt
    ? "archived"
    : `${source.status || (guestName ? "occupied" : deviceId ? "vacant" : "unbound")}`.trim();

  return {
    roomNumber: trimmedRoom,
    floor: `${source.floor || trimmedRoom.slice(0, Math.max(1, trimmedRoom.length - 2))}`.trim(),
    status,
    guestName,
    deviceId,
    lastSyncAt: source.lastSyncAt || fallbackSyncTime,
    checkInAt: guestName ? source.checkInAt || fallbackSyncTime : source.checkInAt || "",
    welcomeNote: `${source.welcomeNote || ""}`.trim(),
    language: `${source.language || "English"}`.trim(),
    overrideEnabled: Boolean(source.overrideEnabled),
    customContentLabel: `${source.customContentLabel || ""}`.trim(),
    lastUpdatedBy: `${source.lastUpdatedBy || ""}`.trim(),
    archivedAt,
    archivedBy,
    hasSessionHistory,
    hasBindingHistory,
    createdAt: source.createdAt || fallbackSyncTime
  };
}

function normalizeMenuItem(category, item = {}) {
  return {
    id: `${item.id || `${category}-${randomId(6)}`}`.trim(),
    title: `${item.title || ""}`.trim(),
    subtitle: `${item.subtitle || ""}`.trim(),
    description: `${item.description || ""}`.trim(),
    price: `${item.price || ""}`.trim(),
    available: item.available !== false,
    badge: `${item.badge || ""}`.trim(),
    accentColor: `${item.accentColor || "#C9A84C"}`.trim(),
    updatedAt: item.updatedAt || nowIso(),
    archivedAt: item.archivedAt || null,
    archivedBy: item.archivedBy || null
  };
}

function normalizeMealCollection(source = {}) {
  const result = {};
  MEAL_CATEGORIES.forEach((category) => {
    result[category] = Array.isArray(source[category])
      ? source[category].map((item) => normalizeMenuItem(category, item))
      : [];
  });
  return result;
}

function createDefaultStore() {
  return {
    property: defaultProperty(),
    hotel: emptyHotelBranding(),
    weather: {
      temperatureC: 0,
      condition: ""
    },
    popup: emptyPopupConfig(),
    backgrounds: {
      home: [],
      roomService: [],
      foodMenu: [],
      inputs: []
    },
    meals: normalizeMealCollection(),
    sections: {},
    visibility: {
      destinations: {
        home: true,
        roomService: true,
        foodMenu: true,
        inputs: true
      },
      visibleAppPackages: [],
      visibleSourceTitles: []
    },
    availableApps: [],
    availableInputs: [],
    rooms: {},
    auditLogs: [],
    notifications: [],
    sync: {
      version: 1,
      ttlSeconds: 300,
      updatedAt: nowIso()
    }
  };
}

function normalizeStore(source = {}) {
  const defaults = createDefaultStore();
  const sync = { ...defaults.sync, ...(source.sync || {}) };
  const store = {
    ...defaults,
    ...source,
    property: { ...defaults.property, ...(source.property || {}) },
    hotel: { ...defaults.hotel, ...(source.hotel || {}) },
    weather: {
      temperatureC: Number(source.weather?.temperatureC ?? defaults.weather.temperatureC),
      condition: `${source.weather?.condition ?? defaults.weather.condition}`.trim()
    },
    popup: { ...defaults.popup, ...(source.popup || {}) },
    backgrounds: {
      ...defaults.backgrounds,
      ...(source.backgrounds || {})
    },
    meals: normalizeMealCollection(source.meals || defaults.meals),
    sections: source.sections && typeof source.sections === "object" ? source.sections : {},
    visibility: {
      ...defaults.visibility,
      ...(source.visibility || {}),
      destinations: {
        ...defaults.visibility.destinations,
        ...((source.visibility && source.visibility.destinations) || {})
      },
      visibleAppPackages: Array.isArray(source.visibility?.visibleAppPackages)
        ? source.visibility.visibleAppPackages.filter(Boolean)
        : [],
      visibleSourceTitles: Array.isArray(source.visibility?.visibleSourceTitles)
        ? source.visibility.visibleSourceTitles.filter(Boolean)
        : []
    },
    availableApps: Array.isArray(source.availableApps) ? source.availableApps : [],
    availableInputs: Array.isArray(source.availableInputs) ? source.availableInputs : [],
    rooms: {},
    auditLogs: Array.isArray(source.auditLogs) ? source.auditLogs : [],
    notifications: Array.isArray(source.notifications) ? source.notifications : [],
    sync
  };

  Object.entries(source.rooms || {}).forEach(([roomNumber, room]) => {
    store.rooms[roomNumber] = normalizeRoom(roomNumber, room, store.sync.updatedAt);
  });

  return store;
}

function pushAudit(store, payload) {
  const entry = {
    id: randomId(),
    actorName: payload.actorName || "System",
    actorRole: payload.actorRole || "SYSTEM",
    action: payload.action,
    entityType: payload.entityType || "SYSTEM",
    entityId: payload.entityId || "",
    tone: payload.tone || "info",
    createdAt: nowIso()
  };
  store.auditLogs = [entry, ...(store.auditLogs || [])].slice(0, 100);
  store.notifications = [
    {
      id: randomId(),
      label: entry.actorRole === "SYSTEM" ? "System" : entry.actorRole.replaceAll("_", " "),
      tone: entry.tone,
      message: entry.action,
      createdAt: entry.createdAt
    },
    ...(store.notifications || [])
  ].slice(0, 20);
}

function buildWorkflowSteps(metrics = {}) {
  return [
    { id: "activation", label: "Device activation requested", status: metrics.pendingBindings > 0 ? "done" : "pending" },
    { id: "binding", label: "Room and TV successfully bound", status: metrics.onlineTvs > 0 ? "done" : "pending" },
    { id: "guest", label: "Guest session active", status: metrics.occupiedRooms > 0 ? "done" : "pending" },
    { id: "content", label: "Content published to launcher", status: metrics.syncVersion > 1 ? "done" : "pending" },
    { id: "sync", label: "TV confirmed live sync", status: metrics.onlineTvs > 0 ? "done" : "pending" }
  ];
}

module.exports = {
  MEAL_CATEGORIES,
  nowIso,
  randomId,
  defaultProperty,
  createDefaultStore,
  normalizeStore,
  normalizeRoom,
  normalizeMealCollection,
  normalizeMenuItem,
  pushAudit,
  buildWorkflowSteps
};
