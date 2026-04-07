require("dotenv").config({ path: require("path").join(__dirname, "..", ".env.local") });
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
  MEAL_CATEGORIES,
  nowIso,
  defaultProperty,
  createDefaultStore,
  normalizeStore,
  normalizeRoom,
  normalizeMenuItem,
  pushAudit,
  buildWorkflowSteps
} = require("./lib/panel-data");
const {
  ROOM_CATEGORY_OPTIONS,
  CLIMATE_CONTROL_OPTIONS,
  normalizeRoomCategory,
  normalizeClimateControl
} = require("./lib/room-options");
const {
  ROLES,
  roleLabel,
  hashPassword,
  verifyPassword,
  safeUser,
  seededUsers,
  hasFirstAdminConfig,
  firstAdminConfig
} = require("./lib/auth");
const { canModuleAction } = require("./lib/permissions");

const app = express();
const PORT = Number(process.env.PORT || 3000);
const ROOT = __dirname;
const DATA_DIR = path.join(ROOT, "data");
const STORE_PATH = path.join(DATA_DIR, "store.json");
const PUBLIC_DIR = path.join(ROOT, "public");
const UPLOAD_DIR = path.join(ROOT, "uploads");
const STARTUP_DIR = path.join(UPLOAD_DIR, "startup");
const BACKGROUND_DIR = path.join(UPLOAD_DIR, "backgrounds");
const SESSION_COOKIE = "hotel_admin_session";
const SESSION_SECRET = process.env.SESSION_SECRET || "";
const SESSION_TTL_HOURS = Number(process.env.ADMIN_SESSION_TTL_HOURS || 12);
const SESSION_RENEWAL_HOURS = Number(process.env.ADMIN_SESSION_RENEWAL_HOURS || 4);
const DEVICE_ONLINE_WINDOW_MS = Number(process.env.DEVICE_ONLINE_WINDOW_MS || 2 * 60 * 1000);
const DEVICE_STALE_WINDOW_MS = Number(process.env.DEVICE_STALE_WINDOW_MS || 15 * 60 * 1000);
const DISABLE_STATIC_UI = process.env.ADMIN_DISABLE_STATIC === "1";

const DATABASE_URL = process.env.DATABASE_URL || "";
const IMAGEKIT_PUBLIC_KEY = process.env.IMAGEKIT_PUBLIC_KEY || "";
const IMAGEKIT_PRIVATE_KEY = process.env.IMAGEKIT_PRIVATE_KEY || "";
const PANEL_WEBHOOK_URL = process.env.PANEL_WEBHOOK_URL || "";
const PANEL_SUBSCRIPTION_PLAN = process.env.PANEL_SUBSCRIPTION_PLAN || "";
const PANEL_SUBSCRIPTION_RENEWAL_DATE = process.env.PANEL_SUBSCRIPTION_RENEWAL_DATE || "";
const IS_PRODUCTION =
  process.env.NODE_ENV === "production" ||
  process.env.VERCEL_ENV === "production" ||
  process.env.VERCEL === "1";

function deriveFallbackSessionSecret() {
  const seedParts = [
    DATABASE_URL,
    IMAGEKIT_PRIVATE_KEY,
    process.env.FIRST_ADMIN_EMAIL || "",
    process.env.FIRST_ADMIN_PROPERTY_ID || process.env.PROPERTY_ID || "",
    process.env.VERCEL_PROJECT_PRODUCTION_URL || process.env.VERCEL_URL || "hotel-central-admin-panel"
  ].filter(Boolean);
  if (seedParts.length === 0) {
    return "";
  }
  return crypto.createHash("sha256").update(seedParts.join("|")).digest("hex");
}

const EFFECTIVE_SESSION_SECRET = SESSION_SECRET || deriveFallbackSessionSecret();
const SESSION_SECRET_IS_DERIVED = !SESSION_SECRET && Boolean(EFFECTIVE_SESSION_SECRET);
const RUNTIME_WARNINGS = [];

if (IS_PRODUCTION && SESSION_SECRET_IS_DERIVED) {
  const warning =
    "SESSION_SECRET is not set in production. Using a stable derived fallback secret. Set SESSION_SECRET in Vercel to remove this warning.";
  RUNTIME_WARNINGS.push(warning);
  console.warn(warning);
}

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

const rateLimitBuckets = new Map();

function pruneRateLimitBucket(bucket) {
  const now = Date.now();
  for (const [key, value] of bucket.entries()) {
    if (value.resetAt <= now) {
      bucket.delete(key);
    }
  }
}

function consumeRateLimit(scope, key, { windowMs, max }) {
  const now = Date.now();
  const bucket = rateLimitBuckets.get(scope) || new Map();
  rateLimitBuckets.set(scope, bucket);
  pruneRateLimitBucket(bucket);
  const current = bucket.get(key);
  if (!current || current.resetAt <= now) {
    bucket.set(key, { count: 1, resetAt: now + windowMs });
    return { allowed: true, remaining: Math.max(max - 1, 0), resetAt: now + windowMs };
  }
  current.count += 1;
  bucket.set(key, current);
  return {
    allowed: current.count <= max,
    remaining: Math.max(max - current.count, 0),
    resetAt: current.resetAt
  };
}

function rateLimitBy(scope, options, keyResolver) {
  return (req, res, next) => {
    const key = `${keyResolver(req) || req.ip || "anonymous"}`;
    const result = consumeRateLimit(scope, key, options);
    res.setHeader("X-RateLimit-Remaining", `${result.remaining}`);
    res.setHeader("X-RateLimit-Reset", `${Math.ceil(result.resetAt / 1000)}`);
    if (!result.allowed) {
      return res.status(429).json({ error: "Too many requests. Please try again shortly." });
    }
    return next();
  };
}

function strictSecurityHeaders(req, res, next) {
  res.setHeader(
    "Content-Security-Policy",
    [
      "default-src 'self'",
      "base-uri 'self'",
      "frame-ancestors 'none'",
      "form-action 'self'",
      "object-src 'none'",
      "img-src 'self' data: blob: https:",
      "font-src 'self' data: https:",
      "connect-src 'self' https:",
      "style-src 'self' 'unsafe-inline'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'"
    ].join("; ")
  );
  res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  if (IS_PRODUCTION) {
    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
  }
  next();
}

function sessionCookieOptions(maxAgeMs) {
  return {
    httpOnly: true,
    sameSite: "strict",
    secure: IS_PRODUCTION,
    path: "/",
    maxAge: maxAgeMs
  };
}

function clientIp(req) {
  return `${req.headers["x-forwarded-for"] || req.socket?.remoteAddress || req.ip || "unknown"}`
    .split(",")[0]
    .trim();
}

app.use(strictSecurityHeaders);
app.use(cors({ origin: true, credentials: true }));
app.use(cookieParser());
app.use(express.json({ limit: "5mb" }));
app.use(express.urlencoded({ extended: true }));
app.use("/uploads", express.static(UPLOAD_DIR));
if (!DISABLE_STATIC_UI) {
  app.use(express.static(PUBLIC_DIR));
}

function validateRuntimeConfig() {
  if (IS_PRODUCTION && !DATABASE_URL) {
    throw new Error("DATABASE_URL is required in production.");
  }
  if (IS_PRODUCTION && !EFFECTIVE_SESSION_SECRET) {
    throw new Error("SESSION_SECRET is required in production or must be derivable from configured secrets.");
  }
}

function ensureImageKitConfigured() {
  if (!IMAGEKIT_PUBLIC_KEY || !IMAGEKIT_PRIVATE_KEY) {
    throw new Error("ImageKit upload is not configured.");
  }
}

function randomToken(size = 8) {
  const entropy = crypto.randomBytes(size).toString("hex");
  const secret = EFFECTIVE_SESSION_SECRET || `dev-secret:${PORT}`;
  return crypto
    .createHmac("sha256", secret)
    .update(`${Date.now()}:${entropy}:${randomToken.counter++}`)
    .digest("hex");
}
randomToken.counter = 0;

const ACTIVATION_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const ACTIVATION_CODE_LENGTH = Math.min(
  12,
  Math.max(10, Number(process.env.ACTIVATION_CODE_LENGTH || 10) || 10)
);

function isHumanActivationCode(value) {
  return new RegExp(`^[${ACTIVATION_CODE_CHARS}]{${ACTIVATION_CODE_LENGTH}}$`).test(`${value || ""}`.trim().toUpperCase());
}

