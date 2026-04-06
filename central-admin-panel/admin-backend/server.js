require("dotenv").config({ path: require("path").join(__dirname, "..", "..", ".env.local") });
require("dotenv").config();

const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const multer = require("multer");
const cookieParser = require("cookie-parser");
const { Pool } = require("pg");
const {
  nowIso,
  defaultProperty,
  createDefaultStore,
  normalizeStore,
  pushAudit,
  buildWorkflowSteps
} = require("./lib/panel-data");
const {
  ROLES,
  DEFAULT_PASSWORD,
  roleLabel,
  hashPassword,
  verifyPassword,
  safeUser,
  seededUsers
} = require("./lib/auth");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const ROOT = __dirname;
const DATA_DIR = path.join(ROOT, "data");
const STORE_PATH = path.join(DATA_DIR, "store.json");
const PUBLIC_DIR = path.join(ROOT, "public");
const UPLOAD_DIR = path.join(ROOT, "uploads");
const STARTUP_DIR = path.join(UPLOAD_DIR, "startup");
const BACKGROUND_DIR = path.join(UPLOAD_DIR, "backgrounds");
const SESSION_COOKIE = "asteria_admin_session";
const SESSION_TTL_DAYS = 7;
const DISABLE_STATIC_UI = process.env.ADMIN_DISABLE_STATIC === "1";

const DATABASE_URL = process.env.DATABASE_URL || "";
const IMAGEKIT_PUBLIC_KEY = process.env.IMAGEKIT_PUBLIC_KEY || "";
const IMAGEKIT_PRIVATE_KEY = process.env.IMAGEKIT_PRIVATE_KEY || "";

const NEEDS_LOCAL_STORE = !DATABASE_URL;
const NEEDS_LOCAL_UPLOADS = !IMAGEKIT_PRIVATE_KEY;
const DIRS_TO_CREATE = new Set([PUBLIC_DIR]);

if (NEEDS_LOCAL_STORE) {
  DIRS_TO_CREATE.add(DATA_DIR);
}

if (NEEDS_LOCAL_UPLOADS) {
  DIRS_TO_CREATE.add(UPLOAD_DIR);
  DIRS_TO_CREATE.add(STARTUP_DIR);
  DIRS_TO_CREATE.add(BACKGROUND_DIR);
}

[...DIRS_TO_CREATE].forEach((dir) => {
  try {
    fs.mkdirSync(dir, { recursive: true });
  } catch (error) {
    if (!process.env.VERCEL) {
      throw error;
    }
  }
});

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }
});

const pool = DATABASE_URL
  ? new Pool({
    connectionString: DATABASE_URL,
    ssl: { rejectUnauthorized: false }
  })
  : null;

app.use(cors());
app.use(cookieParser());
app.use(express.json({ limit: "5mb" }));
app.use(express.urlencoded({ extended: true }));
app.use("/uploads", express.static(UPLOAD_DIR));
if (!DISABLE_STATIC_UI) {
  app.use(express.static(PUBLIC_DIR));
}

function randomToken(size = 8) {
  return crypto.randomBytes(size).toString("hex");
}

function createFileSeed() {
  return {
    store: createDefaultStore(),
    pendingActivations: {},
    bindings: {}
  };
}

function readFileState() {
  if (!fs.existsSync(STORE_PATH)) {
    const seed = createFileSeed();
    fs.writeFileSync(STORE_PATH, JSON.stringify(seed, null, 2));
    return seed;
  }
  const parsed = JSON.parse(fs.readFileSync(STORE_PATH, "utf8"));
  if (parsed.store) {
    parsed.store = normalizeStore(parsed.store);
    return parsed;
  }
  return {
    store: normalizeStore(parsed),
    pendingActivations: parsed.pendingActivations || {},
    bindings: parsed.bindings || {}
  };
}

function writeFileState(state) {
  fs.writeFileSync(STORE_PATH, JSON.stringify(state, null, 2));
}

