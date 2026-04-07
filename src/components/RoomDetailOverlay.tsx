"use client";

import React from "react";
import { AnimatePresence, motion } from "motion/react";
import { BentoCard } from "./BentoCard";
import { PillButton, StatusPill } from "./UIElements";
import { canManageRoomOverrides } from "../lib/permissions";
import { CLIMATE_CONTROL_OPTIONS, ROOM_CATEGORY_OPTIONS, climateControlBadge, roomCategoryBadge } from "../lib/roomOptions";

function roomStatusTone(status?: string): "occupied" | "vacant" | "warning" | "default" {
    if (status === "occupied") return "occupied";
    if (status === "unbound" || status === "archived") return "warning";
    if (status === "vacant") return "vacant";
    return "default";
}

function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
    return (
        <div className="rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4">
            <div className="text-[11px] font-bold uppercase tracking-[0.22em] text-luxury-800/45">{label}</div>
            <div className="mt-2 text-sm font-semibold text-luxury-900">{value}</div>
        </div>
    );
}

export function RoomDetailOverlay({
    room,
    role,
    onClose,
    onSave,
    onCheckout,
    onUnbind,
    onDelete,
    onToggleOverride,
}: {
    room: any;
    role?: string;
    onClose: () => void;
    onSave: (payload: Record<string, string>) => Promise<void>;
    onCheckout: () => void;
    onUnbind: () => void;
    onDelete: () => void;
    onToggleOverride: () => void;
}) {
    const canEditRooms = role === "SUPER_ADMIN" || role === "ADMIN";
    const canDeleteRooms = canEditRooms;
    const canManageBindings = role === "SUPER_ADMIN" || role === "ADMIN";
    const canManageSessions = role === "SUPER_ADMIN" || role === "ADMIN" || role === "RECEPTIONIST";
    const canManageRooms = canManageRoomOverrides(role);

    return (
        <AnimatePresence>
            <motion.div
                className="fixed inset-0 z-40 bg-black/45 backdrop-blur-sm"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={onClose}
            />
            <motion.div
                className="fixed inset-0 z-50 overflow-y-auto p-4 lg:p-8"
                initial={{ opacity: 0, y: 18 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 12 }}
                transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
            >
                <div className="mx-auto max-w-6xl" onClick={(event) => event.stopPropagation()}>
                    <BentoCard className="min-h-[calc(100vh-4rem)]">
                        <div className="flex flex-col gap-6">
                            <div className="flex flex-col gap-4 border-b border-luxury-100 pb-6 lg:flex-row lg:items-start lg:justify-between">
                                <div>
                                    <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-luxury-800/45">Room profile</div>
                                    <div className="mt-2 flex flex-wrap items-center gap-3">
                                        <div className="font-mono text-5xl font-bold text-gold-500">{room.roomNumber}</div>
                                        <StatusPill status={roomStatusTone(room.status)} label={room.status || "unknown"} />
                                    </div>
                                    <div className="mt-3 flex flex-wrap gap-2">
                                        <span className="rounded-full bg-gold-500/10 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-gold-700">
                                            {roomCategoryBadge(room.roomCategory)}
                                        </span>
                                        <span className="rounded-full bg-luxury-100 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-luxury-700">
                                            {climateControlBadge(room.climateControl)}
                                        </span>
                                        <span className="rounded-full border border-luxury-200 bg-white px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-luxury-700">
                                            Floor {room.floor || "Not set"}
                                        </span>
                                    </div>
                                </div>
                                <div className="flex flex-wrap items-start justify-end gap-3">
                                    {room.guestName && canManageSessions && <PillButton type="button" onClick={onCheckout}>Checkout</PillButton>}
                                    {canManageBindings && <PillButton type="button" onClick={onUnbind} disabled={!room.deviceId}>Unbind TV</PillButton>}
                                    {canDeleteRooms && <PillButton type="button" onClick={onDelete}>Delete / Archive</PillButton>}
                                    <PillButton
                                        type="button"
                                        onClick={onClose}
                                        className="!h-11 !w-11 !p-0 text-[28px] leading-none"
                                        aria-label="Close room details"
                                        title="Close room details"
                                    >
                                        ×
                                    </PillButton>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 gap-6 xl:grid-cols-[0.9fr_1.1fr]">
                                <div className="space-y-4">
                                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                        <DetailRow label="Guest" value={room.guestName || "Vacant"} />
                                        <DetailRow label="TV Binding" value={room.deviceId || "Not bound"} />
                                        <DetailRow label="Language" value={room.language || "English"} />
                                        <DetailRow label="Last Sync" value={room.lastSyncAt ? new Date(room.lastSyncAt).toLocaleString() : "N/A"} />
                                        <DetailRow label="Session History" value={room.hasSessionHistory ? "Has past guest activity" : "No past guest activity"} />
                                        <DetailRow label="Binding History" value={room.hasBindingHistory ? "Has been bound before" : "Never bound yet"} />
                                    </div>

                                    <BentoCard title="Operational Notes" eyebrow="What staff should know">
                                        <div className="mt-4 space-y-3 text-sm text-luxury-800/65">
                                            <div>This screen is the complete operational profile for the room.</div>
                                            <div>Staff should be able to update the room profile, confirm the TV state, and handle checkout or unbinding from here.</div>
                                        </div>
                                    </BentoCard>
                                </div>

                                <BentoCard title="Room Actions" eyebrow="Edit profile and next steps">
                                    {!canEditRooms ? (
                                        <div className="mt-4 rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4 text-sm text-luxury-800/60">
                                            This role can review room details here, but editing is limited to higher-level staff.
                                        </div>
                                    ) : (
                                        <form
                                            key={room.roomNumber}
                                            className="mt-4 space-y-4"
                                            onSubmit={async (event) => {
                                                event.preventDefault();
                                                const fd = new FormData(event.currentTarget);
                                                await onSave({
                                                    floor: `${fd.get("floor") || ""}`.trim(),
                                                    roomCategory: `${fd.get("roomCategory") || ""}`.trim(),
                                                    climateControl: `${fd.get("climateControl") || ""}`.trim(),
                                                    guestName: `${fd.get("guestName") || ""}`.trim(),
                                                    welcomeNote: `${fd.get("welcomeNote") || ""}`.trim(),
                                                    language: `${fd.get("language") || ""}`.trim() || "English",
                                                });
                                            }}
                                        >
                                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                                <div>
                                                    <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Floor</label>
                                                    <input name="floor" defaultValue={room.floor || ""} className="field-input" />
                                                </div>
                                                <div>
                                                    <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Language</label>
                                                    <input name="language" defaultValue={room.language || "English"} className="field-input" />
                                                </div>
                                                <div>
                                                    <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Room Category</label>
                                                    <select name="roomCategory" defaultValue={room.roomCategory || ROOM_CATEGORY_OPTIONS[0]} className="field-input">
                                                        {ROOM_CATEGORY_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
                                                    </select>
                                                </div>
                                                <div>
                                                    <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Climate Type</label>
                                                    <select name="climateControl" defaultValue={room.climateControl || CLIMATE_CONTROL_OPTIONS[0]} className="field-input">
                                                        {CLIMATE_CONTROL_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
                                                    </select>
                                                </div>
                                            </div>
                                            <div>
                                                <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Guest Name</label>
                                                <input name="guestName" defaultValue={room.guestName || ""} className="field-input" />
                                            </div>
                                            <div>
                                                <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Welcome Note</label>
                                                <textarea name="welcomeNote" defaultValue={room.welcomeNote || ""} rows={3} className="field-input resize-none" />
                                            </div>
                                            <div className="flex flex-wrap justify-end gap-3 pt-2">
                                                {canManageRooms && (
                                                    <PillButton type="button" primary={!room.overrideEnabled} onClick={onToggleOverride}>
                                                        {room.overrideEnabled ? "Disable Override" : "Enable Override"}
                                                    </PillButton>
                                                )}
                                                <PillButton primary type="submit">Save Changes</PillButton>
                                            </div>
                                        </form>
                                    )}
                                </BentoCard>
                            </div>
                        </div>
                    </BentoCard>
                </div>
            </motion.div>
        </AnimatePresence>
    );
}