function generateActivationCode(length = ACTIVATION_CODE_LENGTH) {
  let code = "";
  const bytes = crypto.randomBytes(length);
  for (let index = 0; index < length; index += 1) {
    code += ACTIVATION_CODE_CHARS[bytes[index] % ACTIVATION_CODE_CHARS.length];
  }
  return code;
}

async function createUniqueActivationCode(length = ACTIVATION_CODE_LENGTH) {
  for (let attempt = 0; attempt < 8; attempt += 1) {
    const code = generateActivationCode(length);
    const existing = await findPendingByActivationCode(code);
    if (!existing) {
      return code;
    }
  }
  return `${randomToken(6).toUpperCase().slice(0, length)}`.padEnd(length, "X");
}

async function repairPendingActivationCodes() {
  if (!pool) {
    const state = readFileState();
    let changed = false;
    for (const [pollToken, pending] of Object.entries(state.pendingActivations || {})) {
      if (!isHumanActivationCode(pending.activationCode)) {
        state.pendingActivations[pollToken] = {
          ...pending,
          activationCode: await createUniqueActivationCode()
        };
        changed = true;
      }
    }
    if (changed) {
      writeFileState(state);
    }
    return;
  }

  const result = await pool.query(
    "SELECT poll_token, activation_code FROM pending_activations"
  );
  for (const row of result.rows) {
    if (!isHumanActivationCode(row.activation_code)) {
      await pool.query(
        "UPDATE pending_activations SET activation_code = $2 WHERE poll_token = $1",
        [row.poll_token, await createUniqueActivationCode()]
      );
    }
  }
}

function createFileSeed() {
  return {
    store: createDefaultStore(),
    pendingActivations: {},
    bindings: {},
    deviceStatuses: {}
  };
}

const LEGACY_DEMO_ROOM_NUMBERS = new Set([
  "1201",
  "1202",
  "1203",
  "1204",
  "1205",
  "1206",
  "1207",
  "1208",
  "1301",
  "1302",
  "1303",
  "1304",
  "1305",
  "1306",
  "1307",
  "1308"
]);

function looksLikeLegacyDemoStore(store) {
  if (!store) {
    return false;
  }

  const roomNumbers = Object.keys(store.rooms || {});
  const hasLegacyRooms = roomNumbers.some((roomNumber) => LEGACY_DEMO_ROOM_NUMBERS.has(roomNumber));
  const hasLegacyBreakfast =
    Array.isArray(store.meals?.breakfast) &&
    store.meals.breakfast.some((item) => item?.id === "breakfast_tray" || item?.id === "breakfast_lobby");

  return (
    store.property?.id === "asteria-grand-main" ||
    store.property?.name === "Asteria Grand" ||
    store.hotel?.hotelName === "Asteria Grand" ||
    hasLegacyRooms ||
    hasLegacyBreakfast
  );
}

function normalizeProductionStore(store) {
  const normalized = normalizeStore(store);
  return looksLikeLegacyDemoStore(normalized) ? createDefaultStore() : normalized;
}

function readFileState() {
  if (!fs.existsSync(STORE_PATH)) {
    const seed = createFileSeed();
    fs.writeFileSync(STORE_PATH, JSON.stringify(seed, null, 2));
    return seed;
  }
  const parsed = JSON.parse(fs.readFileSync(STORE_PATH, "utf8"));
  if (parsed.store) {
    parsed.store = normalizeProductionStore(parsed.store);
    return parsed;
  }
  return {
    store: normalizeProductionStore(parsed),
    pendingActivations: parsed.pendingActivations || {},
    bindings: parsed.bindings || {},
    deviceStatuses: parsed.deviceStatuses || {}
  };
}

function writeFileState(state) {
  fs.writeFileSync(STORE_PATH, JSON.stringify(state, null, 2));
}

async function ensureDatabase() {
  validateRuntimeConfig();
  if (!pool) {
    await repairPendingActivationCodes();
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
      bound_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      expires_at TIMESTAMPTZ,
      revoked_at TIMESTAMPTZ
    );

    CREATE TABLE IF NOT EXISTS device_status_reports (
      session_token TEXT PRIMARY KEY,
      room_number TEXT NOT NULL,
      device_id TEXT NOT NULL,
      mac_address TEXT,
      model_name TEXT,
      launcher_version TEXT,
      api_base_url TEXT,
      installed_apps JSONB NOT NULL DEFAULT '[]'::jsonb,
      source_inputs JSONB NOT NULL DEFAULT '[]'::jsonb,
      security_flags JSONB NOT NULL DEFAULT '[]'::jsonb,
      reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      client_ip TEXT,
      user_agent TEXT
    );
  `);
  await pool.query(`
    ALTER TABLE admin_sessions ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
    ALTER TABLE admin_sessions ADD COLUMN IF NOT EXISTS client_ip TEXT;
    ALTER TABLE admin_sessions ADD COLUMN IF NOT EXISTS user_agent TEXT;
    ALTER TABLE device_bindings ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
    ALTER TABLE device_bindings ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;
    ALTER TABLE device_status_reports ADD COLUMN IF NOT EXISTS security_flags JSONB NOT NULL DEFAULT '[]'::jsonb;
  `);

  const existing = await pool.query(
    "SELECT data FROM launcher_store WHERE store_key = 'global' LIMIT 1"
  );
  let resetLegacyDemoData = false;
  if (existing.rowCount === 0) {
    await pool.query(
      "INSERT INTO launcher_store (store_key, data) VALUES ('global', $1::jsonb)",
      [JSON.stringify(createDefaultStore())]
    );
  } else {
    const nextStore = normalizeProductionStore(existing.rows[0].data);
    resetLegacyDemoData = looksLikeLegacyDemoStore(existing.rows[0].data);
    await pool.query(
      "UPDATE launcher_store SET data = $1::jsonb, updated_at = NOW() WHERE store_key = 'global'",
      [JSON.stringify(nextStore)]
    );
  }

  if (resetLegacyDemoData) {
    await pool.query("TRUNCATE pending_activations, device_bindings, device_status_reports");
  }

  await repairPendingActivationCodes();

  const activeSuperAdminResult = await pool.query(
    `SELECT COUNT(*)::int AS count
     FROM admin_users
     WHERE role = $1 AND status = 'ACTIVE'`,
    [ROLES.SUPER_ADMIN]
  );

  if (activeSuperAdminResult.rows[0].count === 0) {
    if (!hasFirstAdminConfig()) {
      const config = firstAdminConfig();
      if (IS_PRODUCTION) {
        throw new Error(
          `No active super admin found. Set FIRST_ADMIN_NAME, FIRST_ADMIN_EMAIL, and FIRST_ADMIN_PASSWORD. Current values: name=${Boolean(config.name)}, email=${Boolean(config.email)}, password=${Boolean(config.password)}`
        );
      }
    } else {
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
  return normalizeProductionStore(result.rows[0].data);
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
  ).filter((user) => user.status !== "ARCHIVED");
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

async function countActiveSuperAdmins(excludedUserId = null) {
  if (!pool) {
    return seededUsers(defaultProperty().id).filter(
      (user) => user.role === ROLES.SUPER_ADMIN && user.status === "ACTIVE" && user.id !== excludedUserId
    ).length;
  }
  const result = await pool.query(
    `SELECT COUNT(*)::int AS count
     FROM admin_users
     WHERE role = $1
       AND status = 'ACTIVE'
       AND ($2::text IS NULL OR id <> $2::text)`,
    [ROLES.SUPER_ADMIN, excludedUserId]
  );
  return result.rows[0]?.count || 0;
}

async function archiveUser(userId) {
  if (!pool) {
    return null;
  }
  const current = await findUserById(userId);
  if (!current) {
    return null;
  }
  if (current.role === ROLES.SUPER_ADMIN && current.status === "ACTIVE") {
    const others = await countActiveSuperAdmins(userId);
    if (others === 0) {
      throw new Error("At least one active super admin must remain.");
    }
  }
  await pool.query(
    `UPDATE admin_users
     SET status = 'ARCHIVED'
     WHERE id = $1`,
    [userId]
  );
  return safeUser({
    ...current,
    status: "ARCHIVED"
  });
}

async function createSession(userId, metadata = {}) {
  const sessionToken = randomToken(24);
  const expiresAt = new Date(Date.now() + SESSION_TTL_HOURS * 60 * 60 * 1000).toISOString();
  if (pool) {
    await pool.query(
      "INSERT INTO admin_sessions (session_token, user_id, expires_at, last_seen_at, client_ip, user_agent) VALUES ($1, $2, $3, NOW(), $4, $5)",
      [sessionToken, userId, expiresAt, metadata.clientIp || null, metadata.userAgent || null]
    );
  }
  return { sessionToken, userId, expiresAt };
}

async function findSession(sessionToken) {
  if (!pool) {
    return null;
  }
  const result = await pool.query(
    "SELECT session_token, user_id, expires_at, last_seen_at, client_ip, user_agent FROM admin_sessions WHERE session_token = $1 LIMIT 1",
    [sessionToken]
  );
  return result.rowCount ? result.rows[0] : null;
}

async function deleteSession(sessionToken) {
  if (pool) {
    await pool.query("DELETE FROM admin_sessions WHERE session_token = $1", [sessionToken]);
  }
}

async function deleteSessionsByUserId(userId) {
  if (pool) {
    await pool.query("DELETE FROM admin_sessions WHERE user_id = $1", [userId]);
  }
}

async function extendSession(sessionToken, metadata = {}) {
  const expiresAt = new Date(Date.now() + SESSION_TTL_HOURS * 60 * 60 * 1000).toISOString();
  if (pool) {
    await pool.query(
      `UPDATE admin_sessions
       SET expires_at = $2,
           last_seen_at = NOW(),
           client_ip = COALESCE($3, client_ip),
           user_agent = COALESCE($4, user_agent)
       WHERE session_token = $1`,
      [sessionToken, expiresAt, metadata.clientIp || null, metadata.userAgent || null]
    );
  }
  return { sessionToken, expiresAt };
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
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at, expires_at, revoked_at FROM device_bindings WHERE revoked_at IS NULL ORDER BY bound_at DESC"
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
        boundAt: row.bound_at,
        expiresAt: row.expires_at,
        revokedAt: row.revoked_at
      }
    ])
  );
}

async function findBindingBySessionToken(sessionToken) {
  if (!pool) {
    return readFileState().bindings[sessionToken] || null;
  }
  const result = await pool.query(
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at, expires_at, revoked_at FROM device_bindings WHERE session_token = $1 LIMIT 1",
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
    boundAt: row.bound_at,
    expiresAt: row.expires_at,
    revokedAt: row.revoked_at
  };
}

async function requireActiveBinding(sessionToken) {
  const binding = await findBindingBySessionToken(sessionToken);
  if (!bindingIsActive(binding)) {
    if (binding?.sessionToken) {
      await deleteDeviceStatusBySessionToken(binding.sessionToken);
      await deleteBindingBySessionToken(binding.sessionToken);
    }
    return null;
  }
  return binding;
}

async function findBindingByPollToken(pollToken) {
  if (!pool) {
    return Object.values(readFileState().bindings).find((item) => item.pollToken === pollToken) || null;
  }
  const result = await pool.query(
    "SELECT session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at, expires_at, revoked_at FROM device_bindings WHERE poll_token = $1 LIMIT 1",
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
    boundAt: row.bound_at,
    expiresAt: row.expires_at,
    revokedAt: row.revoked_at
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
      (session_token, poll_token, activation_code, room_number, device_id, mac_address, bound_at, expires_at, revoked_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NULL)`,
    [
      binding.sessionToken,
      binding.pollToken,
      binding.activationCode,
      binding.roomNumber,
      binding.deviceId,
      binding.macAddress,
      binding.boundAt,
      binding.expiresAt || null
    ]
  );
  return binding;
}

