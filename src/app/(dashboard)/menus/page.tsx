"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton, StatusPill } from "../../../components/UIElements";
import { api } from "../../../lib/api";

type Category = "breakfast" | "lunch" | "dinner" | "beverages";

export default function MenusPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [tab, setTab] = useState<Category>("breakfast");
    const [editingItem, setEditingItem] = useState<any>(null);

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

    const handleSaveItem = async (e: React.FormEvent) => {
        e.preventDefault();
        const fd = new FormData(e.currentTarget as HTMLFormElement);
        const form = Object.fromEntries(fd);

        const meals = structuredClone(data.meals);
        const current = meals[tab] || [];

        const item = {
            id: form.id || `menu-${Date.now()}`,
            title: form.title,
            subtitle: form.subtitle,
            description: form.description,
            price: form.price,
            available: form.available === "true",
            badge: "NEW",
            accentColor: "#C9A84C"
        };

        if (form.id) {
            meals[tab] = current.map((entry: any) => entry.id === form.id ? item : entry);
        } else {
            meals[tab] = [...current, item];
        }

        await api("/api/admin/config", { method: "POST", body: JSON.stringify({ meals }) });
        setEditingItem(null);
        load();
    };

    if (loading) return <div>Loading...</div>;

    const items = data.meals?.[tab] || [];

    return (
        <div className="grid grid-cols-1 xl:grid-cols-[2fr_1fr] gap-6">
            <BentoCard
                title="Menu Catalog"
                eyebrow="Breakfast, Lunch, Dinner, and Beverages"
                actions={
                    <PillButton primary onClick={() => setEditingItem({ available: true })}>Add New Item</PillButton>
                }
            >
                <div className="flex gap-2 mb-6 mt-4">
                    {(["breakfast", "lunch", "dinner", "beverages"] as Category[]).map(t => (
                        <button
                            key={t}
                            onClick={() => { setTab(t); setEditingItem(null); }}
                            className={`px-4 py-2 rounded-full text-sm font-bold tracking-wide uppercase transition-colors ${tab === t ? "bg-luxury-800 text-white" : "bg-luxury-100 text-luxury-800 hover:bg-luxury-200"
                                }`}
                        >
                            {t}
                        </button>
                    ))}
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left">
                        <thead>
                            <tr className="border-b border-luxury-200">
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 w-1/3">Item Details</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Description</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60 w-24">Price</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Status</th>
                                <th className="pb-3"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {items.length === 0 && (
                                <tr>
                                    <td colSpan={5} className="py-8 text-center text-luxury-800/50">No menu items in this category.</td>
                                </tr>
                            )}
                            {items.map((item: any) => (
                                <tr key={item.id} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                    <td className="py-4">
                                        <div className="font-bold text-luxury-900">{item.title}</div>
                                        {item.subtitle && <div className="text-xs text-luxury-800/60 font-medium mt-1">{item.subtitle}</div>}
                                    </td>
                                    <td className="py-4 text-sm text-luxury-800 pr-4">{item.description || "-"}</td>
                                    <td className="py-4 font-mono font-bold text-gold-500">{item.price}</td>
                                    <td className="py-4"><StatusPill status={item.available ? "occupied" : "vacant"} label={item.available ? "Available" : "Unavailable"} /></td>
                                    <td className="py-4 text-right">
                                        <PillButton onClick={() => setEditingItem(item)} className="!py-1.5 !px-4 !text-xs">Edit</PillButton>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </BentoCard>

            <div className="space-y-6">
                <BentoCard title={editingItem?.id ? "Edit Item" : "Add Menu Item"} eyebrow="Menu Form">
                    {editingItem ? (
                        <form onSubmit={handleSaveItem} className="space-y-4 mt-4">
                            <input type="hidden" name="id" value={editingItem.id || ""} />
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Item Title</label>
                                <input name="title" defaultValue={editingItem.title} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-gold-500/50" required />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Subtitle</label>
                                <input name="subtitle" defaultValue={editingItem.subtitle} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-gold-500/50" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Description</label>
                                <textarea name="description" defaultValue={editingItem.description} rows={3} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-gold-500/50 resize-none" />
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Price</label>
                                    <input name="price" defaultValue={editingItem.price} placeholder="$10" className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2.5 text-sm font-mono focus:ring-2 focus:ring-gold-500/50" />
                                </div>
                                <div>
                                    <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Status</label>
                                    <select name="available" defaultValue={String(editingItem.available)} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50">
                                        <option value="true">Available</option>
                                        <option value="false">Unavailable</option>
                                    </select>
                                </div>
                            </div>
                            <div className="flex gap-3 justify-end pt-4">
                                <PillButton type="button" onClick={() => setEditingItem(null)}>Cancel</PillButton>
                                <PillButton primary type="submit">Save Item</PillButton>
                            </div>
                        </form>
                    ) : (
                        <div className="text-luxury-800/50 h-32 flex justify-center items-center rounded-xl border-2 border-dashed border-luxury-200 mt-4 text-sm font-medium">
                            Click Add Item or Edit to start
                        </div>
                    )}
                </BentoCard>
            </div>
        </div>
    );
}
