"use client";

import React, { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { BentoCard } from "../../../components/BentoCard";
import { useFeedback } from "../../../components/FeedbackProvider";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";
import { CLIMATE_CONTROL_OPTIONS, ROOM_CATEGORY_OPTIONS } from "../../../lib/roomOptions";

type BindingMode = "pending" | "manual" | null;
type BindingFormState = {
    activationCode: string;
    roomNumber: string;
    floor: string;
    roomCategory: string;
    climateControl: string;
};

const emptyForm: BindingFormState = {
    activationCode: "",
    roomNumber: "",
    floor: "",
    roomCategory: ROOM_CATEGORY_OPTIONS[0],
    climateControl: CLIMATE_CONTROL_OPTIONS[0],
};

export default function BindingPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [bindingMode, setBindingMode] = useState<BindingMode>(null);
    const [selectedPending, setSelectedPending] = useState<any>(null);
    const [form, setForm] = useState(emptyForm);
    const [isBinding, setIsBinding] = useState(false);
    const [formErrors, setFormErrors] = useState<Partial<Record<keyof BindingFormState, string>>>({});
    const { notify } = useFeedback();

    const load = async () => {
        setLoadError("");
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (err: any) {
            setLoadError(err.message || "Unable to load pending bindings.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const pendingItems = useMemo(
        () => (data?.pendingActivations ? Object.values(data.pendingActivations) : []),
        [data?.pendingActivations]
    );

    const roomCatalog = data?.roomCatalog || {
        roomCategories: [...ROOM_CATEGORY_OPTIONS],
        climateControls: [...CLIMATE_CONTROL_OPTIONS],
    };

    const activateManualBinding = () => {
        setBindingMode("manual");
        setSelectedPending(null);
        setForm({ ...emptyForm });
        setFormErrors({});
    };

    const activatePendingBinding = (item: any) => {
        setBindingMode("pending");
        setSelectedPending(item);
        setForm({
            ...emptyForm,
            activationCode: item.activationCode || "",
        });
        setFormErrors({});
    };

    const closeComposer = () => {
        setBindingMode(null);
        setSelectedPending(null);
        setForm({ ...emptyForm });
        setFormErrors({});
    };

    const validateForm = () => {
        const nextErrors: Partial<Record<keyof BindingFormState, string>> = {};
        if (!form.activationCode.trim()) nextErrors.activationCode = "Activation code is required.";
        if (!form.roomNumber.trim()) nextErrors.roomNumber = "Room number is required.";
        if (!form.floor.trim()) nextErrors.floor = "Floor is required.";
        setFormErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    const handleBind = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!validateForm()) {
            notify({ tone: "warning", message: "Please complete the room assignment fields before binding." });
            return;
        }
        setIsBinding(true);
        try {
            await api("/api/admin/bind", {
                method: "POST",
                body: JSON.stringify(form)
            });
            notify({ tone: "success", message: "Binding confirmed successfully." });
            closeComposer();
            await load();
        } catch (err: any) {
            notify({ tone: "error", message: err.message || "Failed to bind" });
        } finally {
            setIsBinding(false);
        }
    };

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="space-y-6">
            {loadError && <div className="page-inline-error">{loadError}</div>}

            <div className="grid grid-cols-1 xl:grid-cols-[1.2fr_0.8fr] gap-6">
                <BentoCard title="Pending Bindings" eyebrow="Step 1 - Choose a Device">
                    <div className="flex items-center justify-between gap-4 mt-4 mb-5">
                        <p className="text-sm text-luxury-800/60 max-w-2xl">
                            When a TV requests activation, it appears here. Click <span className="font-semibold text-luxury-900">Bind now</span> to move into the room assignment step.
                        </p>
                        <PillButton primary type="button" onClick={activateManualBinding}>
                            Bind manually
                        </PillButton>
                    </div>

                    {pendingItems.length === 0 ? (
                        <div className="rounded-2xl border-2 border-dashed border-luxury-200 bg-luxury-50/60 px-6 py-8 text-center">
                            <div className="text-sm font-semibold text-luxury-900">No pending TVs right now</div>
                            <div className="text-sm text-luxury-800/60 mt-2">
                                You can still open the assignment flow manually if staff already has an activation code from the TV.
                            </div>
                            <div className="pt-5">
                                <PillButton primary type="button" onClick={activateManualBinding}>
                                    Open manual binding
                                </PillButton>
                            </div>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {pendingItems.map((item: any) => {
                                const isActive = selectedPending?.pollToken === item.pollToken;
                                return (
                                    <motion.div
                                        key={item.pollToken || item.activationCode}
                                        layout
                                        className={`rounded-2xl border p-4 transition-all ${isActive ? "border-gold-400 bg-gold-50 shadow-[0_18px_45px_-28px_rgba(201,168,76,0.65)]" : "border-luxury-100 bg-luxury-50"}`}
                                    >
                                        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                                            <div>
                                                <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-luxury-800/45">Activation code</div>
                                                <div className="mt-1 font-mono text-lg text-gold-600 font-bold tracking-[0.2em]">{item.activationCode}</div>
                                                <div className="text-xs font-mono text-luxury-800/55 mt-2">{item.macAddress || item.deviceId || "Unknown TV device"}</div>
                                            </div>
                                            <div className="flex items-center gap-4">
                                                <span className="text-xs text-luxury-800/60 font-medium">
                                                    {item.createdAt ? new Date(item.createdAt).toLocaleString() : "Just now"}
                                                </span>
                                                <PillButton primary type="button" onClick={() => activatePendingBinding(item)}>
                                                    {isActive ? "Binding selected" : "Bind now"}
                                                </PillButton>
                                            </div>
                                        </div>
                                    </motion.div>
                                );
                            })}
                        </div>
                    )}
                </BentoCard>

                <AnimatePresence mode="wait">
                    {bindingMode ? (
                        <motion.div
                            key={bindingMode === "pending" ? selectedPending?.pollToken || "pending" : "manual"}
                            initial={{ opacity: 0, x: 26, scale: 0.98 }}
                            animate={{ opacity: 1, x: 0, scale: 1 }}
                            exit={{ opacity: 0, x: 20, scale: 0.98 }}
                            transition={{ duration: 0.34, ease: [0.22, 1, 0.36, 1] }}
                        >
                            <BentoCard title="Assign Room Profile" eyebrow="Step 2 - Confirm Binding">
                                <div className="mt-4 rounded-2xl bg-gradient-to-br from-gold-50 via-white to-luxury-50 border border-gold-100 p-4 shadow-[0_20px_50px_-36px_rgba(201,168,76,0.7)]">
                                    <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-luxury-800/45">Selected source</div>
                                    <div className="mt-2 text-sm font-semibold text-luxury-900">
                                        {bindingMode === "pending" ? "Pending activation detected from TV" : "Manual room assignment"}
                                    </div>
                                    <div className="mt-1 text-sm text-luxury-800/60">
                                        {bindingMode === "pending"
                                            ? "The activation code is locked from the selected pending device so staff can focus only on room assignment."
                                            : "Use manual binding when staff already has the activation code from the TV screen."}
                                    </div>
                                </div>

                                <form onSubmit={handleBind} className="space-y-4 mt-5">
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Activation Code</label>
                                        <input
                                            value={form.activationCode}
                                            readOnly={bindingMode === "pending"}
                                            onChange={(e) => {
                                                setForm((current) => ({ ...current, activationCode: e.target.value.toUpperCase() }));
                                                setFormErrors((current) => ({ ...current, activationCode: "" }));
                                            }}
                                            className={`w-full rounded-xl px-4 py-3 text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-gold-500/50 ${bindingMode === "pending" ? "bg-luxury-100 border border-luxury-200 text-luxury-700 cursor-not-allowed" : "bg-luxury-50 border border-luxury-200 focus:border-gold-500"}`}
                                            placeholder="e.g. AB12CD34EF"
                                            required
                                        />
                                        {formErrors.activationCode && <div className="field-error">{formErrors.activationCode}</div>}
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Number</label>
                                            <input
                                                value={form.roomNumber}
                                                onChange={(e) => {
                                                    setForm((current) => ({ ...current, roomNumber: e.target.value }));
                                                    setFormErrors((current) => ({ ...current, roomNumber: "" }));
                                                }}
                                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                                placeholder="e.g. 1205"
                                                required
                                            />
                                            {formErrors.roomNumber && <div className="field-error">{formErrors.roomNumber}</div>}
                                        </div>
                                        <div>
                                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Floor</label>
                                            <input
                                                value={form.floor}
                                                onChange={(e) => {
                                                    setForm((current) => ({ ...current, floor: e.target.value }));
                                                    setFormErrors((current) => ({ ...current, floor: "" }));
                                                }}
                                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                                placeholder="e.g. 12"
                                                required
                                            />
                                            {formErrors.floor && <div className="field-error">{formErrors.floor}</div>}
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Category</label>
                                            <select
                                                value={form.roomCategory}
                                                onChange={(e) => setForm((current) => ({ ...current, roomCategory: e.target.value }))}
                                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                            >
                                                {roomCatalog.roomCategories.map((option: string) => (
                                                    <option key={option} value={option}>{option}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div>
                                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Climate Type</label>
                                            <select
                                                value={form.climateControl}
                                                onChange={(e) => setForm((current) => ({ ...current, climateControl: e.target.value }))}
                                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                            >
                                                {roomCatalog.climateControls.map((option: string) => (
                                                    <option key={option} value={option}>{option}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>

                                    <div className="rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4">
                                        <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-luxury-800/45">Room preview</div>
                                        <div className="mt-2 text-lg font-semibold text-luxury-900">
                                            Room {form.roomNumber || "----"} - Floor {form.floor || "--"}
                                        </div>
                                        <div className="mt-1 text-sm text-luxury-800/60">
                                            {form.roomCategory} - {form.climateControl}
                                        </div>
                                    </div>

                                    <div className="flex justify-between gap-3 pt-2">
                                        <PillButton type="button" onClick={closeComposer}>
                                            Cancel
                                        </PillButton>
                                        <PillButton primary type="submit" disabled={isBinding}>
                                            {isBinding ? "Binding..." : "Confirm Binding"}
                                        </PillButton>
                                    </div>
                                </form>
                            </BentoCard>
                        </motion.div>
                    ) : (
                        <motion.div
                            key="binding-placeholder"
                            initial={{ opacity: 0, x: 14 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 0.28 }}
                        >
                            <BentoCard title="Assign Room Profile" eyebrow="Step 2 - Waiting">
                                <div className="mt-4 min-h-[420px] rounded-[28px] border-2 border-dashed border-luxury-200 bg-[radial-gradient(circle_at_top,_rgba(201,168,76,0.14),_transparent_52%),linear-gradient(180deg,rgba(255,255,255,0.88),rgba(247,244,236,0.9))] px-8 py-10 flex flex-col items-center justify-center text-center">
                                    <div className="w-20 h-20 rounded-full bg-gold-500/10 border border-gold-500/20 flex items-center justify-center text-gold-600 text-2xl shadow-[0_16px_40px_-24px_rgba(201,168,76,0.9)]">
                                        2
                                    </div>
                                    <div className="mt-6 text-xl font-semibold text-luxury-900">Select a pending TV or bind manually</div>
                                    <p className="mt-3 max-w-sm text-sm text-luxury-800/60">
                                        The room-assignment panel stays hidden until staff intentionally enters the second step. That makes the binding flow easier to notice and harder to misuse.
                                    </p>
                                    <div className="pt-6">
                                        <PillButton primary type="button" onClick={activateManualBinding}>
                                            Bind manually
                                        </PillButton>
                                    </div>
                                </div>
                            </BentoCard>
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>
        </div>
    );
}
