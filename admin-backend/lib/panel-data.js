const crypto = require("crypto");

function nowIso() {
  return new Date().toISOString();
}

function randomId(size = 8) {
  return crypto.randomBytes(size).toString("hex");
}

function defaultProperty() {
  return {
    id: "asteria-grand-main",
    name: "Asteria Grand",
    address: "12 Riverside Avenue, Dhaka",
    timezone: "Asia/Dhaka",
    accentColor: "#C9A84C",
    environment: "Production",
    apiKeyPreview: "ag_live_****_panel"
  };
}

function buildDefaultRooms() {
  const result = {};
  const roomSeeds = [
    ["1201", "vacant", ""],
    ["1202", "occupied", "Mr. Karim"],
    ["1203", "occupied", "Ms. Sen"],
    ["1204", "unbound", ""],
    ["1205", "vacant", ""],
    ["1206", "occupied", "Mr. Rahman"],
    ["1207", "vacant", ""],
    ["1208", "occupied", "Mrs. Noor"],
    ["1301", "vacant", ""],
    ["1302", "occupied", "Mr. Hasan"],
    ["1303", "vacant", ""],
    ["1304", "unbound", ""],
    ["1305", "occupied", "Ms. Khatun"],
    ["1306", "vacant", ""],
    ["1307", "vacant", ""],
    ["1308", "occupied", "Mr. Imran"]
  ];

  roomSeeds.forEach(([roomNumber, status, guestName]) => {
    result[roomNumber] = {
      roomNumber,
      floor: roomNumber.slice(0, roomNumber.length - 2),
      status,
      guestName,
      deviceId: status === "unbound" ? "" : `TV-${roomNumber}`,
      lastSyncAt: nowIso(),
      checkInAt: guestName ? nowIso() : "",
      welcomeNote: guestName ? "Welcome to Asteria Grand" : "",
      language: "English",
      overrideEnabled: false,
      customContentLabel: "",
      lastUpdatedBy: guestName ? "System" : ""
    };
  });

  return result;
}

function createDefaultStore() {
  return {
    property: defaultProperty(),
    hotel: {
      hotelName: "Asteria Grand",
      shortBrand: "AG",
      tagline: "A premium guest-room experience across every screen",
      location: "Dhaka",
      supportPhone: "+880 1234 567890",
      startupLogoUrl: null,
      checkoutLabel: "Checkout 12:00 PM",
      billLabel: "Total bill till now",
      billValue: "Pending sync",
      loadingMessage: "Preparing the Asteria Grand experience"
    },
    weather: {
      temperatureC: 28,
      condition: "Clear"
    },
    popup: {
      helpTitle: "Need assistance?",
      ratingText: "4.8/5 guest rating",
      callHint: "If you want you can call this number",
      callNumber: "+880 1234 567890"
    },
    backgrounds: {
      home: [],
      roomService: [],
      foodMenu: [],
      inputs: []
    },
    meals: {
      breakfast: [
        {
          id: "breakfast_tray",
          title: "Suite Breakfast Tray",
          subtitle: "Delivered to your room",
          description: "Coffee, bakery, fruit, and made-to-order eggs served for your stay.",
          badge: "AM",
          accentColor: "#D49B4A",
          price: "450 BDT",
          available: true
        },
        {
          id: "breakfast_lobby",
          title: "Lobby Breakfast",
          subtitle: "Restaurant seating available",
          description: "Light buffet and local breakfast favorites are being highlighted this morning.",
          badge: "DIN",
          accentColor: "#C88A3A",
          price: "520 BDT",
          available: true
        }
      ],
      lunch: [
        {
          id: "lunch_pool",
          title: "Poolside Lunch",
          subtitle: "Fresh midday menu",
          description: "Flatbreads, grilled favorites, and cold drinks are available from the terrace.",
          badge: "SUN",
          accentColor: "#CD8540",
          price: "690 BDT",
          available: true
        }
      ],
      dinner: [
        {
          id: "dinner_grill",
          title: "Rooftop Grill",
          subtitle: "Open this evening",
          description: "Grill selections and chef plates are highlighted for tonight.",
          badge: "PM",
          accentColor: "#D08E44",
          price: "990 BDT",
          available: true
        }
      ],
      beverages: [
        {
          id: "beverage_signature",
          title: "Signature Mocktail",
          subtitle: "Served chilled",
          description: "House-crafted seasonal mocktail with citrus and mint.",
          badge: "BAR",
          accentColor: "#9D7E3F",
          price: "390 BDT",
          available: true
        }
      ]
    },
    sections: {
      services: {
        id: "services",
        title: "Room Service and Concierge",
        subtitle: "Core hotel services ready from this TV",
        style: "COMPACT",
        enabled: true,
        cards: [
          {
            id: "service_housekeeping",
            title: "Housekeeping",
            subtitle: "Freshen up your room",
            description: "Request towels, bedding, or a quick room refresh from the front desk.",
            badge: "CLN",
            accentColor: "#2E8B86"
          }
        ]
      },
      entertainment: {
        id: "entertainment",
        title: "Entertainment Available on TV",
        subtitle: "Apps and channels curated for this room",
        style: "STANDARD",
        enabled: true,
        cards: [
          {
            id: "entertainment_inputs",
            title: "External Devices",
            subtitle: "HDMI, Dish, USB and more",
            description: "Every detected TV source is grouped into the Inputs page for guests.",
            badge: "IN",
            accentColor: "#6B5AC9"
          }
        ]
      },
      foodMenu: {
        id: "food_menu",
        title: "Food Menu",
        subtitle: "Hotel dining experiences and handoff app entry",
        style: "STANDARD",
        enabled: true,
        cards: [
          {
            id: "food_breakfast",
            title: "Breakfast Service",
            subtitle: "Morning dining",
            description: "Warm breakfast service with in-room delivery and restaurant pickup choices.",
            badge: "AM",
            accentColor: "#D49B4A"
          }
        ]
      }
    },
    visibility: {
      destinations: {
        home: true,
        roomService: true,
        foodMenu: true,
        inputs: true
      },
      visibleAppPackages: ["com.netflix.ninja", "com.google.android.youtube.tv"],
      visibleSourceTitles: ["HDMI 1", "HDMI 2", "DTV", "ATV"]
    },
    availableApps: [
      { id: "netflix", packageName: "com.netflix.ninja", name: "Netflix", description: "Streaming and premium series" },
      { id: "youtube", packageName: "com.google.android.youtube.tv", name: "YouTube", description: "Video and music content" },
      { id: "prime", packageName: "com.amazon.amazonvideo.livingroom", name: "Prime Video", description: "Movies and originals" },
      { id: "browser", packageName: "com.android.browser", name: "Browser", description: "Web browsing for guests" }
    ],
    availableInputs: [
      { id: "hdmi1", title: "HDMI 1", description: "Console or STB input" },
      { id: "hdmi2", title: "HDMI 2", description: "Guest device input" },
      { id: "dtv", title: "DTV", description: "Digital TV channel source" },
      { id: "atv", title: "ATV", description: "Analog TV source" },
      { id: "usb", title: "USB Media", description: "USB playback source" }
    ],
    rooms: buildDefaultRooms(),
    auditLogs: [
      {
        id: randomId(),
        actorName: "System",
        actorRole: "SYSTEM",
        action: "Asteria Grand control plane is online",
        entityType: "SYSTEM",
        entityId: "bootstrap",
        tone: "info",
        createdAt: nowIso()
      }
    ],
    notifications: [
      {
        id: randomId(),
        label: "System",
        tone: "info",
        message: "Asteria Grand control plane is online",
        createdAt: nowIso()
      }
    ],
    sync: {
      version: 1,
      ttlSeconds: 300,
      updatedAt: nowIso()
    }
  };
}

