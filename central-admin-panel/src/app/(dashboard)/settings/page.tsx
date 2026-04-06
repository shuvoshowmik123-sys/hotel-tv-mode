"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";

export default function SettingsPage() {
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

    const handleSaveProperty = async (e: React.FormEvent) => {
        e.preventDefault();
        const form = Object.fromEntries(new FormData(e.currentTarget as HTMLFormElement));
        await api("/api/admin/config", {
            method: "POST",
            body: JSON.stringify({
                property: { name: form.propertyName, address: form.address, timezone: form.timezone }
            })
        });
        load();
    };

    if (loading) return <div>Loading...</div>;

    const s = data.settings;
    if (!s) return <div className="p-4 bg-red-50 text-red-600 rounded-xl">You do not have permission to view settings.</div>;

    return (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <BentoCard title="Property Management" eyebrow="Hotel Profile">
                <form onSubmit={handleSaveProperty} className="space-y-4 mt-4">
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Property Name</label>
                        <input name="propertyName" defaultValue={s.property.name} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Address</label>
                        <input name="address" defaultValue={s.property.address} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                    </div>
                    <div>
                        <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Timezone</label>
                        <input name="timezone" defaultValue={s.property.timezone} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" />
                    </div>
                    <div className="flex justify-end pt-4">
                        <PillButton primary type="submit">Save Property</PillButton>
                    </div>
                </form>
            </BentoCard>

            <div className="space-y-6">
                <BentoCard title="API and Integration" eyebrow="Connectivity">
                    <div className="mt-4 space-y-4">
                        <div className="flex justify-between items-center py-3 border-b border-luxury-100">
                            <span className="text-sm font-bold uppercase tracking-wider text-luxury-800/60">API Key</span>
                            <span className="font-mono text-sm text-luxury-900 bg-luxury-100 px-3 py-1 rounded-md">{s.integration.apiKeyPreview}</span>
                        </div>
                        <div className="flex justify-between items-center py-3 border-b border-luxury-100">
                            <span className="text-sm font-bold uppercase tracking-wider text-luxury-800/60">Webhook URL</span>
                            <span className="font-mono text-xs text-luxury-800/60 truncate max-w-[200px]">{s.integration.webhookUrl}</span>
                        </div>
                        <div className="flex justify-between items-center py-3">
                            <span className="text-sm font-bold uppercase tracking-wider text-luxury-800/60">Environment</span>
                            <span className="status-pill-occupied px-3 py-1 rounded-full text-xs font-bold tracking-wider uppercase">{s.subscription.environment}</span>
                        </div>
                    </div>
                </BentoCard>

                <BentoCard title="Subscription" eyebrow="Deployment Info">
                    <div className="mt-4 space-y-4 text-sm font-medium">
                        <div className="flex justify-between pt-2">
                            <span className="text-luxury-800/60">Current Plan</span>
                            <span className="text-luxury-900">{s.subscription.plan}</span>
                        </div>
                        <div className="flex justify-between">
                            <span className="text-luxury-800/60">Renewal Date</span>
                            <span className="font-mono text-luxury-800">{s.subscription.renewalDate}</span>
                        </div>
                    </div>
                </BentoCard>
            </div>
        </div>
    );
}