async function deleteBindingBySessionToken(sessionToken) {
  if (!pool) {
    const state = readFileState();
    delete state.bindings[sessionToken];
    writeFileState(state);
    return;
  }
  await pool.query("DELETE FROM device_bindings WHERE session_token = $1", [sessionToken]);
}

async function findBindingsByRoomNumber(roomNumber) {
  const bindings = await getBindingsMap();
  return Object.values(bindings).filter((binding) => binding.roomNumber === roomNumber);
}

function normalizeReportedInstalledApps(items) {
  return Array.isArray(items)
    ? items
      .filter((item) => item && item.packageName && item.title)
      .map((item) => ({
        packageName: `${item.packageName}`.trim(),
        title: `${item.title}`.trim(),
        isSystemApp: Boolean(item.isSystemApp),
        isLaunchable: item.isLaunchable !== false
      }))
    : [];
}

function normalizeReportedSourceInputs(items) {
  return Array.isArray(items)
    ? items
      .filter((item) => item && item.id && item.title)
      .map((item) => ({
        id: `${item.id}`.trim(),
        title: `${item.title}`.trim(),
        subtitle: `${item.subtitle || ""}`.trim(),
        sourceKind: `${item.sourceKind || "OTHER"}`.trim(),
        isAvailable: item.isAvailable !== false,
        isActive: Boolean(item.isActive),
        isSystemProvided: Boolean(item.isSystemProvided),
        preferredPackageName: item.preferredPackageName || null,
        preferredActivityClassName: item.preferredActivityClassName || null,
        preferredIntentAction: item.preferredIntentAction || null,
        passthroughInputId: item.passthroughInputId || null
      }))
    : [];
}

function normalizeSecurityFlags(items) {
  return Array.isArray(items)
    ? [...new Set(items.map((item) => `${item || ""}`.trim()).filter(Boolean))].slice(0, 16)
    : [];
}

function normalizeDeviceStatusReport(binding, payload) {
  return {
    sessionToken: binding.sessionToken,
    roomNumber: binding.roomNumber,
    deviceId: `${payload.deviceId || binding.deviceId || ""}`.trim(),
    macAddress: payload.macAddress || binding.macAddress || null,
    modelName: payload.modelName || null,
    launcherVersion: payload.launcherVersion || null,
    apiBaseUrl: payload.apiBaseUrl || null,
    installedApps: normalizeReportedInstalledApps(payload.installedApps),
    sourceInputs: normalizeReportedSourceInputs(payload.sourceInputs),
    securityFlags: normalizeSecurityFlags(payload.securityFlags),
    reportedAt: nowIso()
  };
}

async function getDeviceStatusesMap() {
  if (!pool) {
    return readFileState().deviceStatuses || {};
  }
  const result = await pool.query(
    `SELECT
      session_token,
      room_number,
      device_id,
      mac_address,
      model_name,
      launcher_version,
      api_base_url,
      installed_apps,
      source_inputs,
      security_flags,
      reported_at
     FROM device_status_reports
     ORDER BY reported_at DESC`
  );
  return Object.fromEntries(
    result.rows.map((row) => [
      row.session_token,
      {
        sessionToken: row.session_token,
        roomNumber: row.room_number,
        deviceId: row.device_id,
        macAddress: row.mac_address,
        modelName: row.model_name,
        launcherVersion: row.launcher_version,
        apiBaseUrl: row.api_base_url,
        installedApps: normalizeReportedInstalledApps(row.installed_apps),
        sourceInputs: normalizeReportedSourceInputs(row.source_inputs),
        securityFlags: normalizeSecurityFlags(row.security_flags),
        reportedAt: row.reported_at
      }
    ])
  );
}

async function saveDeviceStatusReport(report) {
  if (!pool) {
    const state = readFileState();
    state.deviceStatuses = state.deviceStatuses || {};
    state.deviceStatuses[report.sessionToken] = report;
    writeFileState(state);
    return report;
  }
  await pool.query(
    `INSERT INTO device_status_reports
      (
        session_token,
        room_number,
        device_id,
        mac_address,
        model_name,
        launcher_version,
        api_base_url,
        installed_apps,
        source_inputs,
        security_flags,
        reported_at
      )
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8::jsonb, $9::jsonb, $10::jsonb, $11)
     ON CONFLICT (session_token)
     DO UPDATE SET
       room_number = EXCLUDED.room_number,
       device_id = EXCLUDED.device_id,
       mac_address = EXCLUDED.mac_address,
       model_name = EXCLUDED.model_name,
       launcher_version = EXCLUDED.launcher_version,
       api_base_url = EXCLUDED.api_base_url,
       installed_apps = EXCLUDED.installed_apps,
       source_inputs = EXCLUDED.source_inputs,
       security_flags = EXCLUDED.security_flags,
       reported_at = EXCLUDED.reported_at`,
    [
      report.sessionToken,
      report.roomNumber,
      report.deviceId,
      report.macAddress,
      report.modelName,
      report.launcherVersion,
      report.apiBaseUrl,
      JSON.stringify(report.installedApps || []),
      JSON.stringify(report.sourceInputs || []),
      JSON.stringify(report.securityFlags || []),
      report.reportedAt
    ]
  );
  return report;
}

async function deleteDeviceStatusBySessionToken(sessionToken) {
  if (!pool) {
    const state = readFileState();
    if (state.deviceStatuses) {
      delete state.deviceStatuses[sessionToken];
      writeFileState(state);
    }
    return;
  }
  await pool.query("DELETE FROM device_status_reports WHERE session_token = $1", [sessionToken]);
}

