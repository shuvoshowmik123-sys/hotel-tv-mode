const crypto = require("crypto");

const ROLES = {
  SUPER_ADMIN: "SUPER_ADMIN",
  ADMIN: "ADMIN",
  RECEPTIONIST: "RECEPTIONIST"
};

const DEFAULT_PASSWORD = "Asteria@2026!";

function roleLabel(role) {
  return role.replaceAll("_", " ");
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
    createdAt: user.createdAt
  };
}

function seededUsers(propertyId) {
  return [
    {
      id: "user-super-admin",
      name: "Asteria Super Admin",
      email: "superadmin@asteriagrand.local",
      role: ROLES.SUPER_ADMIN,
      status: "ACTIVE",
      propertyId,
      password: DEFAULT_PASSWORD
    },
    {
      id: "user-admin",
      name: "Asteria Property Admin",
      email: "admin@asteriagrand.local",
      role: ROLES.ADMIN,
      status: "ACTIVE",
      propertyId,
      password: DEFAULT_PASSWORD
    },
    {
      id: "user-reception",
      name: "Asteria Reception",
      email: "reception@asteriagrand.local",
      role: ROLES.RECEPTIONIST,
      status: "ACTIVE",
      propertyId,
      password: DEFAULT_PASSWORD
    }
  ];
}

module.exports = {
  ROLES,
  DEFAULT_PASSWORD,
  roleLabel,
  hashPassword,
  verifyPassword,
  safeUser,
  seededUsers
};
