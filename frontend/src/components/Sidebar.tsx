"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "motion/react";
import { Icon } from "./Icons";
import { getAllowedRoutes } from "../lib/permissions";

const NAV_GROUPS = [
    {
        label: "Operations",
        routes: [
            { id: "dashboard", label: "Dashboard", path: "/dashboard" },
            { id: "binding", label: "Device Binding", path: "/binding" },
            { id: "rooms", label: "Room Management", path: "/rooms" },
            { id: "sessions", label: "Guest Sessions", path: "/sessions" },
        ],
    },
    {
        label: "Content",
        routes: [
            { id: "content", label: "Launcher Content", path: "/content" },
            { id: "menus", label: "Menu Management", path: "/menus" },
            { id: "policies", label: "Apps & Inputs", path: "/policies" },
        ],
    },
    {
        label: "Admin",
        routes: [
            { id: "users", label: "User Management", path: "/users" },
            { id: "audit", label: "Audit Log", path: "/audit" },
            { id: "settings", label: "Settings", path: "/settings" },
        ],
    },
];

export function Sidebar({ user }: { user: any }) {
    const pathname = usePathname();
    const allowedRoutes = new Set(getAllowedRoutes(user?.role));

    const allowedGroups = NAV_GROUPS.map((g) => ({
        ...g,
        routes: g.routes.filter((r) => allowedRoutes.has(r.path)),
    })).filter((g) => g.routes.length > 0);

    return (
        <motion.aside
            className="w-64 bg-white h-screen fixed left-0 top-0 flex flex-col py-6 px-4 shrink-0 overflow-y-auto z-20"
            style={{
                borderRight: "1px solid rgba(229,225,216,0.6)",
                boxShadow: "4px 0 24px rgb(0 0 0 / 0.03)",
            }}
            initial={{ x: -32, opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
        >
            {/* Brand */}
            <motion.div
                className="flex items-center gap-3 mb-8 px-2"
                initial={{ opacity: 0, y: -8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.15, duration: 0.4 }}
            >
                <motion.div
                    className="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-lg text-white"
                    style={{ background: "linear-gradient(135deg, #E6C56E 0%, #C9A84C 60%, #AB8B39 100%)", boxShadow: "0 4px 12px rgb(201 168 76 / 0.4)" }}
                    animate={{ y: [0, -3, 0] }}
                    transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                >
                    AG
                </motion.div>
                <div>
                    <h2 className="font-bold leading-tight" style={{ color: "#292620", fontSize: 15 }}>
                        Asteria Grand
                    </h2>
                    <div style={{ fontSize: 11, color: "rgba(62,59,51,0.5)", fontWeight: 600, letterSpacing: "0.04em" }}>
                        Central Admin Panel
                    </div>
                </div>
            </motion.div>

            {/* Nav Groups */}
            <nav className="flex-1 space-y-6">
                {allowedGroups.map((group, gIdx) => (
                    <div key={group.label}>
                        <motion.div
                            className="bento-card-eyebrow mb-2 px-3"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: 0.2 + gIdx * 0.06 }}
                        >
                            {group.label}
                        </motion.div>
                        <div className="space-y-0.5">
                            {group.routes.map((r, rIdx) => {
                                const isActive = pathname.startsWith(r.path);
                                return (
                                    <motion.div
                                        key={r.id}
                                        initial={{ opacity: 0, x: -12 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        transition={{ delay: 0.25 + gIdx * 0.06 + rIdx * 0.04, duration: 0.3 }}
                                        style={{ position: "relative" }}
                                    >
                                        <Link
                                            href={r.path}
                                            className="flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors relative"
                                            style={{
                                                color: isActive ? "#C9A84C" : "rgba(62,59,51,0.75)",
                                                background: isActive ? "rgba(201,168,76,0.08)" : "transparent",
                                            }}
                                        >
                                            {isActive && (
                                                <motion.span
                                                    layoutId="sidebar-active"
                                                    className="absolute inset-0 rounded-xl"
                                                    style={{ background: "rgba(201,168,76,0.10)", border: "1px solid rgba(201,168,76,0.2)" }}
                                                    transition={{ type: "spring", stiffness: 380, damping: 30 }}
                                                />
                                            )}
                                            <Icon
                                                name={r.id}
                                                className={isActive ? "text-gold-500" : "opacity-40"}
                                            />
                                            <span style={{ position: "relative" }}>{r.label}</span>
                                        </Link>
                                    </motion.div>
                                );
                            })}
                        </div>
                    </div>
                ))}
            </nav>

            {/* Footer */}
            <motion.div
                className="pt-5 mt-5 px-2 flex items-center gap-3"
                style={{ borderTop: "1px solid rgba(229,225,216,0.7)" }}
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.6 }}
            >
                <div
                    className="w-9 h-9 rounded-full flex items-center justify-center font-bold text-sm"
                    style={{ background: "#F3F1ED", color: "#292620" }}
                >
                    {user?.name?.slice(0, 1) || "A"}
                </div>
                <div className="flex-1 min-w-0">
                    <div className="text-sm font-semibold truncate" style={{ color: "#292620" }}>
                        {user?.name || "Account"}
                    </div>
                    <div style={{ fontSize: 10, color: "rgba(62,59,51,0.5)", fontWeight: 700, textTransform: "uppercase", letterSpacing: "0.08em" }}>
                        {user?.role?.replace("_", " ")}
                    </div>
                </div>
            </motion.div>
        </motion.aside>
    );
}
