"use client";

import React, { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { api } from "../../../lib/api";

export default function SessionsPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
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
    const occupiedRooms = data?.rooms ? Object.values(data.rooms).filter((r: any) => r.guestName && r.roomNumber.toLowerCase().includes(query)) : [];
    const selectedRoom: any = selectedRoomId ? data?.rooms?.[selectedRoomId] : null;

    useEffect(() => {
        if (!occupiedRooms.length) return;
        if (!selectedRoomId || !occupiedRooms.some((room: any) => room.roomNumber === selectedRoomId)) {
            setSelectedRoomId((occupiedRooms[0] as any).roomNumber);
        }
    }, [occupiedRooms, selectedRoomId]);

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <BentoCard title="Active Sessions" eyebrow="Current Guests">
                {occupiedRooms.length === 0 ? (
                    <div className="text-luxury-800/60 text-sm mt-4">No active sessions right now.</div>
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
                                {occupiedRooms.map((room: any) => (
                                    <tr key={room.roomNumber} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                        <td className="py-4 font-mono text-gold-600 font-bold">{room.roomNumber}</td>
                                        <td className="py-4 text-sm font-medium text-luxury-900">{room.guestName}</td>
                                        <td className="py-4"><StatusPill status="occupied" label={room.status} /></td>
                                        <td className="py-4 text-sm font-mono text-luxury-800/50">{room.checkInAt ? new Date(room.checkInAt).toLocaleTimeString() : "-"}</td>
                                        <td className="py-4 text-right">
                                            <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => setSelectedRoomId(room.roomNumber)}>Edit</PillButton>
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
                    <form className="space-y-4 mt-4" onSubmit={async (e) => {
                        e.preventDefault();
                        const fd = new FormData(e.currentTarget);
                        await api("/api/admin/rooms", {
                            method: "POST",
                            body: JSON.stringify(Object.fromEntries(fd))
                        });
                        setSelectedRoomId(null);
                        load();
                    }}>
                        <input type="hidden" name="roomNumber" value={selectedRoom.roomNumber} />
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Number</label>
                            <input value={selectedRoom.roomNumber} readOnly className="w-full bg-luxury-100 border border-luxury-200 rounded-xl px-4 py-3 text-sm text-luxury-800 cursor-not-allowed" />
                        </div>
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name</label>
                            <input name="guestName" defaultValue={selectedRoom.guestName || ""} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors" />
                        </div>
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Welcome Note</label>
                            <textarea name="welcomeNote" defaultValue={selectedRoom.welcomeNote || ""} rows={3} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors resize-none" />
                        </div>
                        <div className="flex justify-end gap-3 pt-4">
                            <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout Room</PillButton>
                            <PillButton primary type="submit">Save Changes</PillButton>
                        </div>
                    </form>
                ) : (
                    <div className="text-luxury-800/50 mt-4 h-32 flex items-center justify-center border-2 border-dashed border-luxury-200 rounded-2xl">
                        Select a session to edit
                    </div>
                )}
            </BentoCard>
            <ConfirmModal
                open={Boolean(checkoutTarget)}
                title="Confirm Checkout"
                description={checkoutTarget ? `Clear the guest session for room ${checkoutTarget.roomNumber}?` : ""}
                confirmLabel="Checkout Room"
                onCancel={() => setCheckoutTarget(null)}
                onConfirm={async () => {
                    if (!checkoutTarget) return;
                    await api(`/api/admin/rooms/${checkoutTarget.roomNumber}/checkout`, { method: "POST" });
                    setCheckoutTarget(null);
                    setSelectedRoomId(null);
                    load();
                }}
            />
        </div>
    );
}