function activeRoomsMap(store) {
  return Object.fromEntries(
    Object.entries(store.rooms || {})
      .filter(([, room]) => !room.archivedAt)
      .sort(([left], [right]) => left.localeCompare(right))
  );
}

function activeMeals(store) {
  const result = {};
  MEAL_CATEGORIES.forEach((category) => {
    result[category] = (store.meals?.[category] || []).filter((item) => !item.archivedAt);
  });
  return result;
}

function buildAvailableApps(store, deviceStatuses) {
  const appMap = new Map();
  (store.availableApps || []).forEach((app) => {
    appMap.set(app.packageName, {
      ...app,
      installedOnDevices: 0,
      lastSeenAt: null
    });
  });
  Object.values(deviceStatuses).forEach((status) => {
    (status.installedApps || []).forEach((app) => {
      const existing = appMap.get(app.packageName) || {
        id: `reported_${app.packageName.replace(/[^a-zA-Z0-9]+/g, "_").toLowerCase()}`,
        packageName: app.packageName,
        name: app.title,
        description: app.isSystemApp ? "System app reported by bound TVs" : "App reported by bound TVs",
        installedOnDevices: 0,
        lastSeenAt: null
      };
      existing.installedOnDevices += 1;
      if (!existing.lastSeenAt || new Date(status.reportedAt).getTime() > new Date(existing.lastSeenAt).getTime()) {
        existing.lastSeenAt = status.reportedAt;
      }
      appMap.set(app.packageName, existing);
    });
  });
  return [...appMap.values()].sort((left, right) => `${left.name}`.localeCompare(`${right.name}`));
}

function buildAvailableInputs(store, deviceStatuses) {
  const inputMap = new Map();
  (store.availableInputs || []).forEach((input) => {
    inputMap.set(input.title, {
      ...input,
      detectedOnDevices: 0,
      activeOnDevices: 0,
      lastSeenAt: null
    });
  });
  Object.values(deviceStatuses).forEach((status) => {
    (status.sourceInputs || []).forEach((input) => {
      const existing = inputMap.get(input.title) || {
        id: `reported_${input.id.replace(/[^a-zA-Z0-9]+/g, "_").toLowerCase()}`,
        title: input.title,
        description: input.subtitle || `${input.sourceKind} source reported by bound TVs`,
        detectedOnDevices: 0,
        activeOnDevices: 0,
        lastSeenAt: null
      };
      existing.detectedOnDevices += 1;
      if (input.isActive) {
        existing.activeOnDevices += 1;
      }
      if (!existing.lastSeenAt || new Date(status.reportedAt).getTime() > new Date(existing.lastSeenAt).getTime()) {
        existing.lastSeenAt = status.reportedAt;
      }
      inputMap.set(input.title, existing);
    });
  });
  return [...inputMap.values()].sort((left, right) => `${left.title}`.localeCompare(`${right.title}`));
}

function buildAssetEntries(store) {
  const assets = [];
  if (store.hotel?.startupLogoUrl) {
    const previewUrl = buildImageKitDeliveryUrl(store.hotel.startupLogoUrl, { kind: "startup" });
    assets.push({
      id: crypto.createHash("md5").update(`startup:${previewUrl}`).digest("hex"),
      kind: "startup",
      bucket: "startup",
      url: previewUrl,
      storedUrl: store.hotel.startupLogoUrl
    });
  }
  Object.entries(store.backgrounds || {}).forEach(([bucket, urls]) => {
    (urls || []).forEach((url) => {
      const previewUrl = buildImageKitDeliveryUrl(url, { kind: "background" });
      assets.push({
        id: crypto.createHash("md5").update(`${bucket}:${previewUrl}`).digest("hex"),
        kind: "background",
        bucket,
        url: previewUrl,
        storedUrl: url
      });
    });
  });
  return assets;
}

function bindingIsActive(binding) {
  if (!binding || binding.revokedAt) {
    return false;
  }
  if (binding.expiresAt && new Date(binding.expiresAt).getTime() <= Date.now()) {
    return false;
  }
  return true;
}

function deviceFreshness(reportedAt) {
  if (!reportedAt) {
    return "offline";
  }
  const ageMs = Date.now() - new Date(reportedAt).getTime();
  if (ageMs <= DEVICE_ONLINE_WINDOW_MS) {
    return "online";
  }
  if (ageMs <= DEVICE_STALE_WINDOW_MS) {
    return "stale";
  }
  return "offline";
}

function bestEffortDeleteLocalAsset(url) {
  if (!url || typeof url !== "string" || !url.startsWith("/uploads/")) {
    return;
  }
  const relativePath = url.replace(/^\/+/, "").replaceAll("/", path.sep);
  const assetPath = path.join(ROOT, relativePath);
  if (assetPath.startsWith(UPLOAD_DIR) && fs.existsSync(assetPath)) {
    fs.unlinkSync(assetPath);
  }
}

function buildMetrics(store, pendingActivations, bindings, deviceStatuses) {
  const rooms = Object.values(activeRoomsMap(store));
  const freshnessValues = Object.values(deviceStatuses).map((status) => deviceFreshness(status.reportedAt));
  const onlineTvs = freshnessValues.filter((state) => state === "online").length;
  const staleTvs = freshnessValues.filter((state) => state === "stale").length;
  return {
    onlineTvs,
    staleTvs,
    occupiedRooms: rooms.filter((room) => room.status === "occupied").length,
    pendingBindings: Object.keys(pendingActivations).length,
    unboundDevices: rooms.filter((room) => room.status === "unbound").length,
    vacantRooms: rooms.filter((room) => room.status === "vacant").length,
    offlineRooms: Math.max(rooms.length - onlineTvs, 0),
    syncVersion: Number(store.sync?.version || 0)
  };
}

async function buildAdminState(user) {
  const store = await getStore();
  const pendingActivations = await getPendingActivationsMap();
  const bindings = await getBindingsMap();
  const deviceStatuses = Object.fromEntries(
    Object.entries(await getDeviceStatusesMap()).map(([sessionToken, status]) => [
      sessionToken,
      {
        ...status,
        freshness: deviceFreshness(status.reportedAt)
      }
    ])
  );
  const metrics = buildMetrics(store, pendingActivations, bindings, deviceStatuses);
  const rooms = activeRoomsMap(store);
  const meals = activeMeals(store);
  const availableApps = buildAvailableApps(store, deviceStatuses);
  const availableInputs = buildAvailableInputs(store, deviceStatuses);
  const assets = buildAssetEntries(store);
  const lastDeviceHeartbeat = Object.values(deviceStatuses)
    .map((status) => status.reportedAt)
    .filter(Boolean)
    .sort()
    .at(-1) || null;
  return {
    currentUser: safeUser(user),
    roomCatalog: {
      roomCategories: ROOM_CATEGORY_OPTIONS,
      climateControls: CLIMATE_CONTROL_OPTIONS
    },
    property: store.property,
    hotel: normalizeHotelAssets(store.hotel),
    weather: store.weather,
    popup: store.popup,
    backgrounds: normalizeBackgroundMap(store.backgrounds),
    meals,
    sections: Object.values(store.sections || {}).filter((section) => section && section.enabled !== false),
    visibility: store.visibility,
    availableApps,
    availableInputs,
    rooms,
    assets,
    pendingActivations,
    bindings,
    deviceStatuses,
    metrics,
    workflowSteps: buildWorkflowSteps(metrics),
    systemHealth: {
      apiStatus: RUNTIME_WARNINGS.length ? "Degraded" : "Healthy",
      lastPushTime: store.sync.updatedAt,
      lastDeviceHeartbeat,
      launcherVersion:
        Object.values(deviceStatuses)
          .map((status) => status.launcherVersion)
          .find(Boolean) || "Not configured",
      syncVersion: store.sync.version,
      warnings: RUNTIME_WARNINGS
    },
    notifications: (store.notifications || []).slice(0, 10),
    auditLogs: canModuleAction(user.role, "audit", "view") ? (store.auditLogs || []).slice(0, 50) : [],
    users: canModuleAction(user.role, "users", "view") ? await listUsers() : [],
    settings:
      canModuleAction(user.role, "settings", "view")
        ? {
            property: store.property,
            integration: {
              apiKeyPreview: store.property.apiKeyPreview ? "Configured" : "",
              webhookUrl: PANEL_WEBHOOK_URL ? "Configured" : ""
            },
            subscription: {
              plan: PANEL_SUBSCRIPTION_PLAN || "",
              renewalDate: PANEL_SUBSCRIPTION_RENEWAL_DATE || "",
              environment: store.property.environment || ""
            }
          }
        : null
  };
}

