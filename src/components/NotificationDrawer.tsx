"use client";

import React from "react";
import { motion, AnimatePresence } from "motion/react";
import { BentoCard } from "./BentoCard";

type NotificationEntry = {
    id: string;
    label?: string;
    message?: string;
    tone?: string;
    createdAt?: string;
};

function toneClasses(tone?: string) {
    if (tone === "success") return "bg-green-50 text-green-700 border-green-200";
    if (tone === "warning") return "bg-amber-50 text-amber-700 border-amber-200";
    return "bg-white text-luxury-800 border-luxury-200";
}

export function NotificationDrawer({
    open,
    notifications,
    onClose,
}: {
    open: boolean;
    notifications: NotificationEntry[];
    onClose: () => void;
}) {
    return (
        <AnimatePresence>
            {open && (
                <>
                    <motion.div
                        className="fixed inset-0 z-30 bg-black/10"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                    />
                    <motion.aside
                        className="fixed right-6 top-24 z-40 w-[380px] max-w-[calc(100vw-2rem)]"
                        initial={{ opacity: 0, x: 24 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: 24 }}
                        transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                    >
                        <BentoCard
                            title="Notifications"
                            eyebrow="Recent System Events"
                            actions={
                                <button
                                    className="pill-button pill-button-secondary !px-4 !py-2 !text-xs"
                                    onClick={onClose}
                                >
                                    Close
                                </button>
                            }
                        >
                            <div className="mt-4 max-h-[60vh] overflow-y-auto space-y-3 pr-1">
                                {notifications.length === 0 ? (
                                    <div className="text-sm text-luxury-800/50 py-6 text-center">
                                        No recent notifications.
                                    </div>
                                ) : (
                                    notifications.map((entry) => (
                                        <div
                                            key={entry.id}
                                            className="rounded-2xl border p-4"
                                            style={{ borderColor: "rgba(229,225,216,0.7)" }}
                                        >
                                            <div className="flex items-center justify-between gap-3">
                                                <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider border ${toneClasses(entry.tone)}`}>
                                                    {entry.label || "System"}
                                                </span>
                                                <span className="font-mono text-[11px] text-luxury-800/40">
                                                    {entry.createdAt ? new Date(entry.createdAt).toLocaleString() : ""}
                                                </span>
                                            </div>
                                            <div className="mt-3 text-sm font-medium text-luxury-900">
                                                {entry.message || "Notification"}
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </BentoCard>
                    </motion.aside>
                </>
            )}
        </AnimatePresence>
    );
}