async function ensureDatabase() {
  if (!pool) {
    return;
  }
  await pool.query(`
    CREATE TABLE IF NOT EXISTS launcher_store (
      store_key TEXT PRIMARY KEY,
      data JSONB NOT NULL,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS pending_activations (
      poll_token TEXT PRIMARY KEY,
      activation_code TEXT NOT NULL UNIQUE,
      device_id TEXT NOT NULL,
      mac_address TEXT,
      status TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS device_bindings (
      session_token TEXT PRIMARY KEY,
      poll_token TEXT NOT NULL UNIQUE,
      activation_code TEXT NOT NULL,
      room_number TEXT NOT NULL,
      device_id TEXT NOT NULL,
      mac_address TEXT,
      bound_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS admin_users (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL,
      status TEXT NOT NULL,
      property_id TEXT NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS admin_sessions (
      session_token TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
      expires_at TIMESTAMPTZ NOT NULL,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);

  const existing = await pool.query(
    "SELECT data FROM launcher_store WHERE store_key = 'global' LIMIT 1"
  );
  if (existing.rowCount === 0) {
    await pool.query(
      "INSERT INTO launcher_store (store_key, data) VALUES ('global', $1::jsonb)",
      [JSON.stringify(createDefaultStore())]
    );
  } else {
    await pool.query(
      "UPDATE launcher_store SET data = $1::jsonb, updated_at = NOW() WHERE store_key = 'global'",
      [JSON.stringify(normalizeStore(existing.rows[0].data))]
    );
  }

  for (const user of seededUsers(defaultProperty().id)) {
    const userExists = await pool.query("SELECT 1 FROM admin_users WHERE email = $1 LIMIT 1", [user.email]);
    if (userExists.rowCount === 0) {
      await pool.query(
        `INSERT INTO admin_users
          (id, name, email, password_hash, role, status, property_id)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [user.id, user.name, user.email, hashPassword(user.password), user.role, user.status, user.propertyId]
      );
    }
  }
}

async function getStore() {
  if (!pool) {
    return readFileState().store;
  }
  const result = await pool.query(
    "SELECT data FROM launcher_store WHERE store_key = 'global' LIMIT 1"
  );
  if (result.rowCount === 0) {
    const seed = createDefaultStore();
    await pool.query(
      "INSERT INTO launcher_store (store_key, data) VALUES ('global', $1::jsonb)",
      [JSON.stringify(seed)]
    );
    return seed;
  }
  return normalizeStore(result.rows[0].data);
}

async function saveStore(store) {
  const nextStore = normalizeStore(store);
  nextStore.sync = nextStore.sync || {};
  nextStore.sync.version = Number(nextStore.sync.version || 0) + 1;
  nextStore.sync.updatedAt = nowIso();

  if (!pool) {
    const state = readFileState();
    state.store = nextStore;
    writeFileState(state);
    return nextStore;
  }

  await pool.query(
    `INSERT INTO launcher_store (store_key, data, updated_at)
     VALUES ('global', $1::jsonb, NOW())
     ON CONFLICT (store_key)
     DO UPDATE SET data = EXCLUDED.data, updated_at = NOW()`,
    [JSON.stringify(nextStore)]
  );
  return nextStore;
}

async function listUsers() {
  if (!pool) {
    return seededUsers(defaultProperty().id).map((user) =>
      safeUser({ ...user, createdAt: nowIso(), propertyId: user.propertyId })
    );
  }
  const result = await pool.query(
    "SELECT id, name, email, role, status, property_id, created_at FROM admin_users ORDER BY created_at ASC"
  );
  return result.rows.map((row) =>
    safeUser({
      id: row.id,
      name: row.name,
      email: row.email,
      role: row.role,
      status: row.status,
      propertyId: row.property_id,
      createdAt: row.created_at
    })
  );
}

async function findUserByEmail(email) {
  if (!pool) {
    const localUser = seededUsers(defaultProperty().id).find((user) => user.email === email);
    return localUser
      ? { ...localUser, passwordHash: hashPassword(localUser.password), createdAt: nowIso() }
      : null;
  }
  const result = await pool.query(
    "SELECT id, name, email, password_hash, role, status, property_id, created_at FROM admin_users WHERE email = $1 LIMIT 1",
    [email]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    passwordHash: row.password_hash,
    role: row.role,
    status: row.status,
    propertyId: row.property_id,
    createdAt: row.created_at
  };
}

async function findUserById(id) {
  if (!pool) {
    const localUser = seededUsers(defaultProperty().id).find((user) => user.id === id);
    return localUser
      ? { ...localUser, passwordHash: hashPassword(localUser.password), createdAt: nowIso() }
      : null;
  }
  const result = await pool.query(
    "SELECT id, name, email, password_hash, role, status, property_id, created_at FROM admin_users WHERE id = $1 LIMIT 1",
    [id]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    id: row.id,
    name: row.name,
    email: row.email,
    passwordHash: row.password_hash,
    role: row.role,
    status: row.status,
    propertyId: row.property_id,
    createdAt: row.created_at
  };
}

