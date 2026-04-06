"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { api } from "../../../lib/api";

export default function UsersPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [editingUser, setEditingUser] = useState<any>(null);
    const [deleteTarget, setDeleteTarget] = useState<any>(null);
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

    const handleSaveUser = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage("");
        const rawForm = Object.fromEntries(new FormData(e.currentTarget as HTMLFormElement));
        const form = Object.fromEntries(
            Object.entries(rawForm).filter(([, value]) => `${value ?? ""}`.trim() !== "")
        );

        try {
            if (form.id) {
                await api(`/api/admin/users/${form.id}`, { method: "PATCH", body: JSON.stringify(form) });
                setMessage(`Updated account for ${form.name || editingUser?.name || "user"}.`);
            } else {
                await api("/api/admin/users", { method: "POST", body: JSON.stringify(form) });
                setMessage(`Created account for ${form.name || "user"}.`);
            }
            setShowForm(false);
            setEditingUser(null);
            await load();
        } catch (error: any) {
            setMessage(error.message || "Failed to save user.");
        }
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="space-y-6 max-w-5xl mx-auto">
            {message && (
                <div className={`rounded-2xl px-4 py-3 text-sm ${message.includes("Failed") ? "bg-red-50 text-red-700" : "bg-green-50 text-green-700"}`}>
                    {message}
                </div>
            )}

            {showForm && (
                <BentoCard title={editingUser ? "Edit Account" : "Create Account"} eyebrow="User Form">
                    <form key={editingUser?.id || "new-user"} onSubmit={handleSaveUser} className="space-y-4 mt-4">
                        <input type="hidden" name="id" value={editingUser?.id || ""} />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Name</label>
                                <input name="name" defaultValue={editingUser?.name} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" required />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Email</label>
                                <input type="email" name="email" defaultValue={editingUser?.email} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" required />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Role</label>
                                <select name="role" defaultValue={editingUser?.role || "RECEPTIONIST"} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50">
                                    <option value="SUPER_ADMIN">Super Admin</option>
                                    <option value="ADMIN">Admin</option>
                                    <option value="RECEPTIONIST">Receptionist</option>
                                </select>
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Status</label>
                                <select name="status" defaultValue={editingUser?.status || "ACTIVE"} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50">
                                    <option value="ACTIVE">Active</option>
                                    <option value="DISABLED">Disabled</option>
                                </select>
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Password</label>
                                <input type="password" name="password" placeholder={editingUser ? "Leave blank to keep unchanged" : "Password"} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" {...(!editingUser ? { required: true } : {})} />
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 pt-4">
                            <PillButton type="button" onClick={() => { setShowForm(false); setEditingUser(null); }}>Cancel</PillButton>
                            <PillButton primary type="submit">{editingUser ? "Update Account" : "Create Account"}</PillButton>
                        </div>
                    </form>
                </BentoCard>
            )}

            <BentoCard
                title="User Management"
                eyebrow="Role Access and Accounts"
                actions={
                    <PillButton primary onClick={() => { setEditingUser(null); setShowForm(true); }}>
                        New Account
                    </PillButton>
                }
            >
                <div className="overflow-x-auto mt-6">
                    <table className="w-full text-left">
                        <thead>
                            <tr className="border-b border-luxury-200">
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">User</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Email</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Role</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Status</th>
                                <th className="pb-3"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.users?.length === 0 && (
                                <tr><td colSpan={5} className="py-4 text-center text-luxury-800/50">No active users found.</td></tr>
                            )}
                            {data.users?.map((u: any) => (
                                <tr key={u.id} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                    <td className="py-4 font-medium text-luxury-900">{u.name}</td>
                                    <td className="py-4 text-sm text-luxury-800/80">{u.email}</td>
                                    <td className="py-4">
                                        <span className="inline-flex items-center rounded-full px-2 py-0.5 border border-luxury-200 text-[10px] font-bold uppercase tracking-wider text-luxury-800 bg-white shadow-sm">
                                            {u.role.replace("_", " ")}
                                        </span>
                                    </td>
                                    <td className="py-4 text-[10px] font-bold uppercase tracking-wide">
                                        {u.status === "ACTIVE" ? <span className="text-green-600">Active</span> : <span className="text-red-500">{u.status}</span>}
                                    </td>
                                    <td className="py-4 text-right">
                                        <div className="flex justify-end gap-2">
                                            <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => { setEditingUser(u); setShowForm(true); }}>Edit</PillButton>
                                            <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => setDeleteTarget(u)}>Delete</PillButton>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </BentoCard>

            <ConfirmModal
                open={Boolean(deleteTarget)}
                title="Archive User"
                description={deleteTarget ? `Archive the account for ${deleteTarget.name}? Archived users can no longer sign in.` : ""}
                confirmLabel="Archive User"
                onCancel={() => setDeleteTarget(null)}
                onConfirm={async () => {
                    if (!deleteTarget) return;
                    try {
                        await api(`/api/admin/users/${deleteTarget.id}`, { method: "DELETE" });
                        setMessage(`Archived account for ${deleteTarget.name}.`);
                        setDeleteTarget(null);
                        if (editingUser?.id === deleteTarget.id) {
                            setEditingUser(null);
                            setShowForm(false);
                        }
                        await load();
                    } catch (error: any) {
                        setMessage(error.message || "Failed to archive user.");
                        setDeleteTarget(null);
                    }
                }}
            />
        </div>
    );
}