function normalizeStore(source = {}) {
  const defaults = createDefaultStore();
  const store = {
    ...defaults,
    ...source,
    property: { ...defaults.property, ...(source.property || {}) },
    hotel: { ...defaults.hotel, ...(source.hotel || {}) },
    weather: { ...defaults.weather, ...(source.weather || {}) },
    popup: { ...defaults.popup, ...(source.popup || {}) },
    backgrounds: { ...defaults.backgrounds, ...(source.backgrounds || {}) },
    meals: { ...defaults.meals, ...(source.meals || {}) },
    sections: { ...defaults.sections, ...(source.sections || {}) },
    visibility: {
      ...defaults.visibility,
      ...(source.visibility || {}),
      destinations: {
        ...defaults.visibility.destinations,
        ...((source.visibility && source.visibility.destinations) || {})
      }
    },
    availableApps: source.availableApps || defaults.availableApps,
    availableInputs: source.availableInputs || defaults.availableInputs,
    rooms: { ...defaults.rooms, ...(source.rooms || {}) },
    auditLogs: Array.isArray(source.auditLogs) ? source.auditLogs : defaults.auditLogs,
    notifications: Array.isArray(source.notifications) ? source.notifications : defaults.notifications,
    sync: { ...defaults.sync, ...(source.sync || {}) }
  };

  if (store.hotel.hotelName === "Hotel Vision Grand") {
    store.hotel.hotelName = "Asteria Grand";
  }
  if (store.hotel.shortBrand === "HV") {
    store.hotel.shortBrand = "AG";
  }
  if (store.hotel.tagline === "Comfort and entertainment for every stay") {
    store.hotel.tagline = defaults.hotel.tagline;
  }
  if (store.hotel.loadingMessage === "Preparing your room experience") {
    store.hotel.loadingMessage = defaults.hotel.loadingMessage;
  }

  Object.entries(store.rooms).forEach(([roomNumber, room]) => {
    store.rooms[roomNumber] = {
      roomNumber,
      floor: room.floor || roomNumber.slice(0, roomNumber.length - 2),
      status: room.status || (room.guestName ? "occupied" : room.deviceId ? "vacant" : "unbound"),
      guestName: room.guestName || "",
      deviceId: room.deviceId || "",
      lastSyncAt: room.lastSyncAt || store.sync.updatedAt,
      checkInAt: room.checkInAt || (room.guestName ? store.sync.updatedAt : ""),
      welcomeNote: room.welcomeNote || "",
      language: room.language || "English",
      overrideEnabled: Boolean(room.overrideEnabled),
      customContentLabel: room.customContentLabel || "",
      lastUpdatedBy: room.lastUpdatedBy || ""
    };
  });

  return store;
}

function pushAudit(store, payload) {
  const entry = {
    id: randomId(),
    actorName: payload.actorName,
    actorRole: payload.actorRole,
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
      label: payload.actorRole === "SYSTEM" ? "System" : payload.actorRole.replaceAll("_", " "),
      tone: payload.tone || "info",
      message: payload.action,
      createdAt: entry.createdAt
    },
    ...(store.notifications || [])
  ].slice(0, 20);
}

function buildWorkflowSteps() {
  return [
    { id: "activation", label: "Device activation generated", status: "done" },
    { id: "binding", label: "Reception binds device to room", status: "done" },
    { id: "guest", label: "Guest name assigned to session", status: "pending" },
    { id: "content", label: "Launcher content pushed", status: "pending" },
    { id: "sync", label: "TV confirms live sync", status: "pending" }
  ];
}

module.exports = {
  nowIso,
  randomId,
  defaultProperty,
  createDefaultStore,
  normalizeStore,
  pushAudit,
  buildWorkflowSteps
};
