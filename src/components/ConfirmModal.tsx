"use client";

import React from "react";
import { motion, AnimatePresence } from "motion/react";
import { BentoCard } from "./BentoCard";
import { PillButton } from "./UIElements";

export function ConfirmModal({
    open,
    title,
    description,
    confirmLabel,
    onCancel,
    onConfirm,
}: {
    open: boolean;
    title: string;
    description: string;
    confirmLabel: string;
    onCancel: () => void;
    onConfirm: () => void;
}) {
    return (
        <AnimatePresence>
            {open && (
                <>
                    <motion.div
                        className="fixed inset-0 z-40 bg-black/15"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onCancel}
                    />
                    <motion.div
                        className="fixed inset-0 z-50 flex items-center justify-center p-4"
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: 10 }}
                        transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                    >
                        <div className="w-full max-w-md">
                            <BentoCard title={title} eyebrow="Confirmation Required">
                                <p className="mt-4 text-sm leading-6 text-luxury-800/70">{description}</p>
                                <div className="mt-6 flex justify-end gap-3">
                                    <PillButton type="button" onClick={onCancel}>
                                        Cancel
                                    </PillButton>
                                    <PillButton primary type="button" onClick={onConfirm}>
                                        {confirmLabel}
                                    </PillButton>
                                </div>
                            </BentoCard>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
}
