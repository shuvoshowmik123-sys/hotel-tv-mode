"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";

export default function BindingPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [activationCode, setActivationCode] = useState("");
    const [roomNumber, setRoomNumber] = useState("");
    const [guestName, setGuestName] = useState("");
    const [welcomeNote, setWelcomeNote] = useState("");
    const [isBinding, setIsBinding] = useState(false);
    const [message, setMessage] = useState("");

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

    const handleBind = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsBinding(true);
        setMessage("");
        try {
            await api("/api/admin/bind", {
                method: "POST",
                body: JSON.stringify({ activationCode, roomNumber, guestName, welcomeNote })
            });
            setMessage("Binding confirmed successfully.");
            setActivationCode("");
            setRoomNumber("");
            setGuestName("");
            setWelcomeNote("");
            load();
        } catch (err: any) {
            setMessage(err.message || "Failed to bind");
        } finally {
            setIsBinding(false);
        }
    };

    if (loading) return <div className="text-luxury-800">Loading...</div>;

    const pendingItems = data?.pendingActivations ? Object.values(data.pendingActivations) : [];

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <BentoCard title="Pending Bindings" eyebrow="Activation Queue">
                {pendingItems.length === 0 ? (
                    <div className="text-luxury-800/60 text-sm mt-4">No pending activation codes right now.</div>
                ) : (
                    <div className="space-y-3 mt-4">
                        {pendingItems.map((item: any, i) => (
                            <div key={i} className="flex justify-between items-center p-3 rounded-xl border border-luxury-100 bg-luxury-50">
                                <div>
                                    <div className="font-mono text-gold-600 font-bold tracking-wider">{item.activationCode}</div>
                                    <div className="text-xs font-mono text-luxury-800/50 mt-1">{item.macAddress || item.deviceId}</div>
                                </div>
                                <div className="flex items-center gap-4">
                                    <span className="text-xs text-luxury-800/60 font-medium">{new Date(item.createdAt).toLocaleTimeString()}</span>
                                    <PillButton primary type="button" onClick={() => setActivationCode(item.activationCode)}>
                                        Bind
                                    </PillButton>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </BentoCard>

            <BentoCard title="Binding Form" eyebrow="Assign Room and Guest">
                <form onSubmit={handleBind} className="space-y-4 mt-4">
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Activation Code</label>
                        <input
                            value={activationCode}
                            onChange={e => setActivationCode(e.target.value)}
                            className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                            placeholder="e.g. 1Q2W3E"
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Number</label>
                        <input
                            value={roomNumber}
                            onChange={e => setRoomNumber(e.target.value)}
                            className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                            placeholder="101"
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name (Optional)</label>
                        <input
                            value={guestName}
                            onChange={e => setGuestName(e.target.value)}
                            className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                            placeholder="Mr. Smith"
                        />
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Welcome Note (Optional)</label>
                        <textarea
                            rows={3}
                            value={welcomeNote}
                            onChange={e => setWelcomeNote(e.target.value)}
                            className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors resize-none"
                            placeholder="Welcome to Asteria Grand..."
                        />
                    </div>
                    {message && (
                        <div className={`text-sm p-3 rounded-lg ${message.includes("success") ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                            {message}
                        </div>
                    )}
                    <div className="flex justify-end pt-2">
                        <PillButton primary type="submit" disabled={isBinding}>
                            {isBinding ? "Binding..." : "Confirm Binding"}
                        </PillButton>
                    </div>
                </form>
            </BentoCard>
        </div>
    );
}
