"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
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
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
    const [message, setMessage] = useState("");
    const searchParams = useSearchParams();

    const load = async () => {
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const query = (searchParams?.get("room") || "").trim().toLowerCase();
    const rooms = useMemo(
        () =>
            data?.rooms
                ? Object.values(data.rooms)
                    .filter((room: any) => room.roomNumber.toLowerCase().includes(query))
                    .sort((a: any, b: any) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }))
                : [],
        [data?.rooms, query]
    );

    const selectedRoom: any = selectedRoomId ? data?.rooms?.[selectedRoomId] : null;

    useEffect(() => {
        if (!rooms.length) {
            setSelectedRoomId(null);
            return;
        }
        if (!selectedRoomId || !rooms.some((room: any) => room.roomNumber === selectedRoomId)) {
            setSelectedRoomId((rooms[0] as any).roomNumber);
        }
    }, [rooms, selectedRoomId]);

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="space-y-6">
            {message && (
                <div className={`rounded-2xl px-4 py-3 text-sm ${message.includes("Failed") ? "bg-red-50 text-red-700" : "bg-green-50 text-green-700"}`}>
                    {message}
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BentoCard title="Guest Sessions" eyebrow="All Active and Ready Rooms">
                    {rooms.length === 0 ? (
                        <div className="text-luxury-800/60 text-sm mt-4">No rooms exist yet. Create rooms first, then start guest sessions here.</div>
                    ) : (
                        <div className="overflow-x-auto mt-4">
                            <table className="w-full text-left">
                                <thead>
                                    <tr className="border-b border-luxury-200">
                                        <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 font-mono">Room</th>
                                        <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 font-mono">Guest</th>
                                        <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 font-mono">Status</th>
                                        <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 font-mono">Check In</th>
                                        <th className="pb-3"></th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {rooms.map((room: any) => (
                                        <tr key={room.roomNumber} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                            <td className="py-4 font-mono text-gold-600 font-bold">{room.roomNumber}</td>
                                            <td className="py-4 text-sm font-medium text-luxury-900">{room.guestName || "No active guest"}</td>
                                            <td className="py-4">
                                                <StatusPill status={roomStatusTone(room.status)} label={room.status || "unknown"} />
                                            </td>
                                            <td className="py-4 text-sm font-mono text-luxury-800/50">
                                                {room.checkInAt ? new Date(room.checkInAt).toLocaleString() : "-"}
                                            </td>
                                            <td className="py-4 text-right">
                                                <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => setSelectedRoomId(room.roomNumber)}>
                                                    {room.guestName ? "Edit Session" : "Start Session"}
                                                </PillButton>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </BentoCard>

                <BentoCard title="Session Editor" eyebrow="Guest Details">
                    {selectedRoom ? (
                        <form
                            key={selectedRoom.roomNumber}
                            className="space-y-4 mt-4"
                            onSubmit={async (e) => {
                                e.preventDefault();
                                setMessage("");
                                const fd = new FormData(e.currentTarget);
                                const payload = {
                                    guestName: `${fd.get("guestName") || ""}`.trim(),
                                    welcomeNote: `${fd.get("welcomeNote") || ""}`.trim(),
                                    language: `${fd.get("language") || ""}`.trim() || "English",
                                };

                                if (!payload.guestName) {
                                    setMessage("Guest name is required to start or update a session.");
                                    return;
                                }

                                try {
                                    if (selectedRoom.guestName) {
                                        await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}`, {
                                            method: "PATCH",
                                            body: JSON.stringify(payload)
                                        });
                                        setMessage(`Updated guest session for room ${selectedRoom.roomNumber}.`);
                                    } else {
                                        await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}/checkin`, {
                                            method: "POST",
                                            body: JSON.stringify(payload)
                                        });
                                        setMessage(`Started guest session for room ${selectedRoom.roomNumber}.`);
                                    }
                                    await load();
                                } catch (error: any) {
                                    setMessage(error.message || "Failed to save guest session.");
                                }
                            }}
                        >
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Number</label>
                                <input value={selectedRoom.roomNumber} readOnly className="w-full bg-luxury-100 border border-luxury-200 rounded-xl px-4 py-3 text-sm text-luxury-800 cursor-not-allowed" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Current Status</label>
                                <div className="pt-1">
                                    <StatusPill status={roomStatusTone(selectedRoom.status)} label={selectedRoom.status || "unknown"} />
                                </div>
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name</label>
                                <input name="guestName" defaultValue={selectedRoom.guestName || ""} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Language</label>
                                <input name="language" defaultValue={selectedRoom.language || "English"} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Welcome Note</label>
                                <textarea name="welcomeNote" defaultValue={selectedRoom.welcomeNote || ""} rows={3} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors resize-none" />
                            </div>
                            <div className="flex justify-end gap-3 pt-4">
                                {selectedRoom.guestName && (
                                    <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout Room</PillButton>
                                )}
                                <PillButton primary type="submit">
                                    {selectedRoom.guestName ? "Update Session" : "Start Session"}
                                </PillButton>
                            </div>
                        </form>
                    ) : (
                        <div className="text-luxury-800/50 mt-4 h-32 flex items-center justify-center border-2 border-dashed border-luxury-200 rounded-2xl">
                            Select a room to start or edit a guest session
                        </div>
                    )}
                </BentoCard>
            </div>

            <ConfirmModal
                open={Boolean(checkoutTarget)}
                title="Confirm Checkout"
                description={checkoutTarget ? `Clear the guest session for room ${checkoutTarget.roomNumber}?` : ""}
                confirmLabel="Checkout Room"
                onCancel={() => setCheckoutTarget(null)}
                onConfirm={async () => {
                    if (!checkoutTarget) return;
                    try {
                        await api(`/api/admin/rooms/${encodeURIComponent(checkoutTarget.roomNumber)}/checkout`, { method: "POST" });
                        setMessage(`Checked out room ${checkoutTarget.roomNumber}.`);
                        setCheckoutTarget(null);
                        await load();
                    } catch (error: any) {
                        setMessage(error.message || "Failed to checkout room.");
                        setCheckoutTarget(null);
                    }
                }}
            />
        </div>
    );
}
