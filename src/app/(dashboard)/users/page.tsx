"use client";

import React, { useEffect, useMemo, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { useFeedback } from "../../../components/FeedbackProvider";
import { PillButton } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { api } from "../../../lib/api";
import { getPermissionEntries, permissionLabel, UserRole } from "../../../lib/permissions";

const ROLE_DESCRIPTIONS: Record<UserRole, string> = {
    SUPER_ADMIN: "Full system control, including user management, settings, audit, and every hotel operation.",
    ADMIN: "Hotel operations manager access for rooms, sessions, content, menus, policies, and device binding.",
    RECEPTIONIST: "Front-desk access for bindings, guest sessions, and viewing room status without structural room changes.",
};

function accessSummary(role?: string) {
    const visibleModules = getPermissionEntries(role).filter((entry) => entry.visible);
    return visibleModules.map((entry) => entry.label);
}

export default function UsersPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [editingUser, setEditingUser] = useState<any>(null);
    const [deleteTarget, setDeleteTarget] = useState<any>(null);
    const [loadError, setLoadError] = useState("");
    const [formRole, setFormRole] = useState<UserRole>("RECEPTIONIST");
    const { notify } = useFeedback();

    const load = async () => {
        setLoadError("");
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (err: any) {
            setLoadError(err.message || "Unable to load user accounts.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const openCreate = () => {
        setEditingUser(null);
        setFormRole("RECEPTIONIST");
        setShowForm(true);
    };

    const openEdit = (user: any) => {
        setEditingUser(user);
        setFormRole((user?.role || "RECEPTIONIST") as UserRole);
        setShowForm(true);
    };

    const handleSaveUser = async (e: React.FormEvent) => {
        e.preventDefault();
        const rawForm = Object.fromEntries(new FormData(e.currentTarget as HTMLFormElement));
        const form = Object.fromEntries(
            Object.entries(rawForm).filter(([, value]) => `${value ?? ""}`.trim() !== "")
        );

        try {
            if (form.id) {
                await api(`/api/admin/users/${form.id}`, { method: "PATCH", body: JSON.stringify(form) });
                notify({ tone: "success", message: `Updated account for ${form.name || editingUser?.name || "user"}.` });
            } else {
                await api("/api/admin/users", { method: "POST", body: JSON.stringify(form) });
                notify({ tone: "success", message: `Created account for ${form.name || "user"}.` });
            }
            setShowForm(false);
            setEditingUser(null);
            await load();
        } catch (error: any) {
            notify({ tone: "error", message: error.message || "Failed to save user." });
        }
    };

    const permissionEntries = useMemo(
        () => getPermissionEntries(formRole).filter((entry) => entry.visible || entry.allowedActions.length > 0),
        [formRole]
    );

    if (loading) return <div>Loading...</div>;

    return (
        <div className="space-y-6 max-w-6xl mx-auto">
            {loadError && <div className="page-inline-error">{loadError}</div>}

            {showForm && (
                <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1.25fr)_minmax(360px,0.95fr)] gap-6">
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
                                    <select
                                        name="role"
                                        value={formRole}
                                        onChange={(event) => setFormRole(event.target.value as UserRole)}
                                        className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50"
                                    >
                                        <option value="SUPER_ADMIN">Super Admin</option>
                                        <option value="ADMIN">Admin</option>
                                        <option value="RECEPTIONIST">Receptionist</option>
                                    </select>
                                    <div className="mt-2 text-xs text-luxury-800/55">
                                        {ROLE_DESCRIPTIONS[formRole]}
                                    </div>
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
                            <div className="rounded-2xl border border-luxury-100 bg-luxury-50/70 px-4 py-3 text-sm text-luxury-800/60">
                                Role templates are enforced by the backend. This form shows exactly what each role can access and do across the panel.
                            </div>
                            <div className="flex justify-end gap-3 pt-4">
                                <PillButton type="button" onClick={() => { setShowForm(false); setEditingUser(null); }}>Cancel</PillButton>
                                <PillButton primary type="submit">{editingUser ? "Update Account" : "Create Account"}</PillButton>
                            </div>
                        </form>
                    </BentoCard>

                    <BentoCard title="Role Permissions" eyebrow="Tab Visibility and Actions">
                        <div className="mt-4 space-y-4">
                            <div className="rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4">
                                <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50">Current Template</div>
                                <div className="mt-1 text-lg font-semibold text-luxury-900">{formRole.replace("_", " ")}</div>
                                <div className="mt-2 text-sm text-luxury-800/60">
                                    {permissionEntries.filter((entry) => entry.visible).length} visible tabs with role-based actions across the panel.
                                </div>
                            </div>

                            <div className="space-y-3 max-h-[640px] overflow-y-auto pr-1">
                                {permissionEntries.map((entry) => (
                                    <div key={entry.key} className="rounded-2xl border border-luxury-100 bg-white p-4 shadow-sm">
                                        <div className="flex items-start justify-between gap-3">
                                            <div>
                                                <div className="font-semibold text-luxury-900">{entry.label}</div>
                                                <div className="text-sm text-luxury-800/55 mt-1">{entry.description}</div>
                                            </div>
                                            <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider border ${entry.visible ? "bg-green-50 text-green-700 border-green-200" : "bg-luxury-100 text-luxury-800 border-luxury-200"}`}>
                                                {entry.visible ? "Visible tab" : "Hidden tab"}
                                            </span>
                                        </div>
                                        <div className="mt-3 flex flex-wrap gap-2">
                                            {entry.allowedActions.length > 0 ? (
                                                entry.allowedActions.map((action) => (
                                                    <span key={action} className="inline-flex items-center rounded-full px-3 py-1 text-[11px] font-bold tracking-wide uppercase bg-gold-50 text-gold-700 border border-gold-200">
                                                        {permissionLabel(entry.key, action)}
                                                    </span>
                                                ))
                                            ) : (
                                                <span className="text-sm text-luxury-800/45">No access for this role.</span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </BentoCard>
                </div>
            )}

            <BentoCard
                title="User Management"
                eyebrow="Role Access and Accounts"
                actions={
                    <PillButton primary onClick={openCreate}>
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
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Access</th>
                                <th className="pb-3 text-xs font-bold uppercase tracking-wider text-luxury-800/60">Status</th>
                                <th className="pb-3"></th>
                            </tr>
                        </thead>
                        <tbody>
                            {data.users?.length === 0 && (
                                <tr><td colSpan={6} className="py-4 text-center text-luxury-800/50">No active users found.</td></tr>
                            )}
                            {data.users?.map((u: any) => {
                                const visibleModules = accessSummary(u.role);
                                return (
                                    <tr key={u.id} className="border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors">
                                        <td className="py-4 font-medium text-luxury-900">{u.name}</td>
                                        <td className="py-4 text-sm text-luxury-800/80">{u.email}</td>
                                        <td className="py-4">
                                            <span className="inline-flex items-center rounded-full px-2 py-0.5 border border-luxury-200 text-[10px] font-bold uppercase tracking-wider text-luxury-800 bg-white shadow-sm">
                                                {u.role.replace("_", " ")}
                                            </span>
                                        </td>
                                        <td className="py-4 text-sm text-luxury-800/60">
                                            <div className="max-w-[260px]">
                                                {visibleModules.slice(0, 3).join(", ") || "No access"}
                                                {visibleModules.length > 3 ? ` +${visibleModules.length - 3} more` : ""}
                                            </div>
                                        </td>
                                        <td className="py-4 text-[10px] font-bold uppercase tracking-wide">
                                            {u.status === "ACTIVE" ? <span className="text-green-600">Active</span> : <span className="text-red-500">{u.status}</span>}
                                        </td>
                                        <td className="py-4 text-right">
                                            <div className="flex justify-end gap-2">
                                                <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => openEdit(u)}>Edit</PillButton>
                                                <PillButton className="!py-1.5 !px-4 !text-xs" onClick={() => setDeleteTarget(u)}>Archive</PillButton>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
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
                        notify({ tone: "success", message: `Archived account for ${deleteTarget.name}.` });
                        setDeleteTarget(null);
                        if (editingUser?.id === deleteTarget.id) {
                            setEditingUser(null);
                            setShowForm(false);
                        }
                        await load();
                    } catch (error: any) {
                        notify({ tone: "error", message: error.message || "Failed to archive user." });
                        setDeleteTarget(null);
                    }
                }}
            />
        </div>
    );
}