async function createUser(payload) {
  const user = {
    id: payload.id || `user-${randomToken(6)}`,
    name: payload.name,
    email: payload.email.toLowerCase(),
    role: payload.role,
    status: payload.status || "ACTIVE",
    propertyId: payload.propertyId || defaultProperty().id,
    passwordHash: hashPassword(payload.password),
    createdAt: nowIso()
  };
  if (pool) {
    await pool.query(
      `INSERT INTO admin_users
        (id, name, email, password_hash, role, status, property_id, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
      [user.id, user.name, user.email, user.passwordHash, user.role, user.status, user.propertyId, user.createdAt]
    );
  }
  return safeUser(user);
}

async function updateUser(userId, payload) {
  if (!pool) {
    return null;
  }
  const current = await findUserById(userId);
  if (!current) {
    return null;
  }
  const next = {
    ...current,
    name: payload.name ?? current.name,
    email: (payload.email ?? current.email).toLowerCase(),
    role: payload.role ?? current.role,
    status: payload.status ?? current.status,
    propertyId: payload.propertyId ?? current.propertyId,
    passwordHash: payload.password ? hashPassword(payload.password) : current.passwordHash
  };
  await pool.query(
    `UPDATE admin_users
     SET name = $2, email = $3, password_hash = $4, role = $5, status = $6, property_id = $7
     WHERE id = $1`,
    [userId, next.name, next.email, next.passwordHash, next.role, next.status, next.propertyId]
  );
  return safeUser(next);
}

async function createSession(userId) {
  const sessionToken = randomToken(24);
  const expiresAt = new Date(Date.now() + SESSION_TTL_DAYS * 24 * 60 * 60 * 1000).toISOString();
  if (pool) {
    await pool.query(
      "INSERT INTO admin_sessions (session_token, user_id, expires_at) VALUES ($1, $2, $3)",
      [sessionToken, userId, expiresAt]
    );
  }
  return { sessionToken, userId, expiresAt };
}

async function findSession(sessionToken) {
  if (!pool) {
    return null;
  }
  const result = await pool.query(
    "SELECT session_token, user_id, expires_at FROM admin_sessions WHERE session_token = $1 LIMIT 1",
    [sessionToken]
  );
  return result.rowCount ? result.rows[0] : null;
}

async function deleteSession(sessionToken) {
  if (pool) {
    await pool.query("DELETE FROM admin_sessions WHERE session_token = $1", [sessionToken]);
  }
}

async function getPendingActivationsMap() {
  if (!pool) {
    return readFileState().pendingActivations;
  }
  const result = await pool.query(
    "SELECT poll_token, activation_code, device_id, mac_address, status, created_at FROM pending_activations ORDER BY created_at DESC"
  );
  return Object.fromEntries(
    result.rows.map((row) => [
      row.poll_token,
      {
        pollToken: row.poll_token,
        activationCode: row.activation_code,
        deviceId: row.device_id,
        macAddress: row.mac_address,
        status: row.status,
        createdAt: row.created_at
      }
    ])
  );
}

async function findPendingByDeviceId(deviceId) {
  if (!pool) {
    return Object.values(readFileState().pendingActivations).find((item) => item.deviceId === deviceId) || null;
  }
  const result = await pool.query(
    "SELECT poll_token, activation_code, device_id, mac_address, status, created_at FROM pending_activations WHERE device_id = $1 LIMIT 1",
    [deviceId]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    pollToken: row.poll_token,
    activationCode: row.activation_code,
    deviceId: row.device_id,
    macAddress: row.mac_address,
    status: row.status,
    createdAt: row.created_at
  };
}

async function createPendingActivation(activation) {
  if (!pool) {
    const state = readFileState();
    state.pendingActivations[activation.pollToken] = activation;
    writeFileState(state);
    return activation;
  }
  await pool.query(
    `INSERT INTO pending_activations
      (poll_token, activation_code, device_id, mac_address, status, created_at)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [
      activation.pollToken,
      activation.activationCode,
      activation.deviceId,
      activation.macAddress,
      activation.status,
      activation.createdAt
    ]
  );
  return activation;
}

async function getPendingByPollToken(pollToken) {
  if (!pool) {
    return readFileState().pendingActivations[pollToken] || null;
  }
  const result = await pool.query(
    "SELECT poll_token, activation_code, device_id, mac_address, status, created_at FROM pending_activations WHERE poll_token = $1 LIMIT 1",
    [pollToken]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    pollToken: row.poll_token,
    activationCode: row.activation_code,
    deviceId: row.device_id,
    macAddress: row.mac_address,
    status: row.status,
    createdAt: row.created_at
  };
}

async function findPendingByActivationCode(code) {
  if (!pool) {
    return Object.values(readFileState().pendingActivations).find((item) => item.activationCode === code) || null;
  }
  const result = await pool.query(
    "SELECT poll_token, activation_code, device_id, mac_address, status, created_at FROM pending_activations WHERE activation_code = $1 LIMIT 1",
    [code]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    pollToken: row.poll_token,
    activationCode: row.activation_code,
    deviceId: row.device_id,
    macAddress: row.mac_address,
    status: row.status,
    createdAt: row.created_at
  };
}

async function deletePendingActivation(pollToken) {
  if (!pool) {
    const state = readFileState();
    delete state.pendingActivations[pollToken];
    writeFileState(state);
    return;
  }
  await pool.query("DELETE FROM pending_activations WHERE poll_token = $1", [pollToken]);
}

async function getBindingsMap() {
  if (!pool) {
    return readFileState().bindings;
  }
  const result = await pool.query(
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at FROM device_bindings ORDER BY bound_at DESC"
  );
  return Object.fromEntries(
    result.rows.map((row) => [
      row.session_token,
      {
        sessionToken: row.session_token,
        pollToken: row.poll_token,
        activationCode: row.activation_code,
        roomNumber: row.room_number,
        deviceId: row.device_id,
        macAddress: row.mac_address,
        boundAt: row.bound_at
      }
    ])
  );
}

async function findBindingBySessionToken(sessionToken) {
  if (!pool) {
    return readFileState().bindings[sessionToken] || null;
  }
  const result = await pool.query(
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at FROM device_bindings WHERE session_token = $1 LIMIT 1",
    [sessionToken]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    sessionToken: row.session_token,
    pollToken: row.poll_token,
    activationCode: row.activation_code,
    roomNumber: row.room_number,
    deviceId: row.device_id,
    macAddress: row.mac_address,
    boundAt: row.bound_at
  };
}

async function findBindingByPollToken(pollToken) {
  if (!pool) {
    return Object.values(readFileState().bindings).find((item) => item.pollToken === pollToken) || null;
  }
  const result = await pool.query(
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at FROM device_bindings WHERE poll_token = $1 LIMIT 1",
    [pollToken]
  );
  if (result.rowCount === 0) {
    return null;
  }
  const row = result.rows[0];
  return {
    sessionToken: row.session_token,
    pollToken: row.poll_token,
    activationCode: row.activation_code,
    roomNumber: row.room_number,
    deviceId: row.device_id,
    macAddress: row.mac_address,
    boundAt: row.bound_at
  };
}

async function createBinding(binding) {
  if (!pool) {
    const state = readFileState();
    state.bindings[binding.sessionToken] = binding;
    writeFileState(state);
    return binding;
  }
  await pool.query(
    `INSERT INTO device_bindings
      (session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [
      binding.sessionToken,
      binding.pollToken,
      binding.activationCode,
      binding.roomNumber,
      binding.deviceId,
      binding.macAddress,
      binding.boundAt
    ]
  );
  return binding;
}

function buildMetrics(store, pendingActivations, bindings) {
  const rooms = Object.values(store.rooms);
  return {
    onlineTvs: Object.keys(bindings).length,
    occupiedRooms: rooms.filter((room) => room.status === "occupied").length,
    pendingBindings: Object.keys(pendingActivations).length,
    unboundDevices: rooms.filter((room) => room.status === "unbound").length,
    vacantRooms: rooms.filter((room) => room.status === "vacant").length,
    offlineRooms: Math.max(rooms.length - Object.keys(bindings).length, 0)
  };
}

async function buildAdminState(user) {
  const store = await getStore();
  const pendingActivations = await getPendingActivationsMap();
  const bindings = await getBindingsMap();
  const metrics = buildMetrics(store, pendingActivations, bindings);
  return {
    currentUser: safeUser(user),
    property: store.property,
    hotel: store.hotel,
    weather: store.weather,
    popup: store.popup,
    backgrounds: store.backgrounds,
    meals: store.meals,
    sections: Object.values(store.sections),
    visibility: store.visibility,
    availableApps: store.availableApps,
    availableInputs: store.availableInputs,
    rooms: store.rooms,
    pendingActivations,
    bindings,
    metrics,
    workflowSteps: buildWorkflowSteps(),
    systemHealth: {
      apiStatus: "Healthy",
      lastPushTime: store.sync.updatedAt,
      launcherVersion: "Asteria Launcher 1.0.0",
      syncVersion: store.sync.version
    },
    notifications: (store.notifications || []).slice(0, 10),
    auditLogs: user.role === ROLES.SUPER_ADMIN ? (store.auditLogs || []).slice(0, 50) : [],
    users: user.role === ROLES.SUPER_ADMIN ? await listUsers() : [],
    settings:
      user.role === ROLES.SUPER_ADMIN
        ? {
          property: store.property,
          integration: {
            apiKeyPreview: store.property.apiKeyPreview,
            webhookUrl: "https://api.asteriagrand.local/webhooks/launcher"
          },
          subscription: {
            plan: "Enterprise",
            renewalDate: "2027-01-01",
            environment: store.property.environment
          }
        }
        : null
  };
}

function buildPayload(store, binding) {
  const room = store.rooms[binding.roomNumber] || { roomNumber: binding.roomNumber, guestName: "" };
  return {
    roomNumber: room.roomNumber,
    guestName: room.guestName || "",
    hotel: store.hotel,
    weather: store.weather,
    popup: store.popup,
    backgrounds: store.backgrounds,
    meals: store.meals,
    sections: Object.values(store.sections),
    visibility: store.visibility,
    sync: {
      version: store.sync.version,
      ttlSeconds: store.sync.ttlSeconds || 300,
      updatedAt: store.sync.updatedAt || nowIso()
    }
  };
}

function sanitizeExt(fileName) {
  return path.extname(fileName || "").toLowerCase() || ".jpg";
}

function localRelativeUrl(kind, bucket, fileName) {
  if (kind === "startup") {
    return `/uploads/startup/${fileName}`;
  }
  return `/uploads/backgrounds/${bucket}/${fileName}`;
}

async function saveFileLocally(file, kind, bucket) {
  const ext = sanitizeExt(file.originalname);
  const fileName = `${Date.now()}-${crypto.randomBytes(4).toString("hex")}${ext}`;
  const targetDir = kind === "startup" ? STARTUP_DIR : path.join(BACKGROUND_DIR, bucket);
  fs.mkdirSync(targetDir, { recursive: true });
  const targetPath = path.join(targetDir, fileName);
  fs.writeFileSync(targetPath, file.buffer);
  return localRelativeUrl(kind, bucket, fileName);
}

async function uploadToImageKit(file, kind, bucket) {
  if (!IMAGEKIT_PRIVATE_KEY || !IMAGEKIT_PUBLIC_KEY) {
    return null;
  }

  const ext = sanitizeExt(file.originalname);
  const fileName = `${Date.now()}-${crypto.randomBytes(4).toString("hex")}${ext}`;
  const folder =
    kind === "startup"
      ? "/asteria-grand/startup"
      : `/asteria-grand/backgrounds/${bucket}`;
  const body = new FormData();
  const dataUri = `data:${file.mimetype || "application/octet-stream"};base64,${file.buffer.toString("base64")}`;
  body.append("file", dataUri);
  body.append("fileName", fileName);
  body.append("folder", folder);
  body.append("useUniqueFileName", "true");

  const response = await fetch("https://upload.imagekit.io/api/v1/files/upload", {
    method: "POST",
    headers: {
      Authorization: `Basic ${Buffer.from(`${IMAGEKIT_PRIVATE_KEY}:`).toString("base64")}`
    },
    body
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`ImageKit upload failed: ${response.status} ${text}`);
  }

  const result = await response.json();
  return result.url;
}

async function persistUpload(file, kind, bucket) {
  try {
    const remoteUrl = await uploadToImageKit(file, kind, bucket);
    if (remoteUrl) {
      return remoteUrl;
    }
  } catch (error) {
    console.error("ImageKit upload failed, falling back to local storage:", error.message);
  }
  return saveFileLocally(file, kind, bucket);
}

function asyncHandler(fn) {
  return (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

async function attachCurrentUser(req) {
  const sessionToken = req.cookies[SESSION_COOKIE];
  if (!sessionToken) {
    return null;
  }
  const session = await findSession(sessionToken);
  if (!session) {
    return null;
  }
  if (new Date(session.expires_at || session.expiresAt).getTime() < Date.now()) {
    await deleteSession(sessionToken);
    return null;
  }
  const userId = session.user_id || session.userId;
  return findUserById(userId);
}

function requireAuth(handler) {
  return asyncHandler(async (req, res, next) => {
    const user = await attachCurrentUser(req);
    if (!user || user.status !== "ACTIVE") {
      res.clearCookie(SESSION_COOKIE);
      return res.status(401).json({ error: "Authentication required" });
    }
    req.currentUser = user;
    return handler(req, res, next);
  });
}

function requireRoles(roles, handler) {
  return requireAuth(async (req, res, next) => {
    if (!roles.includes(req.currentUser.role)) {
      return res.status(403).json({ error: "Forbidden" });
    }
    return handler(req, res, next);
  });
}

app.get(
  "/api/health",
  asyncHandler(async (_, res) => {
    let database = "file";
    if (pool) {
      await pool.query("SELECT 1");
      database = "postgres";
    }
    res.json({
      ok: true,
      service: "asteria-grand-central-admin-panel",
      database,
      imageStorage: IMAGEKIT_PRIVATE_KEY ? "imagekit" : "local"
    });
  })
);

app.post(
  "/api/auth/login",
  asyncHandler(async (req, res) => {
    const email = `${req.body.email || ""}`.trim().toLowerCase();
    const password = `${req.body.password || ""}`;
    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }
    const user = await findUserByEmail(email);
    if (!user || user.status !== "ACTIVE" || !verifyPassword(password, user.passwordHash)) {
      return res.status(401).json({ error: "Invalid email or password" });
    }
    const session = await createSession(user.id);
    res.cookie(SESSION_COOKIE, session.sessionToken, {
      httpOnly: true,
      sameSite: "lax",
      secure: false,
      maxAge: SESSION_TTL_DAYS * 24 * 60 * 60 * 1000
    });
    res.json({ ok: true, user: safeUser(user), demoPassword: DEFAULT_PASSWORD });
  })
);

app.post(
  "/api/auth/logout",
  requireAuth(async (req, res) => {
    await deleteSession(req.cookies[SESSION_COOKIE]);
    res.clearCookie(SESSION_COOKIE);
    res.json({ ok: true });
  })
);

app.get(
  "/api/auth/me",
  asyncHandler(async (req, res) => {
    const user = await attachCurrentUser(req);
    if (!user || user.status !== "ACTIVE") {
      return res.status(401).json({ error: "Not authenticated" });
    }
    res.json({ user: safeUser(user) });
  })
);

app.post(
  "/api/device/activate",
  asyncHandler(async (req, res) => {
    const deviceId = req.body.deviceId || randomToken(6);
    const macAddress = req.body.macAddress || null;
    const existing = await findPendingByDeviceId(deviceId);
    if (existing) {
      return res.json(existing);
    }
    const activation = {
      pollToken: randomToken(12),
      activationCode: randomToken(3).toUpperCase(),
      deviceId,
      macAddress,
      status: "pending",
      createdAt: nowIso()
    };
    await createPendingActivation(activation);
    const store = await getStore();
    pushAudit(store, {
      actorName: "System",
      actorRole: "SYSTEM",
      action: `Device activation ${activation.activationCode} is waiting for room binding`,
      entityType: "DEVICE",
      entityId: activation.deviceId,
      tone: "warning"
    });
    await saveStore(store);
    res.json(activation);
  })
);

app.get(
  "/api/device/activation-status/:pollToken",
  asyncHandler(async (req, res) => {
    const pending = await getPendingByPollToken(req.params.pollToken);
    if (pending) {
      return res.json(pending);
    }
    const binding = await findBindingByPollToken(req.params.pollToken);
    if (!binding) {
      return res.status(404).json({ error: "Activation session not found" });
    }
    res.json({
      pollToken: binding.pollToken,
      activationCode: binding.activationCode,
      sessionToken: binding.sessionToken,
      status: "bound",
      roomNumber: binding.roomNumber
    });
  })
);

app.get(
  "/api/launcher/content",
  asyncHandler(async (req, res) => {
    const sessionToken = req.query.sessionToken;
    const binding = await findBindingBySessionToken(sessionToken);
    if (!binding) {
      return res.status(401).json({ error: "Invalid session token" });
    }
    const store = await getStore();
    res.json(buildPayload(store, binding));
  })
);

app.get(
  "/api/admin/state",
  requireAuth(async (req, res) => {
    res.json(await buildAdminState(req.currentUser));
  })
);

app.get(
  "/api/admin/users",
  requireRoles([ROLES.SUPER_ADMIN], async (_, res) => {
    res.json({ users: await listUsers() });
  })
);

app.post(
  "/api/admin/users",
  requireRoles([ROLES.SUPER_ADMIN], async (req, res) => {
    const payload = req.body || {};
    if (!payload.name || !payload.email || !payload.password || !payload.role) {
      return res.status(400).json({ error: "name, email, password, and role are required" });
    }
    const existing = await findUserByEmail(payload.email.toLowerCase());
    if (existing) {
      return res.status(409).json({ error: "A user with that email already exists" });
    }
    const user = await createUser(payload);
    const store = await getStore();
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Created ${roleLabel(user.role)} account for ${user.name}`,
      entityType: "USER",
      entityId: user.id,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, user });
  })
);

