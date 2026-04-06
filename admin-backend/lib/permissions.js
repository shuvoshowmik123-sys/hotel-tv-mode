const ACTIONS = ["view", "create", "edit", "delete", "manage"];

const MODULES = [
  {
    key: "dashboard",
    label: "Dashboard",
    path: "/dashboard",
    description: "View the property-wide operations overview.",
    actionLabels: {
      view: "View dashboard"
    }
  },
  {
    key: "binding",
    label: "Device Binding",
    path: "/binding",
    description: "Assign activation codes and TVs to rooms.",
    actionLabels: {
      view: "View binding queue",
      create: "Start binding",
      edit: "Update binding details",
      delete: "Remove binding",
      manage: "Bind and unbind TVs"
    }
  },
  {
    key: "rooms",
    label: "Room Management",
    path: "/rooms",
    description: "Manage room inventory, profiles, and overrides.",
    actionLabels: {
      view: "View rooms",
      create: "Create rooms",
      edit: "Edit room details",
      delete: "Archive or delete rooms",
      manage: "Manage room overrides"
    }
  },
  {
    key: "sessions",
    label: "Guest Sessions",
    path: "/sessions",
    description: "Start, update, and end guest stays.",
    actionLabels: {
      view: "View sessions",
      create: "Start sessions",
      edit: "Edit sessions",
      delete: "End sessions",
      manage: "Check in and check out guests"
    }
  },
  {
    key: "content",
    label: "Launcher Content",
    path: "/content",
    description: "Manage branding, assets, and guest-facing copy.",
    actionLabels: {
      view: "View content",
      create: "Upload assets",
      edit: "Edit content",
      delete: "Delete assets",
      manage: "Publish changes to TVs"
    }
  },
  {
    key: "menus",
    label: "Menu Management",
    path: "/menus",
    description: "Control food and beverage menu items.",
    actionLabels: {
      view: "View menus",
      create: "Create menu items",
      edit: "Edit menu items",
      delete: "Archive menu items",
      manage: "Publish menu changes"
    }
  },
  {
    key: "policies",
    label: "Apps & Inputs",
    path: "/policies",
    description: "Control app visibility and TV input visibility.",
    actionLabels: {
      view: "View policies",
      create: "Create policy rules",
      edit: "Edit policy rules",
      delete: "Delete policy rules",
      manage: "Publish policy changes"
    }
  },
  {
    key: "users",
    label: "User Management",
    path: "/users",
    description: "Manage accounts, roles, and staff access.",
    actionLabels: {
      view: "View users",
      create: "Create users",
      edit: "Edit users",
      delete: "Archive users",
      manage: "Manage roles and permissions"
    }
  },
  {
    key: "audit",
    label: "Audit Log",
    path: "/audit",
    description: "Review hotel administration activity.",
    actionLabels: {
      view: "View audit log"
    }
  },
  {
    key: "settings",
    label: "Settings",
    path: "/settings",
    description: "Manage property settings and integrations.",
    actionLabels: {
      view: "View settings",
      edit: "Edit settings",
      manage: "Administer integrations"
    }
  }
];

const ROLE_PERMISSIONS = {
  SUPER_ADMIN: {
    dashboard: ["view"],
    binding: ["view", "create", "edit", "manage"],
    rooms: ["view", "create", "edit", "delete", "manage"],
    sessions: ["view", "create", "edit", "delete", "manage"],
    content: ["view", "create", "edit", "delete", "manage"],
    menus: ["view", "create", "edit", "delete", "manage"],
    policies: ["view", "edit", "manage"],
    users: ["view", "create", "edit", "delete", "manage"],
    audit: ["view"],
    settings: ["view", "edit", "manage"]
  },
  ADMIN: {
    dashboard: ["view"],
    binding: ["view", "create", "edit", "manage"],
    rooms: ["view", "create", "edit", "delete", "manage"],
    sessions: ["view", "create", "edit", "delete", "manage"],
    content: ["view", "create", "edit", "delete", "manage"],
    menus: ["view", "create", "edit", "delete", "manage"],
    policies: ["view", "edit", "manage"]
  },
  RECEPTIONIST: {
    dashboard: ["view"],
    binding: ["view", "create", "edit", "manage"],
    rooms: ["view"],
    sessions: ["view", "create", "edit", "delete", "manage"]
  }
};

function getRolePermissions(role) {
  return ROLE_PERMISSIONS[role] || {};
}

function canModuleAction(role, moduleKey, action) {
  return Boolean(getRolePermissions(role)[moduleKey]?.includes(action));
}

function getAllowedRoutes(role) {
  return MODULES.filter((module) => canModuleAction(role, module.key, "view")).map((module) => module.path);
}

function getPermissionEntries(role) {
  return MODULES.map((module) => {
    const allowedActions = ACTIONS.filter((action) => canModuleAction(role, module.key, action));
    return {
      ...module,
      visible: allowedActions.includes("view"),
      allowedActions
    };
  });
}

module.exports = {
  ACTIONS,
  MODULES,
  ROLE_PERMISSIONS,
  getRolePermissions,
  canModuleAction,
  getAllowedRoutes,
  getPermissionEntries
};
