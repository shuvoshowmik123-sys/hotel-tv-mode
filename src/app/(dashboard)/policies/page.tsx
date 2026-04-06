"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";

export default function PoliciesPage() {
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

    const togglePolicy = async (type: "visibleAppPackages" | "visibleSourceTitles", key: string) => {
        const visibility = structuredClone(data.visibility);
        const list = visibility[type];
        visibility[type] = list.includes(key) ? list.filter((v: string) => v !== key) : [...list, key];
        await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) });
        load();
    };

    if (loading) return <div>Loading...</div>;

    const apps = data.availableApps.map((app: any) => ({
        key: app.packageName, name: app.name, description: app.description,
        on: data.visibility.visibleAppPackages.includes(app.packageName)
    }));

    const inputs = data.availableInputs.map((input: any) => ({
        key: input.title, name: input.title, description: input.description,
        on: data.visibility.visibleSourceTitles.includes(input.title)
    }));

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <BentoCard title="App Visibility" eyebrow="Installed App Policy">
                <div className="mt-4 space-y-2">
                    {apps.map((item: any) => (
                        <div key={item.key} className="flex justify-between items-center p-4 border border-luxury-100 rounded-xl bg-luxury-50 hover:bg-luxury-100 transition-colors">
                            <div>
                                <div className="font-medium text-luxury-900">{item.name}</div>
                                <div className="text-xs text-luxury-800/60 mt-0.5">{item.description}</div>
                                <div className="text-[10px] uppercase font-mono tracking-wider text-luxury-800/40 mt-1">{item.key}</div>
                            </div>
                            <button
                                onClick={() => togglePolicy("visibleAppPackages", item.key)}
                                className={`w-12 h-6 rounded-full transition-colors relative shadow-inner ${item.on ? 'bg-gold-500' : 'bg-luxury-200'}`}
                            >
                                <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-all shadow-sm ${item.on ? 'left-7' : 'left-1'}`} />
                            </button>
                        </div>
                    ))}
                </div>
            </BentoCard>

            <div className="space-y-6">
                <BentoCard title="Input Source Visibility" eyebrow="TV Source Policy">
                    <div className="mt-4 space-y-2">
                        {inputs.map((item: any) => (
                            <div key={item.key} className="flex justify-between items-center p-4 border border-luxury-100 rounded-xl bg-luxury-50 hover:bg-luxury-100 transition-colors">
                                <div>
                                    <div className="font-medium text-luxury-900">{item.name}</div>
                                    <div className="text-xs text-luxury-800/60 mt-0.5">{item.description}</div>
                                </div>
                                <button
                                    onClick={() => togglePolicy("visibleSourceTitles", item.key)}
                                    className={`w-12 h-6 rounded-full transition-colors relative shadow-inner ${item.on ? 'bg-gold-500' : 'bg-luxury-200'}`}
                                >
                                    <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-all shadow-sm ${item.on ? 'left-7' : 'left-1'}`} />
                                </button>
                            </div>
                        ))}
                    </div>
                </BentoCard>

                <BentoCard title="Tile Priority" eyebrow="Display Order">
                    <div className="mt-4 space-y-2">
                        {data.visibility.visibleSourceTitles.map((title: string, i: number) => (
                            <div key={title} className="flex justify-between items-center p-3 border border-luxury-100 rounded-lg bg-white shadow-sm">
                                <div className="flex items-center gap-3">
                                    <div className="w-6 h-6 rounded-full bg-luxury-100 flex items-center justify-center text-xs font-bold text-luxury-800/60">{i + 1}</div>
                                    <span className="font-mono text-luxury-900 text-sm">{title}</span>
                                </div>
                                <svg className="w-4 h-4 text-luxury-800/40 cursor-move" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="9" cy="12" r="1" /><circle cx="9" cy="5" r="1" /><circle cx="9" cy="19" r="1" /><circle cx="15" cy="12" r="1" /><circle cx="15" cy="5" r="1" /><circle cx="15" cy="19" r="1" /></svg>
                            </div>
                        ))}
                    </div>
                </BentoCard>
            </div>
        </div>
    );
}