app.patch(
  "/api/admin/users/:userId",
  requireRoles([ROLES.SUPER_ADMIN], async (req, res) => {
    const user = await updateUser(req.params.userId, req.body || {});
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }
    const store = await getStore();
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Updated account settings for ${user.name}`,
      entityType: "USER",
      entityId: user.id,
      tone: "info"
    });
    await saveStore(store);
    res.json({ ok: true, user });
  })
);

app.post(
  "/api/admin/config",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN], async (req, res) => {
    const store = await getStore();
    const next = req.body || {};
    store.hotel = { ...store.hotel, ...(next.hotel || {}) };
    store.weather = { ...store.weather, ...(next.weather || {}) };
    store.popup = { ...store.popup, ...(next.popup || {}) };
    store.backgrounds = { ...store.backgrounds, ...(next.backgrounds || {}) };
    store.meals = { ...store.meals, ...(next.meals || {}) };
    if (next.sections) {
      const mergedSections = Array.isArray(next.sections)
        ? next.sections
        : Object.values(next.sections);
      mergedSections.forEach((section) => {
        store.sections[section.id] = section;
      });
    }
    if (next.visibility) {
      store.visibility = {
        ...store.visibility,
        ...next.visibility
      };
    }
    if (next.sync?.ttlSeconds) {
      store.sync.ttlSeconds = next.sync.ttlSeconds;
    }
    if (next.property) {
      store.property = { ...store.property, ...next.property };
    }
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: "Pushed launcher content and policy changes",
      entityType: "LAUNCHER_CONFIG",
      entityId: "global",
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, store });
  })
);

app.post(
  "/api/admin/rooms/create",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN], async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
    if (!roomNumber) {
      return res.status(400).json({ error: "roomNumber is required" });
    }
    if (store.rooms[roomNumber]) {
      return res.status(409).json({ error: "Room already exists" });
    }

    store.rooms[roomNumber] = {
      roomNumber,
      floor: roomNumber.slice(0, Math.max(1, roomNumber.length - 2)),
      status: "unbound",
      guestName: "",
      deviceId: "",
      lastSyncAt: nowIso(),
      checkInAt: "",
      welcomeNote: "",
      language: "English",
      overrideEnabled: false,
      customContentLabel: "",
      lastUpdatedBy: req.currentUser.name
    };

    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Created new room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });

    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.post(
  "/api/admin/rooms",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.RECEPTIONIST], async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
    if (!roomNumber) {
      return res.status(400).json({ error: "roomNumber is required" });
    }
    const current = store.rooms[roomNumber] || {
      roomNumber,
      floor: roomNumber.slice(0, roomNumber.length - 2)
    };
    const guestName = `${req.body.guestName ?? current.guestName ?? ""}`.trim();
    store.rooms[roomNumber] = {
      ...current,
      roomNumber,
      guestName,
      welcomeNote: `${req.body.welcomeNote ?? current.welcomeNote ?? ""}`.trim(),
      language: `${req.body.language ?? current.language ?? "English"}`.trim(),
      status: req.body.status || (guestName ? "occupied" : current.deviceId ? "vacant" : "unbound"),
      checkInAt: guestName ? current.checkInAt || nowIso() : "",
      lastSyncAt: nowIso(),
      lastUpdatedBy: req.currentUser.name
    };
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Updated guest session for room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "info"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.post(
  "/api/admin/rooms/:roomNumber/checkout",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.RECEPTIONIST], async (req, res) => {
    const store = await getStore();
    const room = store.rooms[req.params.roomNumber];
    if (!room) {
      return res.status(404).json({ error: "Room not found" });
    }
    store.rooms[req.params.roomNumber] = {
      ...room,
      guestName: "",
      welcomeNote: "",
      checkInAt: "",
      status: room.deviceId ? "vacant" : "unbound",
      lastSyncAt: nowIso(),
      lastUpdatedBy: req.currentUser.name
    };
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Checked out room ${req.params.roomNumber}`,
      entityType: "ROOM",
      entityId: req.params.roomNumber,
      tone: "warning"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[req.params.roomNumber] });
  })
);

