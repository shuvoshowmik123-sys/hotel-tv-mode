"use client";

import React from "react";
import { motion } from "motion/react";

export interface BentoCardProps {
    title?: string;
    eyebrow?: string;
    actions?: React.ReactNode;
    children: React.ReactNode;
    className?: string;
    delay?: number;
}

export function BentoCard({ title, eyebrow, actions, children, className = "", delay = 0 }: BentoCardProps) {
    return (
        <motion.section
            className={`bento-card ${className}`}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{
                duration: 0.5,
                delay,
                ease: [0.22, 1, 0.36, 1],
            }}
            whileHover={{
                y: -3,
                transition: { duration: 0.25, ease: [0.34, 1.56, 0.64, 1] },
            }}
        >
            {(title || eyebrow || actions) && (
                <div className="bento-card-header">
                    <div>
                        {eyebrow && <div className="bento-card-eyebrow">{eyebrow}</div>}
                        {title && <h3 className="bento-card-title">{title}</h3>}
                    </div>
                    {actions && <div>{actions}</div>}
                </div>
            )}
            <div>{children}</div>
        </motion.section>
    );
}
