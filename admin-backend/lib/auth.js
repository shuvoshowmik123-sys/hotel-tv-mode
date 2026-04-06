const crypto = require("crypto");
const { getAllowedRoutes, getPermissionEntries } = require("./permissions");

const ROLES = {
  SUPER_ADMIN: "SUPER_ADMIN",
  ADMIN: "ADMIN",
  RECEPTIONIST: "RECEPTIONIST"
};

function roleLabel(role) {
  return `${role || ""}`.replaceAll("_", " ").trim();
}

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString("hex");
  const hash = crypto.scryptSync(password, salt, 64).toString("hex");
  return `${salt}:${hash}`;
}

function verifyPassword(password, encoded) {
  if (!encoded || !encoded.includes(":")) {
    return false;
  }
  const [salt, storedHash] = encoded.split(":");
  const computedHash = crypto.scryptSync(password, salt, 64).toString("hex");
  const a = Buffer.from(storedHash, "hex");
  const b = Buffer.from(computedHash, "hex");
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

function safeUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    role: user.role,
    roleLabel: roleLabel(user.role),
    status: user.status,
    propertyId: user.propertyId,
    createdAt: user.createdAt,
    allowedRoutes: getAllowedRoutes(user.role),
    permissions: getPermissionEntries(user.role)
  };
}

function firstAdminConfig() {
  const name = `${process.env.FIRST_ADMIN_NAME || ""}`.trim();
  const email = `${process.env.FIRST_ADMIN_EMAIL || ""}`.trim().toLowerCase();
  const password = `${process.env.FIRST_ADMIN_PASSWORD || ""}`.trim();
  const propertyId = `${process.env.FIRST_ADMIN_PROPERTY_ID || process.env.PROPERTY_ID || "primary"}`.trim();
  return {
    name,
    email,
    password,
    propertyId
  };
}

function hasFirstAdminConfig() {
  const config = firstAdminConfig();
  return Boolean(config.name && config.email && config.password);
}

function seededUsers(propertyId) {
  if (!hasFirstAdminConfig()) {
    return [];
  }
  const config = firstAdminConfig();
  return [
    {
      id: "user-super-admin",
      name: config.name,
      email: config.email,
      role: ROLES.SUPER_ADMIN,
      status: "ACTIVE",
      propertyId: config.propertyId || propertyId,
      password: config.password
    }
  ];
}

module.exports = {
  ROLES,
  roleLabel,
  hashPassword,
  verifyPassword,
  safeUser,
  seededUsers,
  firstAdminConfig,
  hasFirstAdminConfig
};
