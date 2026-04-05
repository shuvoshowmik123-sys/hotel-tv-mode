"use client";

import React from "react";
import { motion } from "motion/react";

export function PillButton({
    children,
    primary = false,
    className = "",
    ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { primary?: boolean }) {
    const variantClass = primary ? "pill-button-primary" : "pill-button-secondary";
    return (
        <motion.button
            className={`pill-button ${variantClass} ${className}`}
            whileTap={{ scale: 0.93 }}
            whileHover={{ y: -1 }}
            transition={{ type: "spring", stiffness: 400, damping: 17 }}
            {...(props as any)}
        >
            {children}
        </motion.button>
    );
}

export function StatusPill({ status, label }: { status: "occupied" | "vacant" | "warning" | "default", label: string }) {
    const pillClasses: Record<string, string> = {
        occupied: "status-pill-occupied",
        vacant: "status-pill-vacant",
        warning: "status-pill-warning",
        default: "status-pill-vacant"
    };

    return (
        <motion.span
            className={`status-pill ${pillClasses[status] || pillClasses.default}`}
            initial={{ opacity: 0, scale: 0.85 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ type: "spring", stiffness: 300, damping: 20 }}
        >
            {label}
        </motion.span>
    );
}
