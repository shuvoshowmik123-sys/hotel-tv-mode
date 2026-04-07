"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AnimatePresence, motion } from "motion/react";
import { BentoCard } from "../../../components/BentoCard";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { useFeedback } from "../../../components/FeedbackProvider";
import { api } from "../../../lib/api";

function roomStatusTone(status?: string): "occupied" | "vacant" | "warning" | "default" {
    if (status === "occupied") return "occupied";
    if (status === "unbound") return "warning";
    if (status === "vacant") return "vacant";
    return "default";
}

export default function SessionsPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
    const searchParams = useSearchParams();
    const router = useRouter();
    const { notify } = useFeedback();

    const updateQuery = (updates: Record<string, string | null>) => {
        const params = new URLSearchParams(searchParams?.toString() || "");
        Object.entries(updates).forEach(([key, value]) => {
            if (value && value.trim()) params.set(key, value);
            else params.delete(key);
        });
        const next = params.toString();
        router.replace(next ? `/sessions?${next}` : "/sessions");
    };

    const load = async () => {
        setLoadError("");
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (error: any) {
            setLoadError(error.message || "Unable to load guest sessions.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const query = (searchParams?.get("room") || "").trim().toLowerCase();
    const selectedRoomId = searchParams?.get("selected") || null;
    const rooms = useMemo(
        () => data?.rooms
            ? Object.values(data.rooms)
                .filter((room: any) => room.roomNumber.toLowerCase().includes(query))
                .sort((a: any, b: any) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }))
            : [],
        [data?.rooms, query]
    );
    const selectedRoom: any = selectedRoomId ? data?.rooms?.[selectedRoomId] : null;

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="space-y-6">
            {loadError && <div className="page-inline-error">{loadError}</div>}

            <BentoCard title="Guest Sessions" eyebrow="Select room, then manage stay details">
                {rooms.length === 0 ? (
                    <div className="mt-4 rounded-2xl border-2 border-dashed border-luxury-200 p-6 text-sm text-luxury-800/55">
                        No rooms exist yet. Create rooms first, then start guest sessions here.
                    </div>
                ) : (
                    <div className="mt-4 overflow-x-auto">
                        <table className="w-full text-left">
                            <thead>
                                <tr className="border-b border-luxury-200">
                                    <th className="pb-3 font-mono text-xs font-bold uppercase tracking-wider text-luxury-800/60">Room</th>
                                    <th className="pb-3 font-mono text-xs font-bold uppercase tracking-wider text-luxury-800/60">Guest</th>
                                    <th className="pb-3 font-mono text-xs font-bold uppercase tracking-wider text-luxury-800/60">Status</th>
                                    <th className="pb-3 font-mono text-xs font-bold uppercase tracking-wider text-luxury-800/60">Check In</th>
                                    <th className="pb-3"></th>
                                </tr>
                            </thead>
                            <tbody>
                                {rooms.map((room: any) => (
                                    <tr key={room.roomNumber} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                        <td className="py-4 font-mono font-bold text-gold-600">{room.roomNumber}</td>
                                        <td className="py-4 text-sm font-medium text-luxury-900">{room.guestName || "No active guest"}</td>
                                        <td className="py-4"><StatusPill status={roomStatusTone(room.status)} label={room.status || "unknown"} /></td>
                                        <td className="py-4 text-sm font-mono text-luxury-800/50">{room.checkInAt ? new Date(room.checkInAt).toLocaleString() : "-"}</td>
                                        <td className="py-4 text-right">
                                            <PillButton className="!px-4 !py-1.5 !text-xs" onClick={() => updateQuery({ selected: room.roomNumber })}>
                                                {room.guestName ? "Manage Session" : "Start Session"}
                                            </PillButton>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </BentoCard>

            <AnimatePresence>
                {selectedRoom && (
                    <>
                        <motion.div className="fixed inset-0 z-40 bg-black/45 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => updateQuery({ selected: null })} />
                        <motion.div className="fixed inset-0 z-50 overflow-y-auto p-4 lg:p-8" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 12 }}>
                            <div className="mx-auto max-w-3xl" onClick={(event) => event.stopPropagation()}>
                                <BentoCard title="Session Detail" eyebrow="Guest stay editor" className="min-h-[calc(100vh-4rem)]">
                                    <form
                                        key={selectedRoom.roomNumber}
                                        className="mt-4 space-y-4"
                                        onSubmit={async (event) => {
                                            event.preventDefault();
                                            const fd = new FormData(event.currentTarget);
                                            const payload = {
                                                guestName: `${fd.get("guestName") || ""}`.trim(),
                                                welcomeNote: `${fd.get("welcomeNote") || ""}`.trim(),
                                                language: `${fd.get("language") || ""}`.trim() || "English",
                                            };
                                            if (!payload.guestName) {
                                                notify({ tone: "warning", message: "Guest name is required to start or update a session." });
                                                return;
                                            }
                                            try {
                                                if (selectedRoom.guestName) {
                                                    await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}`, { method: "PATCH", body: JSON.stringify(payload) });
                                                    notify({ tone: "success", message: `Updated guest session for room ${selectedRoom.roomNumber}.` });
                                                } else {
                                                    await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}/checkin`, { method: "POST", body: JSON.stringify(payload) });
                                                    notify({ tone: "success", message: `Started guest session for room ${selectedRoom.roomNumber}.` });
                                                }
                                                await load();
                                            } catch (error: any) {
                                                notify({ tone: "error", message: error.message || "Failed to save guest session." });
                                            }
                                        }}
                                    >
                                        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-luxury-100 pb-5">
                                            <div>
                                                <div className="font-mono text-4xl font-bold text-gold-500">{selectedRoom.roomNumber}</div>
                                                <div className="mt-2"><StatusPill status={roomStatusTone(selectedRoom.status)} label={selectedRoom.status || "unknown"} /></div>
                                            </div>
                                            <div className="flex gap-3">
                                                {selectedRoom.guestName && <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout Room</PillButton>}
                                                <PillButton type="button" onClick={() => updateQuery({ selected: null })}>Back</PillButton>
                                            </div>
                                        </div>
                                        <div>
                                            <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Guest Name</label>
                                            <input name="guestName" defaultValue={selectedRoom.guestName || ""} className="field-input" />
                                        </div>
                                        <div>
                                            <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Language</label>
                                            <input name="language" defaultValue={selectedRoom.language || "English"} className="field-input" />
                                        </div>
                                        <div>
                                            <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Welcome Note</label>
                                            <textarea name="welcomeNote" defaultValue={selectedRoom.welcomeNote || ""} rows={4} className="field-input resize-none" />
                                        </div>
                                        <div className="flex justify-end gap-3 pt-4">
                                            <PillButton primary type="submit">{selectedRoom.guestName ? "Update Session" : "Start Session"}</PillButton>
                                        </div>
                                    </form>
                                </BentoCard>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>

            <ConfirmModal open={Boolean(checkoutTarget)} title="Confirm Checkout" description={checkoutTarget ? `Clear the guest session for room ${checkoutTarget.roomNumber}?` : ""} confirmLabel="Checkout Room" onCancel={() => setCheckoutTarget(null)} onConfirm={async () => {
                if (!checkoutTarget) return;
                try {
                    await api(`/api/admin/rooms/${encodeURIComponent(checkoutTarget.roomNumber)}/checkout`, { method: "POST" });
                    notify({ tone: "success", message: `Checked out room ${checkoutTarget.roomNumber}.` });
                    setCheckoutTarget(null);
                    await load();
                } catch (error: any) {
                    notify({ tone: "error", message: error.message || "Failed to checkout room." });
                    setCheckoutTarget(null);
                }
            }} />
        </div>
    );
}
