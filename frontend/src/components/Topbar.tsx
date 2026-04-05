"use client";

import React, { useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { motion } from "motion/react";

export function Topbar({
    title,
    syncLabel,
    onLogout,
    onToggleNotifications,
}: {
    title: string;
    syncLabel?: string;
    onLogout?: () => void;
    onToggleNotifications?: () => void;
}) {
    const router = useRouter();
    const pathname = usePathname();
    const searchParams = useSearchParams();
    const [searchValue, setSearchValue] = useState(searchParams?.get("room") || "");

    useEffect(() => {
        setSearchValue(searchParams?.get("room") || "");
    }, [searchParams]);

    const updateRoomSearch = (value: string) => {
        setSearchValue(value);
        const params = new URLSearchParams(searchParams?.toString() || "");
        if (value.trim()) {
            params.set("room", value.trim());
        } else {
            params.delete("room");
        }
        const query = params.toString();
        const safePathname = pathname || "/dashboard";
        router.replace(query ? `${safePathname}?${query}` : safePathname);
    };

    return (
        <motion.header
            className="h-20 topbar-glass flex items-center justify-between px-8 sticky top-0 z-10"
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
        >
            <div>
                <div className="bento-card-eyebrow">Current Page</div>
                <motion.h1
                    key={title}
                    className="text-2xl font-bold tracking-tight"
                    style={{ color: "#292620" }}
                    initial={{ opacity: 0, x: -8 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
                >
                    {title}
                </motion.h1>
            </div>

            <div className="flex items-center gap-6">
                {/* Search */}
                <div className="relative">
                    <input
                        type="text"
                        placeholder="Search room number..."
                        className="field-input !rounded-full !py-2 pl-10 pr-4 w-60 shadow-sm"
                        value={searchValue}
                        onChange={(event) => updateRoomSearch(event.target.value)}
                    />
                    <svg
                        className="w-4 h-4 absolute left-4 top-1/2 -translate-y-1/2 opacity-40"
                        xmlns="http://www.w3.org/2000/svg"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                    >
                        <circle cx="11" cy="11" r="8" />
                        <path d="m21 21-4.3-4.3" />
                    </svg>
                </div>

                {/* Live sync */}
                <div className="flex items-center gap-2 text-xs font-semibold" style={{ color: "rgba(62,59,51,0.6)" }}>
                    <span className="live-dot" />
                    {syncLabel || "Live Sync Active"}
                </div>

                {/* Actions */}
                <div className="flex items-center gap-3 border-l pl-6" style={{ borderColor: "rgba(229,225,216,0.7)" }}>
                    <motion.button
                        whileHover={{ scale: 1.08 }}
                        whileTap={{ scale: 0.92 }}
                        className="w-10 h-10 rounded-full bg-white border flex items-center justify-center"
                        style={{ borderColor: "#E5E1D8", boxShadow: "0 1px 4px rgb(0 0 0 / 0.06)" }}
                        onClick={onToggleNotifications}
                    >
                        <svg className="w-5 h-5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5" />
                            <path d="M10 21a2 2 0 0 0 4 0" />
                        </svg>
                    </motion.button>

                    {onLogout && (
                        <motion.button
                            whileHover={{ scale: 1.04, y: -1 }}
                            whileTap={{ scale: 0.94 }}
                            onClick={onLogout}
                            className="pill-button pill-button-secondary !px-4 !py-2 !text-xs"
                        >
                            Logout
                        </motion.button>
                    )}
                </div>
            </div>
        </motion.header>
    );
}
