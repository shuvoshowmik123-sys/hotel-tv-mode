"use client";

import React, { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AnimatePresence, motion } from "motion/react";
import { BentoCard } from "../../../components/BentoCard";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { RoomDetailOverlay } from "../../../components/RoomDetailOverlay";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { useFeedback } from "../../../components/FeedbackProvider";
import { api, ApiError } from "../../../lib/api";
import { canModuleAction } from "../../../lib/permissions";
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
    const [loadError, setLoadError] = useState("");
    const [checkoutTarget, setCheckoutTarget] = useState<any>(null);
    const [unbindTarget, setUnbindTarget] = useState<any>(null);
    const [deleteTarget, setDeleteTarget] = useState<any>(null);
    const [isAddingRoom, setIsAddingRoom] = useState(false);
    const [newRoomNumber, setNewRoomNumber] = useState("");
    const [newFloor, setNewFloor] = useState("");
    const [newRoomCategory, setNewRoomCategory] = useState<string>(ROOM_CATEGORY_OPTIONS[0]);
    const [newClimateControl, setNewClimateControl] = useState<string>(CLIMATE_CONTROL_OPTIONS[0]);
    const [newRoomErrors, setNewRoomErrors] = useState<Record<string, string>>({});
    const [inventorySearch, setInventorySearch] = useState("");
    const router = useRouter();
    const searchParams = useSearchParams();
    const { notify } = useFeedback();

    const updateQuery = (updates: Record<string, string | null>) => {
        const params = new URLSearchParams(searchParams?.toString() || "");
        Object.entries(updates).forEach(([key, value]) => {
            if (value && value.trim()) params.set(key, value);
            else params.delete(key);
        });
        const next = params.toString();
        router.replace(next ? `/rooms?${next}` : "/rooms");
    };

    const load = async () => {
        setLoadError("");
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (error: any) {
            setLoadError(error.message || "Unable to load rooms right now.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);
    useEffect(() => { setInventorySearch(searchParams?.get("room") || ""); }, [searchParams]);

    const searchQuery = (searchParams?.get("room") || "").trim().toLowerCase();
    const selectedRoomId = searchParams?.get("selected") || null;

    const rooms = useMemo(
        () => data?.rooms
            ? Object.values(data.rooms)
                .filter((room: any) => room.roomNumber.toLowerCase().includes(searchQuery))
                .sort((a: any, b: any) => a.roomNumber.localeCompare(b.roomNumber, undefined, { numeric: true }))
            : [],
        [data?.rooms, searchQuery]
    );

    const floors = useMemo(() => {
        const grouped = new Map<string, any[]>();
        rooms.forEach((room: any) => {
            const floorKey = `${room.floor || "Unassigned"}`;
            if (!grouped.has(floorKey)) grouped.set(floorKey, []);
            grouped.get(floorKey)?.push(room);
        });
        return Array.from(grouped.entries()).sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }));
    }, [rooms]);

    const selectedRoom = selectedRoomId
        ? (Object.values(data?.rooms || {}).find((room: any) => room.roomNumber === selectedRoomId) as any) || null
        : null;

    const currentRole = data?.currentUser?.role;
    const canCreateRooms = canModuleAction(currentRole, "rooms", "create");

    const validateNewRoom = () => {
        const nextErrors: Record<string, string> = {};
        if (!newRoomNumber.trim()) nextErrors.roomNumber = "Room number is required.";
        if (!newFloor.trim()) nextErrors.floor = "Floor is required.";
        setNewRoomErrors(nextErrors);
        return Object.keys(nextErrors).length === 0;
    };

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    return (
        <div className="space-y-6">
            {loadError && <div className="page-inline-error">{loadError}</div>}

            <BentoCard className="flex items-center gap-4 py-4">
                <span className="mr-4 text-sm font-bold uppercase tracking-wider text-luxury-800/60">Summary</span>
                <StatusPill status="occupied" label={`Occupied ${data.metrics?.occupiedRooms || 0}`} />
                <StatusPill status="vacant" label={`Vacant ${data.metrics?.vacantRooms || 0}`} />
                <StatusPill status="warning" label={`Unbound ${data.metrics?.unboundDevices || 0}`} />
            </BentoCard>

            <BentoCard
                title="Room Inventory"
                eyebrow="Floor-by-floor operational overview"
                actions={canCreateRooms ? (
                    <PillButton primary onClick={() => {
                        setIsAddingRoom(true);
                        setNewRoomNumber("");
                        setNewFloor("");
                        setNewRoomCategory(ROOM_CATEGORY_OPTIONS[0]);
                        setNewClimateControl(CLIMATE_CONTROL_OPTIONS[0]);
                        setNewRoomErrors({});
                    }}>
                        Add Room
                    </PillButton>
                ) : undefined}
            >
                <div className="mb-5 mt-4 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                    <div className="max-w-2xl text-sm text-luxury-800/60">
                        This inventory should answer the operational question first: which rooms exist, which floor they are on, and what state they are in before staff opens the full room profile.
                    </div>
                    <div className="relative w-full lg:w-72">
                        <input
                            type="text"
                            value={inventorySearch}
                            onChange={(event) => {
                                const nextValue = event.target.value;
                                setInventorySearch(nextValue);
                                updateQuery({ room: nextValue.trim() || null });
                            }}
                            placeholder="Search room number..."
                            className="field-input !rounded-full !py-2.5 pl-10 pr-4 w-full shadow-sm"
                        />
                        <svg className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 opacity-40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="11" cy="11" r="8" />
                            <path d="m21 21-4.3-4.3" />
                        </svg>
                    </div>
                </div>

                <div className="space-y-5">
                    {rooms.length === 0 && (
                        <div className="flex min-h-[180px] flex-col items-center justify-center rounded-[24px] border-2 border-dashed border-luxury-200 bg-luxury-50/60 text-center">
                            <div className="text-base font-semibold text-luxury-900">No rooms created yet</div>
                            <div className="mt-2 max-w-md text-sm text-luxury-800/55">Create the first room so staff can bind TVs, manage guest sessions, and organize the hotel floor by floor.</div>
                        </div>
                    )}

                    {floors.map(([floor, floorRooms]) => (
                        <div key={floor} className="rounded-[24px] border border-luxury-200 bg-[linear-gradient(180deg,rgba(255,255,255,0.98),rgba(248,245,238,0.92))] p-4 shadow-[0_16px_44px_-34px_rgba(0,0,0,0.18)]">
                            <div className="mb-4 flex items-center justify-between gap-4">
                                <div>
                                    <div className="text-[11px] font-bold uppercase tracking-[0.24em] text-luxury-800/45">Floor</div>
                                    <div className="text-2xl font-semibold text-luxury-900">{floor}</div>
                                </div>
                                <div className="rounded-full bg-gold-500/10 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-gold-700">
                                    {floorRooms.length} room{floorRooms.length === 1 ? "" : "s"}
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-4">
                                {floorRooms.map((room: any) => (
                                    <button
                                        key={room.roomNumber}
                                        type="button"
                                        onClick={() => updateQuery({ selected: room.roomNumber })}
                                        className="rounded-[18px] border border-luxury-200 bg-white/90 p-4 text-left transition-all hover:border-gold-400 hover:shadow-[0_18px_36px_-30px_rgba(201,168,76,0.65)]"
                                    >
                                        <div className="flex items-start justify-between gap-3">
                                            <div>
                                                <span className="font-mono text-xl font-bold">{room.roomNumber}</span>
                                                <div className="mt-1 text-[10px] font-medium uppercase tracking-wider text-luxury-800/50">{room.status}</div>
                                            </div>
                                            <StatusPill status={roomStatusTone(room.status)} label={room.status || "unknown"} />
                                        </div>
                                        <div className="mt-4 space-y-1">
                                            <div className="text-xs font-semibold text-luxury-900">{roomCategoryBadge(room.roomCategory)}</div>
                                            <div className="text-[11px] text-luxury-800/55">{climateControlBadge(room.climateControl)}</div>
                                            <div className="text-[11px] text-luxury-800/40">{room.guestName || room.deviceId || "No guest / no TV"}</div>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </BentoCard>

            {selectedRoom && (
                <RoomDetailOverlay
                    room={selectedRoom}
                    role={currentRole}
                    onClose={() => updateQuery({ selected: null })}
                    onSave={async (payload) => {
                        try {
                            await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}`, { method: "PATCH", body: JSON.stringify(payload) });
                            notify({ tone: "success", message: `Room ${selectedRoom.roomNumber} updated.` });
                            await load();
                        } catch (error: any) {
                            notify({ tone: "error", message: error.message || "Failed to update room." });
                        }
                    }}
                    onCheckout={() => setCheckoutTarget(selectedRoom)}
                    onUnbind={() => setUnbindTarget(selectedRoom)}
                    onDelete={() => setDeleteTarget(selectedRoom)}
                    onToggleOverride={async () => {
                        try {
                            await api(`/api/admin/rooms/${encodeURIComponent(selectedRoom.roomNumber)}/override`, {
                                method: "POST",
                                body: JSON.stringify({ enabled: !selectedRoom.overrideEnabled, customContentLabel: "" }),
                            });
                            notify({ tone: "success", message: `${selectedRoom.overrideEnabled ? "Disabled" : "Enabled"} override for room ${selectedRoom.roomNumber}.` });
                            await load();
                        } catch (error: any) {
                            notify({ tone: "error", message: error.message || "Failed to change room override." });
                        }
                    }}
                />
            )}

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

            <ConfirmModal open={Boolean(unbindTarget)} title="Unbind TV" description={unbindTarget ? `Remove the current TV binding from room ${unbindTarget.roomNumber}?` : ""} confirmLabel="Unbind TV" onCancel={() => setUnbindTarget(null)} onConfirm={async () => {
                if (!unbindTarget) return;
                try {
                    await api(`/api/admin/rooms/${encodeURIComponent(unbindTarget.roomNumber)}/unbind`, { method: "POST" });
                    notify({ tone: "success", message: `Room ${unbindTarget.roomNumber} is now unbound.` });
                    setUnbindTarget(null);
                    await load();
                } catch (error: any) {
                    notify({ tone: "error", message: error.message || "Failed to unbind room." });
                    setUnbindTarget(null);
                }
            }} />

            <ConfirmModal open={Boolean(deleteTarget)} title="Delete or Archive Room" description={deleteTarget ? `Unused rooms are deleted. Rooms with binding or guest history are archived instead. Continue with room ${deleteTarget.roomNumber}?` : ""} confirmLabel="Continue" onCancel={() => setDeleteTarget(null)} onConfirm={async () => {
                if (!deleteTarget) return;
                try {
                    const result = await api(`/api/admin/rooms/${encodeURIComponent(deleteTarget.roomNumber)}`, { method: "DELETE" });
                    notify({ tone: "success", message: result.archived ? `Archived room ${deleteTarget.roomNumber}.` : `Deleted room ${deleteTarget.roomNumber}.` });
                    setDeleteTarget(null);
                    if (selectedRoomId === deleteTarget.roomNumber) updateQuery({ selected: null });
                    await load();
                } catch (error: any) {
                    notify({ tone: "error", message: error.message || "Failed to remove room." });
                    setDeleteTarget(null);
                }
            }} />

            <AnimatePresence>
                {isAddingRoom && (
                    <>
                        <motion.div className="fixed inset-0 z-40 bg-black/45 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setIsAddingRoom(false)} />
                        <motion.div className="fixed inset-0 z-50 flex items-center justify-center p-4" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 12 }}>
                            <div className="w-full max-w-md" onClick={(event) => event.stopPropagation()}>
                                <BentoCard title="Add New Room" eyebrow="Create room inventory before assignment">
                                    <form className="mt-4 space-y-4" onSubmit={async (event) => {
                                        event.preventDefault();
                                        if (!validateNewRoom()) return;
                                        try {
                                            await api("/api/admin/rooms", { method: "POST", body: JSON.stringify({ roomNumber: newRoomNumber.trim(), floor: newFloor.trim(), roomCategory: newRoomCategory, climateControl: newClimateControl }) });
                                            notify({ tone: "success", message: `Created room ${newRoomNumber.trim()}.` });
                                            setIsAddingRoom(false);
                                            updateQuery({ selected: newRoomNumber.trim() });
                                            await load();
                                        } catch (error: any) {
                                            const fieldErrors = error instanceof ApiError ? error.fieldErrors : {};
                                            setNewRoomErrors(Object.keys(fieldErrors).length ? fieldErrors : { roomNumber: error.message || "Failed to create room." });
                                            notify({ tone: "error", message: error.message || "Failed to create room." });
                                        }
                                    }}>
                                        <div>
                                            <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Room Number</label>
                                            <input autoFocus type="text" value={newRoomNumber} onChange={(event) => setNewRoomNumber(event.target.value)} className="field-input" placeholder="e.g. 101" />
                                            {newRoomErrors.roomNumber && <div className="field-error">{newRoomErrors.roomNumber}</div>}
                                        </div>
                                        <div>
                                            <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Floor</label>
                                            <input type="text" value={newFloor} onChange={(event) => setNewFloor(event.target.value)} className="field-input" placeholder="e.g. 1" />
                                            {newRoomErrors.floor && <div className="field-error">{newRoomErrors.floor}</div>}
                                        </div>
                                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                            <div>
                                                <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Room Category</label>
                                                <select value={newRoomCategory} onChange={(event) => setNewRoomCategory(event.target.value)} className="field-input">
                                                    {ROOM_CATEGORY_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
                                                </select>
                                            </div>
                                            <div>
                                                <label className="mb-1 block text-xs font-bold uppercase tracking-wider text-luxury-800/60">Climate Type</label>
                                                <select value={newClimateControl} onChange={(event) => setNewClimateControl(event.target.value)} className="field-input">
                                                    {CLIMATE_CONTROL_OPTIONS.map((option) => <option key={option} value={option}>{option}</option>)}
                                                </select>
                                            </div>
                                        </div>
                                        <div className="flex justify-end gap-3 pt-3">
                                            <PillButton type="button" onClick={() => setIsAddingRoom(false)}>Cancel</PillButton>
                                            <PillButton primary type="submit">Create Room</PillButton>
                                        </div>
                                    </form>
                                </BentoCard>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </div>
    );
}