app.post(
  "/api/admin/rooms/:roomNumber/override",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN], async (req, res) => {
    const store = await getStore();
    const room = store.rooms[req.params.roomNumber];
    if (!room) {
      return res.status(404).json({ error: "Room not found" });
    }
    store.rooms[req.params.roomNumber] = {
      ...room,
      overrideEnabled: Boolean(req.body.enabled),
      customContentLabel: `${req.body.customContentLabel || ""}`.trim(),
      lastSyncAt: nowIso(),
      lastUpdatedBy: req.currentUser.name
    };
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `${store.rooms[req.params.roomNumber].overrideEnabled ? "Enabled" : "Disabled"} room override for ${req.params.roomNumber}`,
      entityType: "ROOM_OVERRIDE",
      entityId: req.params.roomNumber,
      tone: "info"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[req.params.roomNumber] });
  })
);

app.post(
  "/api/admin/bind",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN, ROLES.RECEPTIONIST], async (req, res) => {
    const store = await getStore();
    const activationCode = `${req.body.activationCode || ""}`.trim().toUpperCase();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
    const guestName = `${req.body.guestName || ""}`.trim();
    const welcomeNote = `${req.body.welcomeNote || ""}`.trim();
    if (!activationCode || !roomNumber) {
      return res.status(400).json({ error: "activationCode and roomNumber are required" });
    }
    const pendingEntry = await findPendingByActivationCode(activationCode);
    if (!pendingEntry) {
      return res.status(404).json({ error: "Activation code not found" });
    }
    const current = store.rooms[roomNumber] || {
      roomNumber,
      floor: roomNumber.slice(0, roomNumber.length - 2)
    };
    store.rooms[roomNumber] = {
      ...current,
      roomNumber,
      guestName,
      welcomeNote,
      status: guestName ? "occupied" : "vacant",
      deviceId: pendingEntry.deviceId || current.deviceId || `TV-${roomNumber}`,
      checkInAt: guestName ? nowIso() : current.checkInAt || "",
      lastSyncAt: nowIso(),
      lastUpdatedBy: req.currentUser.name
    };
    const sessionToken = randomToken(16);
    const binding = {
      sessionToken,
      pollToken: pendingEntry.pollToken,
      activationCode,
      roomNumber,
      deviceId: pendingEntry.deviceId,
      macAddress: pendingEntry.macAddress,
      boundAt: nowIso()
    };
    await createBinding(binding);
    await deletePendingActivation(pendingEntry.pollToken);
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Bound activation ${activationCode} to room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, sessionToken, roomNumber });
  })
);

