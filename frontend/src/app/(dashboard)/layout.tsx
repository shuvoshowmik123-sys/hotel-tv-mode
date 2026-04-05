"use client";

import React, { useEffect, useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import { motion, AnimatePresence } from "motion/react";
import { Sidebar } from "../../components/Sidebar";
import { Topbar } from "../../components/Topbar";
import { NotificationDrawer } from "../../components/NotificationDrawer";
import { api } from "../../lib/api";
import { canAccessRoute } from "../../lib/permissions";

function relativeSyncLabel(value?: string) {
    if (!value) return "Live Sync Active";
    const diffMinutes = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
    if (diffMinutes < 60) return `Last synced ${diffMinutes}m ago`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `Last synced ${diffHours}h ago`;
    return `Last synced ${Math.floor(diffHours / 24)}d ago`;
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<any>(null);
    const [panelMeta, setPanelMeta] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [notificationsOpen, setNotificationsOpen] = useState(false);
    const router = useRouter();
    const pathname = usePathname();

    async function fetchPanelMeta() {
        try {
            const state = await api("/api/admin/state");
            setPanelMeta({
                notifications: state.notifications || [],
                systemHealth: state.systemHealth || null,
            });
        } catch (error) {
            console.error(error);
        }
    }

    useEffect(() => {
        async function fetchUser() {
            try {
                const data = await api("/api/auth/me");
                if (data.user) {
                    setUser(data.user);
                    await fetchPanelMeta();
                } else router.push("/login");
            } catch {
                router.push("/login");
            } finally {
                setLoading(false);
            }
        }
        fetchUser();
    }, [router]);

    useEffect(() => {
        if (!user) return;
        if (!canAccessRoute(user.role, pathname)) {
            router.replace("/dashboard");
            return;
        }
        fetchPanelMeta();
        const interval = window.setInterval(() => {
            fetchPanelMeta();
        }, 60000);
        return () => window.clearInterval(interval);
    }, [user, pathname, router]);

    const handleLogout = async () => {
        await api("/api/auth/logout", { method: "POST" });
        router.push("/login");
    };

    if (loading) {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center gap-6"
                style={{ background: "#FAFAF8" }}>
                {/* Animated logo skeleton */}
                <motion.div
                    animate={{ scale: [1, 1.05, 1], opacity: [0.7, 1, 0.7] }}
                    transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                    className="w-16 h-16 rounded-2xl flex items-center justify-center font-bold text-2xl text-white"
                    style={{ background: "linear-gradient(135deg, #E6C56E, #C9A84C, #AB8B39)" }}
                >
                    AG
                </motion.div>
                <div className="text-sm font-semibold" style={{ color: "rgba(62,59,51,0.5)" }}>
                    Loading workspace...
                </div>
            </div>
        );
    }

    if (!user) return null;
    if (!canAccessRoute(user.role, pathname)) return null;

    const titles: Record<string, string> = {
        "/dashboard": "Dashboard Overview",
        "/binding": "Device Binding",
        "/rooms": "Room Management",
        "/sessions": "Guest Sessions",
        "/content": "Launcher Content",
        "/menus": "Menu Management",
        "/policies": "Apps & Inputs Policy",
        "/users": "User Management",
        "/audit": "Audit Log",
        "/settings": "Settings",
    };

    const currentTitle = titles[pathname] || "Dashboard Overview";

    return (
        <div className="flex min-h-screen" style={{ background: "#FAFAF8" }}>
            <Sidebar user={user} />
            <div className="flex-1 ml-64 flex flex-col min-h-screen" style={{ maxWidth: "calc(100vw - 16rem)" }}>
                <Topbar
                    title={currentTitle}
                    syncLabel={relativeSyncLabel(panelMeta?.systemHealth?.lastPushTime)}
                    onLogout={handleLogout}
                    onToggleNotifications={() => {
                        fetchPanelMeta();
                        setNotificationsOpen((value) => !value);
                    }}
                />
                <main className="flex-1 p-8 overflow-x-hidden">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={pathname}
                            initial={{ opacity: 0, y: 12 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -8 }}
                            transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }}
                        >
                            {children}
                        </motion.div>
                    </AnimatePresence>
                </main>
                <NotificationDrawer
                    open={notificationsOpen}
                    notifications={panelMeta?.notifications || []}
                    onClose={() => setNotificationsOpen(false)}
                />
            </div>
        </div>
    );
}