function buildPayload(store, binding) {
  const room = store.rooms[binding.roomNumber] || { roomNumber: binding.roomNumber, guestName: "" };
  const meals = activeMeals(store);
  return {
    roomNumber: room.roomNumber,
    guestName: room.guestName || "",
    hotel: normalizeHotelAssets(store.hotel),
    weather: store.weather,
    popup: store.popup,
    backgrounds: normalizeBackgroundMap(store.backgrounds),
    meals,
    sections: Object.values(store.sections || {}).filter((section) => section && section.enabled !== false),
    notifications: (store.notifications || []).slice(0, 10),
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

function buildImageKitUploadAuth() {
  const expire = Math.floor(Date.now() / 1000) + 10 * 60;
  const token = randomToken(16);
  const signature = crypto
    .createHmac("sha1", IMAGEKIT_PRIVATE_KEY)
    .update(`${token}${expire}`)
    .digest("hex");
  return { token, expire, signature };
}

function buildImageKitDeliveryUrl(sourceUrl, options = {}) {
  if (!sourceUrl) {
    return sourceUrl;
  }
  try {
    const parsed = new URL(sourceUrl);
    const transforms = [];
    if (options.kind === "background") {
      transforms.push("w-1920", "h-1080", "c-at_max", "q-80", "f-webp");
    } else if (options.kind === "startup" && `${options.mimeType || ""}`.startsWith("image/")) {
      transforms.push("w-1600", "h-900", "c-at_max", "q-85");
    }
    if (transforms.length === 0) {
      return sourceUrl;
    }
    const pathSegments = parsed.pathname.split("/").filter(Boolean);
    if (pathSegments.length === 0) {
      return sourceUrl;
    }

    let endpoint = pathSegments.shift();
    if (endpoint?.startsWith("tr:")) {
      endpoint = pathSegments.shift();
    }
    if (!endpoint) {
      return sourceUrl;
    }

    while (pathSegments[0]?.startsWith("tr:")) {
      pathSegments.shift();
    }

    const normalizedPath = `/${[endpoint, `tr:${transforms.join(",")}`, ...pathSegments].join("/")}`;
    return `${parsed.origin}${normalizedPath}${parsed.search}`;
  } catch {
    return sourceUrl;
  }
}

function normalizeBackgroundMap(backgrounds = {}) {
  return Object.fromEntries(
    Object.entries(backgrounds).map(([bucket, urls]) => [
      bucket,
      (urls || []).map((url) => buildImageKitDeliveryUrl(url, { kind: "background" }))
    ])
  );
}

function normalizeHotelAssets(hotel = {}) {
  return {
    ...hotel,
    startupLogoUrl: buildImageKitDeliveryUrl(hotel.startupLogoUrl, { kind: "startup", mimeType: "image/png" })
  };
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
      ? "/hotel-central-admin-panel/startup"
      : `/hotel-central-admin-panel/backgrounds/${bucket}`;
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
  ensureImageKitConfigured();
  try {
    const remoteUrl = await uploadToImageKit(file, kind, bucket);
    if (remoteUrl) {
      return remoteUrl;
    }
  } catch (error) {
    if (IS_PRODUCTION) {
      throw error;
    }
    console.error("ImageKit upload failed, falling back to local storage:", error.message);
  }
  return saveFileLocally(file, kind, bucket);
}

function asyncHandler(fn) {
  return (req, res, next) => {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

function isValidHttpsUrl(value) {
  try {
    const parsed = new URL(value);
    return parsed.protocol === "https:";
  } catch {
    return false;
  }
}

function assertImageKitUrl(value) {
  if (!isValidHttpsUrl(value)) {
    return false;
  }
  return value.includes("imagekit.io");
}

async function attachCurrentUser(req, res = null) {
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
  const remainingMs = new Date(session.expires_at || session.expiresAt).getTime() - Date.now();
  if (res && remainingMs <= SESSION_RENEWAL_HOURS * 60 * 60 * 1000) {
    const extended = await extendSession(sessionToken, {
      clientIp: clientIp(req),
      userAgent: req.headers["user-agent"] || null
    });
    res.cookie(SESSION_COOKIE, sessionToken, sessionCookieOptions(new Date(extended.expiresAt).getTime() - Date.now()));
    session.expires_at = extended.expiresAt;
  }
  const userId = session.user_id || session.userId;
  const user = await findUserById(userId);
  return user ? { user, session } : null;
}

function requireAuth(handler) {
  return asyncHandler(async (req, res, next) => {
    const attached = await attachCurrentUser(req, res);
    const user = attached?.user;
    if (!user || user.status !== "ACTIVE") {
      res.clearCookie(SESSION_COOKIE);
      return res.status(401).json({ error: "Authentication required" });
    }
    req.currentUser = user;
    req.currentSession = attached.session;
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

function requireModuleAction(moduleKey, action, handler) {
  return requireAuth(async (req, res, next) => {
    if (!canModuleAction(req.currentUser.role, moduleKey, action)) {
      return res.status(403).json({ error: "Forbidden" });
    }
    return handler(req, res, next);
  });
}

function ensureConfigPermission(user, payload = {}) {
  const checks = [];
  if (payload.hotel || payload.weather || payload.popup || payload.backgrounds || payload.sections) {
    checks.push(["content", "edit"]);
  }

  const visibility = payload.visibility || {};
  if (Object.prototype.hasOwnProperty.call(visibility, "destinations")) {
    checks.push(["content", "edit"]);
  }
  if (
    Object.prototype.hasOwnProperty.call(visibility, "visibleAppPackages") ||
    Object.prototype.hasOwnProperty.call(visibility, "visibleSourceTitles")
  ) {
    checks.push(["policies", "edit"]);
  }
  if (payload.meals) {
    checks.push(["menus", "edit"]);
  }
  if (payload.property || payload.sync) {
    checks.push(["settings", "edit"]);
  }

  const denied = checks.find(([moduleKey, action]) => !canModuleAction(user.role, moduleKey, action));
  return !denied;
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
      service: "hotel-central-admin-panel",
      database,
      imageStorage: IMAGEKIT_PRIVATE_KEY ? "imagekit" : "local",
      sessionSecret: SESSION_SECRET ? "configured" : SESSION_SECRET_IS_DERIVED ? "derived" : "missing",
      warnings: RUNTIME_WARNINGS
    });
  })
);

app.post(
  "/api/auth/login",
  rateLimitBy("auth-login", { windowMs: 15 * 60 * 1000, max: 10 }, (req) => `${clientIp(req)}:${`${req.body?.email || ""}`.trim().toLowerCase()}`),
  asyncHandler(async (req, res) => {
    const email = `${req.body.email || ""}`.trim().toLowerCase();
    const password = `${req.body.password || ""}`;
    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }
    const user = await findUserByEmail(email);
    if (!user || user.status !== "ACTIVE" || !verifyPassword(password, user.passwordHash)) {
      const store = await getStore();
      pushAudit(store, {
        actorName: email || "Unknown account",
        actorRole: "ANONYMOUS",
        action: `Failed login attempt from ${clientIp(req)}`,
        entityType: "AUTH",
        entityId: email || "unknown",
        tone: "warning"
      });
      await saveStore(store);
      return res.status(401).json({ error: "Invalid email or password" });
    }
    const session = await createSession(user.id, {
      clientIp: clientIp(req),
      userAgent: req.headers["user-agent"] || null
    });
    res.cookie(SESSION_COOKIE, session.sessionToken, sessionCookieOptions(new Date(session.expiresAt).getTime() - Date.now()));
    const store = await getStore();
    pushAudit(store, {
      actorName: user.name,
      actorRole: user.role,
      action: `Successful login from ${clientIp(req)}`,
      entityType: "AUTH",
      entityId: user.id,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, user: safeUser(user) });
  })
);

app.post(
  "/api/auth/logout",
  requireAuth(async (req, res) => {
    await deleteSession(req.cookies[SESSION_COOKIE]);
    const store = await getStore();
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: "Logged out and revoked session",
      entityType: "AUTH",
      entityId: req.currentUser.id,
      tone: "info"
    });
    await saveStore(store);
    res.clearCookie(SESSION_COOKIE);
    res.json({ ok: true });
  })
);