app.post(
  "/api/admin/upload",
  requireRoles([ROLES.SUPER_ADMIN, ROLES.ADMIN], async (req, res) => {
    await new Promise((resolve, reject) => {
      upload.single("file")(req, res, (error) => {
        if (error) {
          reject(error);
        } else {
          resolve();
        }
      });
    });
    const store = await getStore();
    if (!req.file) {
      return res.status(400).json({ error: "file is required" });
    }
    const kind = (req.query.kind || "background").toLowerCase();
    const bucket = (req.query.bucket || "home").toLowerCase();
    const url = await persistUpload(req.file, kind, bucket);

    if (kind === "startup") {
      store.hotel.startupLogoUrl = url;
    } else {
      const target = Array.isArray(store.backgrounds[bucket]) ? store.backgrounds[bucket] : [];
      store.backgrounds[bucket] = [url, ...target].slice(0, 8);
    }
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Uploaded ${kind === "startup" ? "startup asset" : `${bucket} background asset`}`,
      entityType: "ASSET",
      entityId: bucket,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, url, store });
  })
);

if (!DISABLE_STATIC_UI) {
  app.get("*", (_, res) => {
    res.sendFile(path.join(PUBLIC_DIR, "index.html"));
  });
}

app.use((error, _, res, __) => {
  console.error(error);
  res.status(500).json({ error: error.message || "Internal server error" });
});

async function start() {
  await ensureDatabase();
  app.listen(PORT, () => {
    const dbMode = pool ? "postgres" : "file";
    const imageMode = IMAGEKIT_PRIVATE_KEY ? "imagekit" : "local";
    console.log(
      `Asteria Grand admin panel listening on http://localhost:${PORT} (db=${dbMode}, images=${imageMode})`
    );
  });
}

module.exports = {
  app,
  ensureDatabase,
  start
};

if (require.main === module) {
  start().catch((error) => {
    console.error("Failed to start admin panel:", error);
    process.exit(1);
  });
}
