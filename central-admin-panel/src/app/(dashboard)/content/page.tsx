"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";

export default function ContentPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);

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

    const handleUpload = async (e: React.FormEvent, endpoint: string) => {
        e.preventDefault();
        const fd = new FormData(e.currentTarget as HTMLFormElement);
        await api(endpoint, { method: "POST", body: fd });
        load();
    };

    const handleConfigUpdate = async (e: React.FormEvent) => {
        e.preventDefault();
        const form = Object.fromEntries(new FormData(e.currentTarget as HTMLFormElement));
        await api("/api/admin/config", {
            method: "POST",
            body: JSON.stringify({
                hotel: { hotelName: form.hotelName },
                popup: { helpTitle: form.helpTitle, callNumber: form.callNumber, ratingText: form.ratingText }
            })
        });
        load();
    };

    const toggleDestination = async (key: string) => {
        const visibility = structuredClone(data.visibility);
        visibility.destinations[key] = !visibility.destinations[key];
        await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) });
        load();
    }

    if (loading) return <div>Loading...</div>;

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <BentoCard title="Hotel Logo" eyebrow="Brand Asset">
                    <form className="mt-4 space-y-4" onSubmit={e => handleUpload(e, "/api/admin/upload?kind=startup")}>
                        <div className="border-2 border-dashed border-luxury-200 rounded-2xl p-6 text-center hover:bg-luxury-50 transition-colors">
                            <input type="file" name="file" accept="image/*" className="text-sm text-luxury-800" required />
                        </div>
                        <PillButton primary type="submit" className="w-full">Upload Logo</PillButton>
                    </form>
                </BentoCard>

                <BentoCard title="Startup Animation" eyebrow="Arrival Branding">
                    <form className="mt-4 space-y-4" onSubmit={e => handleUpload(e, "/api/admin/upload?kind=startup")}>
                        <div className="border-2 border-dashed border-luxury-200 rounded-2xl p-6 text-center hover:bg-luxury-50 transition-colors">
                            <input type="file" name="file" accept="video/mp4,image/*" className="text-sm text-luxury-800" required />
                        </div>
                        <PillButton primary type="submit" className="w-full">Upload Asset</PillButton>
                    </form>
                </BentoCard>

                <BentoCard title="Background Slideshow" eyebrow="Ambient Media">
                    <form className="mt-4 space-y-4" onSubmit={e => handleUpload(e, "/api/admin/upload?kind=background")}>
                        <select name="bucket" className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-gold-500/50">
                            <option value="home">Home Screen</option>
                            <option value="roomService">Room Service</option>
                            <option value="foodMenu">Food Menu</option>
                            <option value="inputs">TV Inputs</option>
                        </select>
                        <div className="border-2 border-dashed border-luxury-200 rounded-2xl p-6 text-center hover:bg-luxury-50 transition-colors">
                            <input type="file" name="file" accept="image/*" className="text-sm text-luxury-800" required />
                        </div>
                        <PillButton primary type="submit" className="w-full">Upload Background</PillButton>
                    </form>
                </BentoCard>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BentoCard title="Tile Visibility" eyebrow="Launcher Destinations">
                    <div className="mt-4 space-y-2">
                        {["home", "roomService", "foodMenu", "inputs"].map(key => {
                            const isOn = data.visibility.destinations[key];
                            return (
                                <div key={key} className="flex justify-between items-center p-4 border border-luxury-100 rounded-xl bg-luxury-50 hover:bg-luxury-100 transition-colors">
                                    <div>
                                        <div className="font-medium text-luxury-900 capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</div>
                                        <div className="text-xs text-luxury-800/60">Destination tile visible on TV</div>
                                    </div>
                                    <button
                                        onClick={() => toggleDestination(key)}
                                        className={`w-12 h-6 rounded-full transition-colors relative ${isOn ? 'bg-gold-500' : 'bg-luxury-200'}`}
                                    >
                                        <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-all ${isOn ? 'left-7' : 'left-1'}`} />
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                </BentoCard>

                <BentoCard title="Popup and Branding" eyebrow="Guest-Facing Copy">
                    <form onSubmit={handleConfigUpdate} className="space-y-4 mt-4">
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Hotel Name</label>
                            <input name="hotelName" defaultValue={data.hotel?.hotelName} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                        </div>
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Help Title</label>
                            <input name="helpTitle" defaultValue={data.popup?.helpTitle} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Call Number</label>
                                <input name="callNumber" defaultValue={data.popup?.callNumber} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Rating Prompt Text</label>
                                <input name="ratingText" defaultValue={data.popup?.ratingText} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                            </div>
                        </div>
                        <div className="flex justify-end pt-4">
                            <PillButton primary type="submit">Push Changes to TVs</PillButton>
                        </div>
                    </form>
                </BentoCard>
            </div>
        </div>
    );
}
