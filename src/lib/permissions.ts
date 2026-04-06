export type UserRole = "SUPER_ADMIN" | "ADMIN" | "RECEPTIONIST";
export type PermissionAction = "view" | "create" | "edit" | "delete" | "manage";
export type ModuleKey =
    | "dashboard"
    | "binding"
    | "rooms"
    | "sessions"
    | "content"
    | "menus"
    | "policies"
    | "users"
    | "audit"
    | "settings";

type ModuleDefinition = {
    key: ModuleKey;
    label: string;
    path: string;
    description: string;
    actionLabels: Partial<Record<PermissionAction, string>>;
};

const ACTIONS: PermissionAction[] = ["view", "create", "edit", "delete", "manage"];

export const MODULES: ModuleDefinition[] = [
    {
        key: "dashboard",
        label: "Dashboard",
        path: "/dashboard",
        description: "View the property-wide operations overview.",
        actionLabels: { view: "View dashboard" },
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
            manage: "Bind and unbind TVs",
        },
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
            manage: "Manage room overrides",
        },
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
            manage: "Check in and check out guests",
        },
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
            manage: "Publish changes to TVs",
        },
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
            manage: "Publish menu changes",
        },
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
            manage: "Publish policy changes",
        },
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
            manage: "Manage roles and permissions",
        },
    },
    {
        key: "audit",
        label: "Audit Log",
        path: "/audit",
        description: "Review hotel administration activity.",
        actionLabels: { view: "View audit log" },
    },
    {
        key: "settings",
        label: "Settings",
        path: "/settings",
        description: "Manage property settings and integrations.",
        actionLabels: {
            view: "View settings",
            edit: "Edit settings",
            manage: "Administer integrations",
        },
    },
];

export const ROLE_PERMISSIONS: Record<UserRole, Partial<Record<ModuleKey, PermissionAction[]>>> = {
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
        settings: ["view", "edit", "manage"],
    },
    ADMIN: {
        dashboard: ["view"],
        binding: ["view", "create", "edit", "manage"],
        rooms: ["view", "create", "edit", "delete", "manage"],
        sessions: ["view", "create", "edit", "delete", "manage"],
        content: ["view", "create", "edit", "delete", "manage"],
        menus: ["view", "create", "edit", "delete", "manage"],
        policies: ["view", "edit", "manage"],
    },
    RECEPTIONIST: {
        dashboard: ["view"],
        binding: ["view", "create", "edit", "manage"],
        rooms: ["view"],
        sessions: ["view", "create", "edit", "delete", "manage"],
    },
};

export function getRolePermissions(role?: string) {
    return ROLE_PERMISSIONS[role as UserRole] || {};
}

export function canModuleAction(role: string | undefined, moduleKey: ModuleKey, action: PermissionAction) {
    return Boolean(getRolePermissions(role)[moduleKey]?.includes(action));
}

export function getAllowedRoutes(role?: string) {
    return MODULES.filter((module) => canModuleAction(role, module.key, "view")).map((module) => module.path);
}

export function canAccessRoute(role: string | undefined, pathname?: string | null) {
    if (!pathname) {
        return false;
    }
    const allowedRoutes = getAllowedRoutes(role);
    return allowedRoutes.some((route) => pathname === route || pathname.startsWith(`${route}/`));
}

export function getPermissionEntries(role?: string) {
    return MODULES.map((module) => {
        const allowedActions = ACTIONS.filter((action) => canModuleAction(role, module.key, action));
        return {
            ...module,
            visible: allowedActions.includes("view"),
            allowedActions,
        };
    });
}

export function permissionLabel(moduleKey: ModuleKey, action: PermissionAction) {
    const module = MODULES.find((item) => item.key === moduleKey);
    return module?.actionLabels[action] || `${action[0].toUpperCase()}${action.slice(1)} ${module?.label || ""}`.trim();
}

export function canManageRoomOverrides(role?: string) {
    return canModuleAction(role, "rooms", "manage");
}

export function isRoomManagementReadOnly(role?: string) {
    return !canModuleAction(role, "rooms", "edit");
}
