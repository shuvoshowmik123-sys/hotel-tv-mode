"use client";

import React, { createContext, useCallback, useContext, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "motion/react";

type FeedbackTone = "success" | "error" | "warning" | "info";

type FeedbackItem = {
    id: number;
    title?: string;
    message: string;
    tone: FeedbackTone;
};

type FeedbackContextValue = {
    notify: (input: { title?: string; message: string; tone?: FeedbackTone }) => void;
    dismiss: (id: number) => void;
};

const FeedbackContext = createContext<FeedbackContextValue | null>(null);

const toneStyles: Record<FeedbackTone, { shell: string; badge: string; label: string }> = {
    success: {
        shell: "border-emerald-200 bg-emerald-50/95 text-emerald-900",
        badge: "bg-emerald-600/10 text-emerald-700",
        label: "Saved",
    },
    error: {
        shell: "border-red-200 bg-red-50/95 text-red-900",
        badge: "bg-red-600/10 text-red-700",
        label: "Error",
    },
    warning: {
        shell: "border-amber-200 bg-amber-50/95 text-amber-900",
        badge: "bg-amber-600/10 text-amber-700",
        label: "Attention",
    },
    info: {
        shell: "border-luxury-200 bg-white/95 text-luxury-900",
        badge: "bg-gold-500/10 text-gold-700",
        label: "Update",
    },
};

export function FeedbackProvider({ children }: { children: React.ReactNode }) {
    const [items, setItems] = useState<FeedbackItem[]>([]);
    const nextId = useRef(1);

    const dismiss = useCallback((id: number) => {
        setItems((current) => current.filter((item) => item.id !== id));
    }, []);

    const notify = useCallback(({ title, message, tone = "info" }: { title?: string; message: string; tone?: FeedbackTone }) => {
        const id = nextId.current++;
        setItems((current) => [...current, { id, title, message, tone }]);
        window.setTimeout(() => dismiss(id), tone === "error" ? 6500 : 4200);
    }, [dismiss]);

    const value = useMemo(() => ({ notify, dismiss }), [notify, dismiss]);

    return (
        <FeedbackContext.Provider value={value}>
            {children}
            <div className="pointer-events-none fixed right-5 top-5 z-[90] flex w-full max-w-sm flex-col gap-3">
                <AnimatePresence initial={false}>
                    {items.map((item) => {
                        const tone = toneStyles[item.tone];
                        return (
                            <motion.div
                                key={item.id}
                                initial={{ opacity: 0, y: -14, scale: 0.98 }}
                                animate={{ opacity: 1, y: 0, scale: 1 }}
                                exit={{ opacity: 0, y: -12, scale: 0.98 }}
                                transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
                                className={`pointer-events-auto overflow-hidden rounded-[22px] border px-4 py-3 shadow-[0_24px_60px_-38px_rgba(0,0,0,0.35)] backdrop-blur ${tone.shell}`}
                            >
                                <div className="flex items-start gap-3">
                                    <div className={`mt-0.5 inline-flex rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.18em] ${tone.badge}`}>
                                        {item.title || tone.label}
                                    </div>
                                    <div className="min-w-0 flex-1">
                                        <div className="text-sm font-semibold leading-5">{item.message}</div>
                                    </div>
                                    <button
                                        type="button"
                                        onClick={() => dismiss(item.id)}
                                        className="rounded-full p-1 text-luxury-800/45 transition hover:bg-black/5 hover:text-luxury-900"
                                        aria-label="Dismiss notification"
                                    >
                                        <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M18 6 6 18" />
                                            <path d="m6 6 12 12" />
                                        </svg>
                                    </button>
                                </div>
                            </motion.div>
                        );
                    })}
                </AnimatePresence>
            </div>
        </FeedbackContext.Provider>
    );
}

export function useFeedback() {
    const context = useContext(FeedbackContext);
    if (!context) {
        throw new Error("useFeedback must be used within FeedbackProvider");
    }
    return context;
}