app.get(
  "/api/auth/me",
  asyncHandler(async (req, res) => {
    const attached = await attachCurrentUser(req, res);
    const user = attached?.user;
    if (!user || user.status !== "ACTIVE") {
      return res.status(401).json({ error: "Not authenticated" });
    }
    res.json({ user: safeUser(user) });
  })
);

app.post(
  "/api/device/activate",
  rateLimitBy("device-activate", { windowMs: 60 * 1000, max: 30 }, (req) => `${clientIp(req)}:${`${req.body?.deviceId || ""}`.trim()}`),
  asyncHandler(async (req, res) => {
    const deviceId = req.body.deviceId || randomToken(6);
    const macAddress = req.body.macAddress || null;
    const existing = await findPendingByDeviceId(deviceId);
    if (existing) {
      if (!isHumanActivationCode(existing.activationCode)) {
        await deletePendingActivation(existing.pollToken);
      } else {
        return res.json(existing);
      }
    }
    const activation = {
      pollToken: randomToken(12),
      activationCode: await createUniqueActivationCode(),
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
    const binding = await requireActiveBinding(sessionToken);
    if (!binding) {
      return res.status(401).json({ error: "Invalid session token" });
    }
    const store = await getStore();
    res.json(buildPayload(store, binding));
  })
);

app.post(
  "/api/device/status",
  rateLimitBy("device-status", { windowMs: 60 * 1000, max: 180 }, (req) => `${clientIp(req)}:${`${req.body?.sessionToken || ""}`.trim()}`),
  asyncHandler(async (req, res) => {
    const sessionToken = `${req.body.sessionToken || ""}`.trim();
    if (!sessionToken) {
      return res.status(400).json({ error: "sessionToken is required" });
    }
    const binding = await requireActiveBinding(sessionToken);
    if (!binding) {
      return res.status(401).json({ error: "Invalid session token" });
    }
    const report = normalizeDeviceStatusReport(binding, req.body || {});
    await saveDeviceStatusReport(report);

    const store = await getStore();
    const room = store.rooms[binding.roomNumber];
    if (room) {
      store.rooms[binding.roomNumber] = normalizeRoom(binding.roomNumber, {
        ...room,
        deviceId: report.deviceId || room.deviceId,
        hasBindingHistory: true,
        lastSyncAt: report.reportedAt
      }, store.sync.updatedAt);
      await saveStore(store);
    }

    res.json({ ok: true, reportedAt: report.reportedAt });
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
  requireModuleAction("users", "view", async (_, res) => {
    res.json({ users: await listUsers() });
  })
);

app.post(
  "/api/admin/users",
  requireModuleAction("users", "create", async (req, res) => {
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
  requireModuleAction("users", "edit", async (req, res) => {
    const current = await findUserById(req.params.userId);
    if (!current) {
      return res.status(404).json({ error: "User not found" });
    }
    if (
      current.role === ROLES.SUPER_ADMIN &&
      current.status === "ACTIVE" &&
      ((req.body.role && req.body.role !== ROLES.SUPER_ADMIN) || (req.body.status && req.body.status !== "ACTIVE"))
    ) {
      const others = await countActiveSuperAdmins(req.params.userId);
      if (others === 0) {
        return res.status(400).json({ error: "At least one active super admin must remain." });
      }
    }
    const invalidatesSessions =
      Boolean(req.body.password) ||
      (req.body.role && req.body.role !== current.role) ||
      (req.body.status && req.body.status !== current.status);
    const user = await updateUser(req.params.userId, req.body || {});
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }
    if (invalidatesSessions) {
      await deleteSessionsByUserId(req.params.userId);
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
    if (invalidatesSessions) {
      pushAudit(store, {
        actorName: req.currentUser.name,
        actorRole: req.currentUser.role,
        action: `Revoked active sessions for ${user.name}`,
        entityType: "AUTH",
        entityId: user.id,
        tone: "warning"
      });
    }
    await saveStore(store);
    res.json({ ok: true, user });
  })
);

app.delete(
  "/api/admin/users/:userId",
  requireModuleAction("users", "delete", async (req, res) => {
    let user;
    try {
      user = await archiveUser(req.params.userId);
    } catch (error) {
      return res.status(400).json({ error: error.message || "Unable to archive user" });
    }
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }
    await deleteSessionsByUserId(req.params.userId);
    const store = await getStore();
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Archived user account for ${user.name}`,
      entityType: "USER",
      entityId: user.id,
      tone: "warning"
    });
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Revoked active sessions for archived user ${user.name}`,
      entityType: "AUTH",
      entityId: user.id,
      tone: "warning"
    });
    await saveStore(store);
    res.json({ ok: true, user });
  })
);

app.post(
  "/api/admin/config",
  requireAuth(async (req, res) => {
    const store = await getStore();
    const next = req.body || {};
    if (!ensureConfigPermission(req.currentUser, next)) {
      return res.status(403).json({ error: "Forbidden" });
    }
    store.hotel = { ...store.hotel, ...(next.hotel || {}) };
    store.weather = { ...store.weather, ...(next.weather || {}) };
    store.popup = { ...store.popup, ...(next.popup || {}) };
    store.backgrounds = { ...store.backgrounds, ...(next.backgrounds || {}) };
    if (next.meals) {
      store.meals = {
        ...store.meals,
        ...Object.fromEntries(
          MEAL_CATEGORIES.map((category) => [
            category,
            Array.isArray(next.meals?.[category])
              ? next.meals[category].map((item) => normalizeMenuItem(category, item))
              : store.meals[category] || []
          ])
        )
      };
    }
    if (next.sections) {
      const mergedSections = Array.isArray(next.sections)
        ? next.sections
        : Object.values(next.sections);
      mergedSections.forEach((section) => {
        if (section?.id) {
          store.sections[section.id] = section;
        }
      });
    }
    if (next.visibility) {
      store.visibility = {
        ...store.visibility,
        ...next.visibility,
        visibleAppPackages: Array.isArray(next.visibility.visibleAppPackages)
          ? next.visibility.visibleAppPackages.filter(Boolean)
          : store.visibility.visibleAppPackages,
        visibleSourceTitles: Array.isArray(next.visibility.visibleSourceTitles)
          ? next.visibility.visibleSourceTitles.filter(Boolean)
          : store.visibility.visibleSourceTitles
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
      action: "Updated launcher configuration",
      entityType: "LAUNCHER_CONFIG",
      entityId: "global",
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, store });
  })
);

app.get(
  "/api/admin/rooms",
  requireModuleAction("rooms", "view", async (_, res) => {
    const store = await getStore();
    res.json({ rooms: activeRoomsMap(store) });
  })
);

app.post(
  "/api/admin/rooms",
  requireModuleAction("rooms", "create", async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
    if (!roomNumber) {
      return res.status(400).json({ error: "roomNumber is required" });
    }
    const existing = store.rooms[roomNumber];
    if (existing && !existing.archivedAt) {
      return res.status(409).json({ error: "Room already exists" });
    }

    store.rooms[roomNumber] = normalizeRoom(
      roomNumber,
      {
        ...(existing || {}),
        roomNumber,
        floor: req.body.floor || existing?.floor || roomNumber.slice(0, Math.max(1, roomNumber.length - 2)),
        roomCategory: normalizeRoomCategory(req.body.roomCategory || existing?.roomCategory),
        climateControl: normalizeClimateControl(req.body.climateControl || existing?.climateControl),
        status: "unbound",
        guestName: "",
        deviceId: "",
        checkInAt: "",
        welcomeNote: "",
        archivedAt: null,
        archivedBy: null,
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );

    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: existing?.archivedAt ? `Restored room ${roomNumber}` : `Created new room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });

    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.post(
  "/api/admin/rooms/create",
  requireModuleAction("rooms", "create", async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
    if (!roomNumber) {
      return res.status(400).json({ error: "roomNumber is required" });
    }
    const existing = store.rooms[roomNumber];
    if (existing && !existing.archivedAt) {
      return res.status(409).json({ error: "Room already exists" });
    }
    store.rooms[roomNumber] = normalizeRoom(
      roomNumber,
      {
        ...(existing || {}),
        roomNumber,
        floor: req.body.floor || existing?.floor || roomNumber.slice(0, Math.max(1, roomNumber.length - 2)),
        roomCategory: normalizeRoomCategory(req.body.roomCategory || existing?.roomCategory),
        climateControl: normalizeClimateControl(req.body.climateControl || existing?.climateControl),
        status: "unbound",
        guestName: "",
        deviceId: "",
        checkInAt: "",
        welcomeNote: "",
        archivedAt: null,
        archivedBy: null,
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: existing?.archivedAt ? `Restored room ${roomNumber}` : `Created new room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.patch(
  "/api/admin/rooms/:roomNumber",
  requireModuleAction("rooms", "edit", async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.params.roomNumber || ""}`.trim();
    const current = store.rooms[roomNumber];
    if (!current || current.archivedAt) {
      return res.status(404).json({ error: "Room not found" });
    }
    const nextGuestName = req.body.guestName === undefined ? current.guestName : `${req.body.guestName || ""}`.trim();
    store.rooms[roomNumber] = normalizeRoom(
      roomNumber,
      {
        ...current,
        floor: `${req.body.floor ?? current.floor ?? ""}`.trim(),
        roomCategory: normalizeRoomCategory(req.body.roomCategory ?? current.roomCategory),
        climateControl: normalizeClimateControl(req.body.climateControl ?? current.climateControl),
        guestName: nextGuestName,
        welcomeNote: `${req.body.welcomeNote ?? current.welcomeNote ?? ""}`.trim(),
        language: `${req.body.language ?? current.language ?? "English"}`.trim(),
        status: `${req.body.status || (nextGuestName ? "occupied" : current.deviceId ? "vacant" : "unbound")}`.trim(),
        checkInAt: nextGuestName ? current.checkInAt || nowIso() : "",
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Updated room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "info"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.post(
  "/api/admin/rooms/:roomNumber/checkin",
  requireModuleAction("sessions", "create", async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.params.roomNumber || ""}`.trim();
    const room = store.rooms[roomNumber];
    if (!room || room.archivedAt) {
      return res.status(404).json({ error: "Room not found" });
    }
    const guestName = `${req.body.guestName || ""}`.trim();
    if (!guestName) {
      return res.status(400).json({ error: "guestName is required" });
    }
    store.rooms[roomNumber] = normalizeRoom(
      roomNumber,
      {
        ...room,
        guestName,
        welcomeNote: `${req.body.welcomeNote ?? room.welcomeNote ?? ""}`.trim(),
        language: `${req.body.language ?? room.language ?? "English"}`.trim(),
        status: "occupied",
        checkInAt: room.checkInAt || nowIso(),
        hasSessionHistory: true,
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Checked in guest to room ${roomNumber}`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, room: store.rooms[roomNumber] });
  })
);

app.post(
  "/api/admin/rooms/:roomNumber/checkout",
  requireModuleAction("sessions", "delete", async (req, res) => {
    const store = await getStore();
    const room = store.rooms[req.params.roomNumber];
    if (!room || room.archivedAt) {
      return res.status(404).json({ error: "Room not found" });
    }
    store.rooms[req.params.roomNumber] = normalizeRoom(
      req.params.roomNumber,
      {
        ...room,
        guestName: "",
        welcomeNote: "",
        checkInAt: "",
        status: room.deviceId ? "vacant" : "unbound",
        hasSessionHistory: room.hasSessionHistory || Boolean(room.checkInAt),
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
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
  "/api/admin/rooms/:roomNumber/unbind",
  requireModuleAction("binding", "manage", async (req, res) => {
    const store = await getStore();
    const room = store.rooms[req.params.roomNumber];
    if (!room || room.archivedAt) {
      return res.status(404).json({ error: "Room not found" });
    }
    const bindings = await findBindingsByRoomNumber(req.params.roomNumber);
    await Promise.all(
      bindings.flatMap((binding) => [
        deleteBindingBySessionToken(binding.sessionToken),
        deleteDeviceStatusBySessionToken(binding.sessionToken)
      ])
    );
    store.rooms[req.params.roomNumber] = normalizeRoom(
      req.params.roomNumber,
      {
        ...room,
        deviceId: "",
        status: room.guestName ? "occupied" : "unbound",
        hasBindingHistory: room.hasBindingHistory || bindings.length > 0,
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Unbound TV from room ${req.params.roomNumber}`,
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
  requireModuleAction("rooms", "manage", async (req, res) => {
    const store = await getStore();
    const room = store.rooms[req.params.roomNumber];
    if (!room || room.archivedAt) {
      return res.status(404).json({ error: "Room not found" });
    }
    store.rooms[req.params.roomNumber] = normalizeRoom(
      req.params.roomNumber,
      {
        ...room,
        overrideEnabled: Boolean(req.body.enabled),
        customContentLabel: `${req.body.customContentLabel || ""}`.trim(),
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name
      },
      store.sync.updatedAt
    );
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

app.delete(
  "/api/admin/rooms/:roomNumber",
  requireModuleAction("rooms", "delete", async (req, res) => {
    const store = await getStore();
    const roomNumber = `${req.params.roomNumber || ""}`.trim();
    const room = store.rooms[roomNumber];
    if (!room) {
      return res.status(404).json({ error: "Room not found" });
    }
    const activeBindings = await findBindingsByRoomNumber(roomNumber);
    const requiresArchive =
      Boolean(room.guestName) ||
      Boolean(room.deviceId) ||
      Boolean(room.hasSessionHistory) ||
      Boolean(room.hasBindingHistory) ||
      activeBindings.length > 0;

    if (requiresArchive) {
      await Promise.all(
        activeBindings.flatMap((binding) => [
          deleteBindingBySessionToken(binding.sessionToken),
          deleteDeviceStatusBySessionToken(binding.sessionToken)
        ])
      );
      store.rooms[roomNumber] = normalizeRoom(
        roomNumber,
        {
          ...room,
          guestName: "",
          deviceId: "",
          checkInAt: "",
          welcomeNote: "",
          status: "archived",
          archivedAt: nowIso(),
          archivedBy: req.currentUser.name,
          hasSessionHistory: room.hasSessionHistory || Boolean(room.checkInAt),
          hasBindingHistory: room.hasBindingHistory || activeBindings.length > 0 || Boolean(room.deviceId),
          lastSyncAt: nowIso(),
          lastUpdatedBy: req.currentUser.name
        },
        store.sync.updatedAt
      );
      pushAudit(store, {
        actorName: req.currentUser.name,
        actorRole: req.currentUser.role,
        action: `Archived room ${roomNumber}`,
        entityType: "ROOM",
        entityId: roomNumber,
        tone: "warning"
      });
    } else {
      delete store.rooms[roomNumber];
      pushAudit(store, {
        actorName: req.currentUser.name,
        actorRole: req.currentUser.role,
        action: `Deleted unused room ${roomNumber}`,
        entityType: "ROOM",
        entityId: roomNumber,
        tone: "warning"
      });
    }

    await saveStore(store);
    res.json({ ok: true, archived: requiresArchive });
  })
);

app.get(
  "/api/admin/menus",
  requireModuleAction("menus", "view", async (_, res) => {
    const store = await getStore();
    res.json({ meals: activeMeals(store) });
  })
);

app.post(
  "/api/admin/menus/:category/items",
  requireModuleAction("menus", "create", async (req, res) => {
    const category = `${req.params.category || ""}`.trim();
    if (!MEAL_CATEGORIES.includes(category)) {
      return res.status(400).json({ error: "Invalid menu category" });
    }
    const title = `${req.body.title || ""}`.trim();
    if (!title) {
      return res.status(400).json({ error: "title is required" });
    }
    const store = await getStore();
    const item = normalizeMenuItem(category, {
      ...req.body,
      id: req.body.id || `${category}-${randomToken(6)}`,
      updatedAt: nowIso(),
      archivedAt: null,
      archivedBy: null
    });
    store.meals[category] = [...(store.meals[category] || []), item];
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Created ${category} menu item ${item.title}`,
      entityType: "MENU_ITEM",
      entityId: item.id,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, item });
  })
);

app.patch(
  "/api/admin/menus/:category/items/:itemId",
  requireModuleAction("menus", "edit", async (req, res) => {
    const category = `${req.params.category || ""}`.trim();
    if (!MEAL_CATEGORIES.includes(category)) {
      return res.status(400).json({ error: "Invalid menu category" });
    }
    const store = await getStore();
    const currentItems = store.meals[category] || [];
    const current = currentItems.find((item) => item.id === req.params.itemId && !item.archivedAt);
    if (!current) {
      return res.status(404).json({ error: "Menu item not found" });
    }
    const next = normalizeMenuItem(category, {
      ...current,
      ...req.body,
      id: current.id,
      updatedAt: nowIso(),
      archivedAt: current.archivedAt,
      archivedBy: current.archivedBy
    });
    store.meals[category] = currentItems.map((item) => (item.id === current.id ? next : item));
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Updated ${category} menu item ${next.title}`,
      entityType: "MENU_ITEM",
      entityId: next.id,
      tone: "info"
    });
    await saveStore(store);
    res.json({ ok: true, item: next });
  })
);

app.delete(
  "/api/admin/menus/:category/items/:itemId",
  requireModuleAction("menus", "delete", async (req, res) => {
    const category = `${req.params.category || ""}`.trim();
    if (!MEAL_CATEGORIES.includes(category)) {
      return res.status(400).json({ error: "Invalid menu category" });
    }
    const store = await getStore();
    const currentItems = store.meals[category] || [];
    const current = currentItems.find((item) => item.id === req.params.itemId && !item.archivedAt);
    if (!current) {
      return res.status(404).json({ error: "Menu item not found" });
    }
    store.meals[category] = currentItems.map((item) =>
      item.id === current.id
        ? normalizeMenuItem(category, {
            ...item,
            archivedAt: nowIso(),
            archivedBy: req.currentUser.name,
            updatedAt: nowIso()
          })
        : item
    );
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Archived ${category} menu item ${current.title}`,
      entityType: "MENU_ITEM",
      entityId: current.id,
      tone: "warning"
    });
    await saveStore(store);
    res.json({ ok: true });
  })
);

app.post(
  "/api/admin/bind",
  rateLimitBy("admin-bind", { windowMs: 60 * 1000, max: 40 }, (req) => `${clientIp(req)}:${req.currentUser?.id || "unknown"}`),
  requireModuleAction("binding", "create", async (req, res) => {
    const store = await getStore();
    const activationCode = `${req.body.activationCode || ""}`.trim().toUpperCase();
    const roomNumber = `${req.body.roomNumber || ""}`.trim();
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
    const floor = `${req.body.floor || current.floor || roomNumber.slice(0, Math.max(1, roomNumber.length - 2))}`.trim();
    const roomCategory = normalizeRoomCategory(req.body.roomCategory || current.roomCategory);
    const climateControl = normalizeClimateControl(req.body.climateControl || current.climateControl);
    store.rooms[roomNumber] = normalizeRoom(
      roomNumber,
      {
        ...current,
        roomNumber,
        floor,
        roomCategory,
        climateControl,
        guestName: current.guestName || "",
        welcomeNote: current.welcomeNote || "",
        status: current.guestName ? "occupied" : "vacant",
        deviceId: pendingEntry.deviceId || current.deviceId || `TV-${roomNumber}`,
        checkInAt: current.guestName ? current.checkInAt || nowIso() : current.checkInAt || "",
        lastSyncAt: nowIso(),
        lastUpdatedBy: req.currentUser.name,
        hasBindingHistory: true,
        hasSessionHistory: current.hasSessionHistory || Boolean(current.guestName),
        archivedAt: null,
        archivedBy: null
      },
      store.sync.updatedAt
    );
    const sessionToken = randomToken(16);
    const binding = {
      sessionToken,
      pollToken: pendingEntry.pollToken,
      activationCode,
      roomNumber,
      deviceId: pendingEntry.deviceId,
      macAddress: pendingEntry.macAddress,
      boundAt: nowIso(),
      expiresAt: new Date(Date.now() + 45 * 24 * 60 * 60 * 1000).toISOString()
    };
    await createBinding(binding);
    await deletePendingActivation(pendingEntry.pollToken);
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Bound activation ${activationCode} to room ${roomNumber} (${roomCategory}, ${climateControl})`,
      entityType: "ROOM",
      entityId: roomNumber,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, sessionToken, roomNumber, room: store.rooms[roomNumber] });
  })
);

app.get(
  "/api/admin/assets",
  requireModuleAction("content", "view", async (_, res) => {
    const store = await getStore();
    res.json({ assets: buildAssetEntries(store) });
  })
);

app.post(
  "/api/admin/upload-auth",
  rateLimitBy("upload-auth", { windowMs: 60 * 1000, max: 25 }, (req) => `${clientIp(req)}:${req.currentUser?.id || "unknown"}`),
  requireModuleAction("content", "create", async (req, res) => {
    if (!IMAGEKIT_PUBLIC_KEY || !IMAGEKIT_PRIVATE_KEY) {
      return res.status(503).json({ error: "ImageKit upload is not configured." });
    }
    const kind = `${req.body.kind || "background"}`.trim().toLowerCase();
    const bucket = `${req.body.bucket || "home"}`.trim().toLowerCase();
    const originalName = `${req.body.originalName || "asset"}`.trim() || "asset";
    if (!["startup", "background"].includes(kind)) {
      return res.status(400).json({ error: "Invalid upload kind" });
    }
    if (kind === "background" && !["home", "roomservice", "foodmenu", "inputs"].includes(bucket.replace(/\s+/g, "").toLowerCase())) {
      return res.status(400).json({ error: "Invalid upload bucket" });
    }
    const auth = buildImageKitUploadAuth();
    const ext = sanitizeExt(originalName);
    const fileName = `${Date.now()}-${crypto.randomBytes(4).toString("hex")}${ext}`;
    const folder =
      kind === "startup"
        ? "/hotel-central-admin-panel/startup"
        : `/hotel-central-admin-panel/backgrounds/${bucket}`;
    res.json({
      ok: true,
      publicKey: IMAGEKIT_PUBLIC_KEY,
      uploadUrl: "https://upload.imagekit.io/api/v1/files/upload",
      fileName,
      folder,
      kind,
      bucket,
      ...auth
    });
  })
);

app.post(
  "/api/admin/assets/register",
  rateLimitBy("asset-register", { windowMs: 60 * 1000, max: 25 }, (req) => `${clientIp(req)}:${req.currentUser?.id || "unknown"}`),
  requireModuleAction("content", "create", async (req, res) => {
    ensureImageKitConfigured();
    const store = await getStore();
    const kind = `${req.body.kind || "background"}`.trim().toLowerCase();
    const bucket = `${req.body.bucket || "home"}`.trim().toLowerCase();
    const originalUrl = `${req.body.originalUrl || ""}`.trim();
    const mimeType = `${req.body.mimeType || ""}`.trim().toLowerCase();
    if (!originalUrl) {
      return res.status(400).json({ error: "originalUrl is required" });
    }
    if (!["startup", "background"].includes(kind)) {
      return res.status(400).json({ error: "Invalid asset kind" });
    }
    if (!assertImageKitUrl(originalUrl)) {
      return res.status(400).json({ error: "Only HTTPS ImageKit asset URLs are allowed" });
    }
    const deliveryUrl = buildImageKitDeliveryUrl(originalUrl, { kind, mimeType });
    if (kind === "startup") {
      store.hotel.startupLogoUrl = deliveryUrl;
    } else {
      const target = Array.isArray(store.backgrounds[bucket]) ? store.backgrounds[bucket] : [];
      store.backgrounds[bucket] = [deliveryUrl, ...target.filter((url) => url !== deliveryUrl)].slice(0, 8);
    }
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Uploaded ${kind === "startup" ? "startup asset" : `${bucket} background asset`} via ImageKit`,
      entityType: "ASSET",
      entityId: bucket,
      tone: "success"
    });
    await saveStore(store);
    res.json({ ok: true, url: deliveryUrl, store });
  })
);

app.post(
  "/api/admin/upload",
  rateLimitBy("legacy-upload", { windowMs: 60 * 1000, max: 10 }, (req) => `${clientIp(req)}:${req.currentUser?.id || "unknown"}`),
  requireModuleAction("content", "create", async (req, res) => {
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

app.delete(
  "/api/admin/assets/:assetId",
  requireModuleAction("content", "delete", async (req, res) => {
    const store = await getStore();
    const asset = buildAssetEntries(store).find((entry) => entry.id === req.params.assetId);
    if (!asset) {
      return res.status(404).json({ error: "Asset not found" });
    }
    if (asset.kind === "startup") {
      store.hotel.startupLogoUrl = null;
    } else {
      store.backgrounds[asset.bucket] = (store.backgrounds[asset.bucket] || []).filter(
        (url) => (asset.storedUrl ? url !== asset.storedUrl : buildImageKitDeliveryUrl(url, { kind: "background" }) !== asset.url)
      );
    }
    bestEffortDeleteLocalAsset(asset.storedUrl || asset.url);
    pushAudit(store, {
      actorName: req.currentUser.name,
      actorRole: req.currentUser.role,
      action: `Deleted ${asset.kind === "startup" ? "startup" : `${asset.bucket} background`} asset`,
      entityType: "ASSET",
      entityId: asset.id,
      tone: "warning"
    });
    await saveStore(store);
    res.json({ ok: true });
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
      `Hotel admin panel listening on http://localhost:${PORT} (db=${dbMode}, images=${imageMode})`
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
