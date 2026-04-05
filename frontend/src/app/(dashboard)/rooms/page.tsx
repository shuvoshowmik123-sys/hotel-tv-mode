"use client";

import React, { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { api } from "../../../lib/api";
import { canManageRoomOverrides, isRoomManagementReadOnly } from "../../../lib/permissions";

export default function RoomsPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
    const searchParams = useSearchParams();

    const load = async () => {
        try {
            const state = await api("/api/admin/state");
            setData(state);
            if (!selectedRoomId && state.rooms) {
                setSelectedRoomId(Object.keys(state.rooms)[0]);
            }
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);
    const query = (searchParams?.get("room") || "").trim().toLowerCase();
    const rooms = data?.rooms ? Object.values(data.rooms).filter((room: any) => room.roomNumber.toLowerCase().includes(query)) : [];
    const selectedRoom: any = rooms.find((r: any) => r.roomNumber === selectedRoomId);
    const roomReadOnly = isRoomManagementReadOnly(data?.currentUser?.role);
    const canOverride = canManageRoomOverrides(data?.currentUser?.role);

    useEffect(() => {
        if (!rooms.length) return;
        if (!selectedRoomId || !rooms.some((room: any) => room.roomNumber === selectedRoomId)) {
            setSelectedRoomId((rooms[0] as any).roomNumber);
        }
    }, [rooms, selectedRoomId]);

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="space-y-6">
            <BentoCard className="flex items-center gap-4 py-4">
                <span className="text-sm font-bold uppercase tracking-wider text-luxury-800/60 mr-4">Summary</span>
                <StatusPill status="occupied" label={`Occupied ${data.metrics?.occupiedRooms || 0}`} />
                <StatusPill status="vacant" label={`Vacant ${data.metrics?.vacantRooms || 0}`} />
                <StatusPill status="warning" label={`Unbound ${data.metrics?.unboundDevices || 0}`} />
            </BentoCard>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BentoCard title="Floor Map" eyebrow="Room Status Overview">
                    <div className="flex flex-wrap gap-3 mt-4">
                        {rooms.map((room: any, i: number) => {
                            const isSelected = room.roomNumber === selectedRoomId;
                            const bgClass = isSelected ? "bg-gold-500 text-white border-gold-600" : "bg-white border-luxury-200 hover:border-gold-400";
                            return (
                                <button
                                    key={i}
                                    onClick={() => setSelectedRoomId(room.roomNumber)}
                                    className={`w-20 h-20 rounded-[16px] border flex flex-col items-center justify-center transition-all shadow-sm ${bgClass}`}
                                >
                                    <span className="font-mono font-bold text-lg">{room.roomNumber}</span>
                                    <span className={`text-[10px] font-medium tracking-wider uppercase ${isSelected ? "text-white/80" : "text-luxury-800/50"}`}>
                                        {room.status}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </BentoCard>

                <BentoCard title="Room Detail" eyebrow="Selected Room Profile">
                    {selectedRoom ? (
                        <div className="mt-4">
                            <div className="flex items-center justify-between mb-8 pb-6 border-b border-luxury-100">
                                <div className="text-4xl font-mono text-gold-500">{selectedRoom.roomNumber}</div>
                                <StatusPill
                                    status={selectedRoom.status === "occupied" ? "occupied" : selectedRoom.status === "unbound" ? "warning" : "vacant"}
                                    label={selectedRoom.status}
                                />
                            </div>

                            <div className="grid grid-cols-2 gap-y-6 gap-x-4 mb-8">
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Guest</div>
                                    <div className="text-sm font-medium text-luxury-900">{selectedRoom.guestName || "Vacant"}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Device ID</div>
                                    <div className="text-sm font-mono text-luxury-800/60">{selectedRoom.deviceId || "Not bound"}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Last Sync</div>
                                    <div className="text-sm font-mono text-luxury-800/60">{selectedRoom.lastSyncAt ? new Date(selectedRoom.lastSyncAt).toLocaleTimeString() : "N/A"}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Override</div>
                                    {canOverride ? (
                                        <PillButton
                                            className="!py-1.5 !px-3 !text-xs mt-1"
                                            primary={!selectedRoom.overrideEnabled}
                                            onClick={async () => {
                                                await api(`/api/admin/rooms/${selectedRoom.roomNumber}/override`, {
                                                    method: "POST",
                                                    body: JSON.stringify({ enabled: !selectedRoom.overrideEnabled, customContentLabel: "" })
                                                });
                                                load();
                                            }}
                                        >
                                            {selectedRoom.overrideEnabled ? "Disable Override" : "Enable Override"}
                                        </PillButton>
                                    ) : (
                                        <span className="text-sm text-luxury-800/50">View only</span>
                                    )}
                                </div>
                            </div>

                            {/* Guest Form */}
                            {roomReadOnly ? (
                                <div className="pt-6 border-t border-luxury-100">
                                    <div className="text-sm text-luxury-800/50">
                                        Reception can view room details here. Guest editing remains available in Guest Sessions.
                                    </div>
                                </div>
                            ) : (
                                <form className="space-y-4 pt-6 border-t border-luxury-100" onSubmit={async (e) => {
                                    e.preventDefault();
                                    const fd = new FormData(e.currentTarget);
                                    await api("/api/admin/rooms", {
                                        method: "POST",
                                        body: JSON.stringify(Object.fromEntries(fd))
                                    });
                                    load();
                                }}>
                                    <input type="hidden" name="roomNumber" value={selectedRoom.roomNumber} />
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name</label>
                                        <input name="guestName" defaultValue={selectedRoom.guestName || ""} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50" />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Welcome Note</label>
                                        <textarea name="welcomeNote" defaultValue={selectedRoom.welcomeNote || ""} rows={2} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 resize-none" />
                                    </div>
                                    <div className="flex justify-end gap-3 pt-2">
                                        <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout</PillButton>
                                        <PillButton primary type="submit">Save Changes</PillButton>
                                    </div>
                                </form>
                            )}

                        </div>
                    ) : (
                        <div className="text-luxury-800/50 mt-4">No room selected.</div>
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
                    await api(`/api/admin/rooms/${checkoutTarget.roomNumber}/checkout`, { method: "POST" });
                    setCheckoutTarget(null);
                    load();
                }}
            />
        </div>
    );
}
