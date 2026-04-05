export type UserRole = "SUPER_ADMIN" | "ADMIN" | "RECEPTIONIST";

export const ROLE_ROUTES: Record<UserRole, string[]> = {
    SUPER_ADMIN: ["/dashboard", "/binding", "/rooms", "/sessions", "/content", "/menus", "/policies", "/users", "/audit", "/settings"],
    ADMIN: ["/dashboard", "/rooms", "/sessions", "/content", "/menus", "/policies"],
    RECEPTIONIST: ["/dashboard", "/binding", "/rooms", "/sessions"],
};

export function getAllowedRoutes(role?: string) {
    if (!role || !(role in ROLE_ROUTES)) {
        return [];
    }
    return ROLE_ROUTES[role as UserRole];
}

export function canAccessRoute(role: string | undefined, pathname: string) {
    const allowedRoutes = getAllowedRoutes(role);
    return allowedRoutes.some((route) => pathname === route || pathname.startsWith(`${route}/`));
}

export function canManageRoomOverrides(role?: string) {
    return role === "SUPER_ADMIN" || role === "ADMIN";
}

export function isRoomManagementReadOnly(role?: string) {
    return role === "RECEPTIONIST";
}
