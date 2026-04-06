"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { api } from "../../../lib/api";
import { canManageRoomOverrides, canModuleAction, isRoomManagementReadOnly } from "../../../lib/permissions";
import { CLIMATE_CONTROL_OPTIONS, ROOM_CATEGORY_OPTIONS, climateControlBadge, roomCategoryBadge } from "../../../lib/roomOptions";

function roomStatusTone(status?: string): "occupied" | "vacant" | "warning" | "default" {
    if (status === "occupied") return "occupied";
    if (status === "unbound" || status === "archived") return "warning";
    if (status === "vacant") return "vacant";
    return "default";
}

export default function RoomsPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
    const [unbindTarget, setUnbindTarget] = useState<any>(null);
    const [deleteTarget, setDeleteTarget] = useState<any>(null);
    const [isAddingRoom, setIsAddingRoom] = useState(false);
    const [newRoomNumber, setNewRoomNumber] = useState("");
    const [newFloor, setNewFloor] = useState("");
    const [newRoomCategory, setNewRoomCategory] = useState<string>(ROOM_CATEGORY_OPTIONS[0]);
    const [newClimateControl, setNewClimateControl] = useState<string>(CLIMATE_CONTROL_OPTIONS[0]);
    const [newRoomError, setNewRoomError] = useState("");
    const [message, setMessage] = useState("");
    const searchParams = useSearchParams();

    const load = async () => {
        try {
            const state = await api("/api/admin/state");
            setData(state);
            if (!selectedRoomId && state.rooms) {
                const firstRoom = Object.values(state.rooms)[0] as any;
                setSelectedRoomId(firstRoom?.roomNumber || null);
            }
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

    const selectedRoom: any = rooms.find((room: any) => room.roomNumber === selectedRoomId) || null;
    const currentRole = data?.currentUser?.role;
    const roomReadOnly = isRoomManagementReadOnly(currentRole);
    const canCreateRooms = canModuleAction(currentRole, "rooms", "create");
    const canEditRooms = canModuleAction(currentRole, "rooms", "edit");
    const canDeleteRooms = canModuleAction(currentRole, "rooms", "delete");
    const canManageRooms = canManageRoomOverrides(currentRole);
    const canManageBindings = canModuleAction(currentRole, "binding", "manage");
    const canManageSessions = canModuleAction(currentRole, "sessions", "manage") || canModuleAction(currentRole, "sessions", "delete");

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

            <BentoCard className="flex items-center gap-4 py-4">
                <span className="text-sm font-bold uppercase tracking-wider text-luxury-800/60 mr-4">Summary</span>
                <StatusPill status="occupied" label={`Occupied ${data.metrics?.occupiedRooms || 0}`} />
                <StatusPill status="vacant" label={`Vacant ${data.metrics?.vacantRooms || 0}`} />
                <StatusPill status="warning" label={`Unbound ${data.metrics?.unboundDevices || 0}`} />
            </BentoCard>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BentoCard
                    title="Room Inventory"
                    eyebrow="Room Status Overview"
                    actions={
                        canCreateRooms && (
                            <PillButton primary onClick={() => {
                                setIsAddingRoom(true);
                                setNewRoomNumber("");
                                setNewFloor("");
                                setNewRoomCategory(ROOM_CATEGORY_OPTIONS[0]);
                                setNewClimateControl(CLIMATE_CONTROL_OPTIONS[0]);
                                setNewRoomError("");
                            }}>
                                Add Room
                            </PillButton>
                        )
                    }
                >
                    <div className="flex flex-wrap gap-3 mt-4">
                        {rooms.length === 0 && (
                            <div className="w-full min-h-[160px] flex items-center justify-center rounded-xl border-2 border-dashed border-luxury-200 text-sm font-medium text-luxury-800/50">
                                No rooms created yet.
                            </div>
                        )}
                        {rooms.map((room: any) => {
                            const isSelected = room.roomNumber === selectedRoomId;
                            const bgClass = isSelected ? "bg-gold-500 text-white border-gold-600" : "bg-white border-luxury-200 hover:border-gold-400";
                            return (
                                <button
                                    key={room.roomNumber}
                                    onClick={() => setSelectedRoomId(room.roomNumber)}
                                    className={`w-24 h-24 rounded-[16px] border flex flex-col items-center justify-center transition-all shadow-sm ${bgClass}`}
                                >
                                    <span className="font-mono font-bold text-lg">{room.roomNumber}</span>
                                    <span className={`text-[10px] font-medium tracking-wider uppercase ${isSelected ? "text-white/80" : "text-luxury-800/50"}`}>
                                        {room.status}
                                    </span>
                                    <span className={`text-[10px] mt-1 ${isSelected ? "text-white/80" : "text-luxury-800/40"}`}>
                                        {roomCategoryBadge(room.roomCategory)}
                                    </span>
                                    <span className={`text-[10px] ${isSelected ? "text-white/75" : "text-luxury-800/35"}`}>
                                        {climateControlBadge(room.climateControl)}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                </BentoCard>

                <BentoCard title="Room Detail" eyebrow="Selected Room Profile">
                    {selectedRoom ? (
                        <div className="mt-4">
                            <div className="flex items-center justify-between mb-8 pb-6 border-b border-luxury-100 gap-4">
                                <div>
                                    <div className="text-4xl font-mono text-gold-500">{selectedRoom.roomNumber}</div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mt-2">
                                        Floor {selectedRoom.floor || "Not set"}
                                    </div>
                                    <div className="flex flex-wrap gap-2 mt-3">
                                        <span className="rounded-full bg-gold-500/10 text-gold-700 px-3 py-1 text-[11px] font-bold uppercase tracking-wider">
                                            {roomCategoryBadge(selectedRoom.roomCategory)}
                                        </span>
                                        <span className="rounded-full bg-luxury-100 text-luxury-700 px-3 py-1 text-[11px] font-bold uppercase tracking-wider">
                                            {climateControlBadge(selectedRoom.climateControl)}
                                        </span>
                                    </div>
                                </div>
                                <StatusPill status={roomStatusTone(selectedRoom.status)} label={selectedRoom.status || "unknown"} />
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
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Language</div>
                                    <div className="text-sm font-medium text-luxury-900">{selectedRoom.language || "English"}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Room Category</div>
                                    <div className="text-sm font-medium text-luxury-900">{roomCategoryBadge(selectedRoom.roomCategory)}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Climate Type</div>
                                    <div className="text-sm font-medium text-luxury-900">{climateControlBadge(selectedRoom.climateControl)}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Last Sync</div>
                                    <div className="text-sm font-mono text-luxury-800/60">
                                        {selectedRoom.lastSyncAt ? new Date(selectedRoom.lastSyncAt).toLocaleString() : "N/A"}
                                    </div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Session History</div>
                                    <div className="text-sm font-medium text-luxury-900">{selectedRoom.hasSessionHistory ? "Has past guest activity" : "No past guest activity"}</div>
                                </div>
                                <div>
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-1">Binding History</div>
                                    <div className="text-sm font-medium text-luxury-900">{selectedRoom.hasBindingHistory ? "Has been bound before" : "Never bound yet"}</div>
                                </div>
                            </div>

                            {roomReadOnly ? (
                                <div className="pt-6 border-t border-luxury-100 space-y-3">
                                    <div className="text-sm text-luxury-800/50">
                                        Reception can review room status here. Guest check-in, session edits, and checkout are available in Guest Sessions.
                                    </div>
                                    {selectedRoom.guestName && canManageSessions && (
                                        <div className="flex justify-end">
                                            <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout Room</PillButton>
                                        </div>
                                    )}
                                </div>
                            ) : canEditRooms ? (
                                <form
                                    key={selectedRoom.roomNumber}
                                    className="space-y-4 pt-6 border-t border-luxury-100"
                                    onSubmit={async (e) => {
                                        e.preventDefault();
                                        setMessage("");
                                        try {
                                            const fd = new FormData(e.currentTarget);
                                            const payload = {
                                                floor: `${fd.get("floor") || ""}`.trim(),
                                                roomCategory: `${fd.get("roomCategory") || ""}`.trim(),
                                                climateControl: `${fd.get("climateControl") || ""}`.trim(),
                                                guestName: `${fd.get("guestName") || ""}`.trim(),
                                                welcomeNote: `${fd.get("welcomeNote") || ""}`.trim(),
                                                language: `${fd.get("language") || ""}`.trim() || "English",
                                            };
                                            await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}`, {
                                                method: "PATCH",
                                                body: JSON.stringify(payload)
                                            });
                                            setMessage(`Room ${selectedRoom.roomNumber} updated.`);
                                            await load();
                                        } catch (error: any) {
                                            setMessage(error.message || "Failed to update room.");
                                        }
                                    }}
                                >
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Floor</label>
                                        <input name="floor" defaultValue={selectedRoom.floor || ""} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50" />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Category</label>
                                        <select name="roomCategory" defaultValue={selectedRoom.roomCategory || ROOM_CATEGORY_OPTIONS[0]} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50">
                                            {ROOM_CATEGORY_OPTIONS.map((option) => (
                                                <option key={option} value={option}>{option}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Climate Type</label>
                                        <select name="climateControl" defaultValue={selectedRoom.climateControl || CLIMATE_CONTROL_OPTIONS[0]} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50">
                                            {CLIMATE_CONTROL_OPTIONS.map((option) => (
                                                <option key={option} value={option}>{option}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name</label>
                                        <input name="guestName" defaultValue={selectedRoom.guestName || ""} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50" />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Language</label>
                                        <input name="language" defaultValue={selectedRoom.language || "English"} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50" />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Welcome Note</label>
                                        <textarea name="welcomeNote" defaultValue={selectedRoom.welcomeNote || ""} rows={2} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 resize-none" />
                                    </div>
                                    <div className="flex flex-wrap justify-end gap-3 pt-2">
                                        {selectedRoom.guestName && canManageSessions && (
                                            <PillButton type="button" onClick={() => setCheckoutTarget(selectedRoom)}>Checkout</PillButton>
                                        )}
                                        {canManageBindings && (
                                            <PillButton type="button" onClick={() => setUnbindTarget(selectedRoom)} disabled={!selectedRoom.deviceId}>
                                                Unbind TV
                                            </PillButton>
                                        )}
                                        {canDeleteRooms && (
                                            <PillButton type="button" onClick={() => setDeleteTarget(selectedRoom)}>
                                                Delete / Archive
                                            </PillButton>
                                        )}
                                        {canManageRooms && (
                                            <PillButton
                                                type="button"
                                                primary={!selectedRoom.overrideEnabled}
                                                onClick={async () => {
                                                    setMessage("");
                                                    try {
                                                        await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}/override`, {
                                                            method: "POST",
                                                            body: JSON.stringify({ enabled: !selectedRoom.overrideEnabled, customContentLabel: "" })
                                                        });
                                                        setMessage(`${selectedRoom.overrideEnabled ? "Disabled" : "Enabled"} override for room ${selectedRoom.roomNumber}.`);
                                                        await load();
                                                    } catch (error: any) {
                                                        setMessage(error.message || "Failed to change room override.");
                                                    }
                                                }}
                                            >
                                                {selectedRoom.overrideEnabled ? "Disable Override" : "Enable Override"}
                                            </PillButton>
                                        )}
                                        <PillButton primary type="submit">Save Changes</PillButton>
                                    </div>
                                </form>
                            ) : (
                                <div className="pt-6 border-t border-luxury-100 text-sm text-luxury-800/50">
                                    This role can view room details here, but room editing is managed by higher-level staff.
                                </div>
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

            <ConfirmModal
                open={Boolean(unbindTarget)}
                title="Unbind TV"
                description={unbindTarget ? `Remove the current TV binding from room ${unbindTarget.roomNumber}?` : ""}
                confirmLabel="Unbind TV"
                onCancel={() => setUnbindTarget(null)}
                onConfirm={async () => {
                    if (!unbindTarget) return;
                    try {
                        await api(`/api/admin/rooms/${encodeURIComponent(unbindTarget.roomNumber)}/unbind`, { method: "POST" });
                        setMessage(`Room ${unbindTarget.roomNumber} is now unbound.`);
                        setUnbindTarget(null);
                        await load();
                    } catch (error: any) {
                        setMessage(error.message || "Failed to unbind room.");
                        setUnbindTarget(null);
                    }
                }}
            />

            <ConfirmModal
                open={Boolean(deleteTarget)}
                title="Delete or Archive Room"
                description={deleteTarget ? `Unused rooms are deleted. Rooms with binding or guest history are archived instead. Continue with room ${deleteTarget.roomNumber}?` : ""}
                confirmLabel="Continue"
                onCancel={() => setDeleteTarget(null)}
                onConfirm={async () => {
                    if (!deleteTarget) return;
                    try {
                        const result = await api(`/api/admin/rooms/${encodeURIComponent(deleteTarget.roomNumber)}`, { method: "DELETE" });
                        setMessage(result.archived ? `Archived room ${deleteTarget.roomNumber}.` : `Deleted room ${deleteTarget.roomNumber}.`);
                        setDeleteTarget(null);
                        if (selectedRoomId === deleteTarget.roomNumber) {
                            setSelectedRoomId(null);
                        }
                        await load();
                    } catch (error: any) {
                        setMessage(error.message || "Failed to remove room.");
                        setDeleteTarget(null);
                    }
                }}
            />

            {isAddingRoom && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
                    <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm" onClick={e => e.stopPropagation()}>
                        <h3 className="text-xl font-medium text-luxury-900 mb-2">Add New Room</h3>
                        <p className="text-sm text-luxury-800/60 mb-6">Create a vacant room before binding a TV or checking in a guest.</p>

                        <form onSubmit={async (e) => {
                            e.preventDefault();
                            setNewRoomError("");
                            if (!newRoomNumber.trim()) return;
                            try {
                                await api("/api/admin/rooms", {
                                    method: "POST",
                                    body: JSON.stringify({
                                        roomNumber: newRoomNumber.trim(),
                                        floor: newFloor.trim(),
                                        roomCategory: newRoomCategory,
                                        climateControl: newClimateControl
                                    })
                                });
                                setMessage(`Created room ${newRoomNumber.trim()}.`);
                                setIsAddingRoom(false);
                                setSelectedRoomId(newRoomNumber.trim());
                                await load();
                            } catch (err: any) {
                                setNewRoomError(err.message || "Failed to create room.");
                            }
                        }}>
                            <div className="mb-6">
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">
                                    Room Number
                                </label>
                                <input
                                    autoFocus
                                    type="text"
                                    value={newRoomNumber}
                                    onChange={e => setNewRoomNumber(e.target.value)}
                                    placeholder="e.g. 101"
                                    className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50"
                                />
                                {newRoomError && <div className="text-xs text-red-500 mt-2 font-medium">{newRoomError}</div>}
                            </div>

                            <div className="mb-4">
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">
                                    Floor
                                </label>
                                <input
                                    type="text"
                                    value={newFloor}
                                    onChange={e => setNewFloor(e.target.value)}
                                    placeholder="e.g. 12"
                                    className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50"
                                />
                            </div>

                            <div className="mb-4">
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">
                                    Room Category
                                </label>
                                <select
                                    value={newRoomCategory}
                                    onChange={e => setNewRoomCategory(e.target.value)}
                                    className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50"
                                >
                                    {ROOM_CATEGORY_OPTIONS.map((option) => (
                                        <option key={option} value={option}>{option}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="mb-6">
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">
                                    Climate Type
                                </label>
                                <select
                                    value={newClimateControl}
                                    onChange={e => setNewClimateControl(e.target.value)}
                                    className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50"
                                >
                                    {CLIMATE_CONTROL_OPTIONS.map((option) => (
                                        <option key={option} value={option}>{option}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex justify-end gap-3">
                                <PillButton type="button" onClick={() => setIsAddingRoom(false)}>Cancel</PillButton>
                                <PillButton primary type="submit" disabled={!newRoomNumber.trim()}>Create Room</PillButton>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
