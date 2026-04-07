"use client";

import React, { useEffect, useState, useRef } from "react";
import { motion, animate } from "motion/react";
import { BentoCard } from "../../../components/BentoCard";
import { useFeedback } from "../../../components/FeedbackProvider";
import { SkeletonStatCard, SkeletonCard } from "../../../components/SkeletonCard";
import { api } from "../../../lib/api";
import { PillButton } from "../../../components/UIElements";

function AnimatedNumber({ value }: { value: number }) {
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const controls = animate(0, value, {
            duration: 1.4,
            ease: [0.22, 1, 0.36, 1],
            onUpdate(v) {
                if (ref.current) {
                    ref.current.textContent = Math.floor(v).toString();
                }
            },
        });
        return controls.stop;
    }, [value]);

    return <div ref={ref}>0</div>;
}

function StatCard({ label, value, subtext, delay }: { label: string; value: number | string; subtext: string; delay: number }) {
    return (
        <BentoCard delay={delay}>
            <div className="bento-card-eyebrow">{label}</div>
            <div className="mt-3 mb-2" style={{ fontSize: 42, lineHeight: 1, fontFamily: "var(--font-mono)", color: "#C9A84C", fontWeight: 700 }}>
                {typeof value === "number" ? <AnimatedNumber value={value} /> : value}
            </div>
            <div className="text-sm font-medium" style={{ color: "rgba(62,59,51,0.6)" }}>{subtext}</div>
        </BentoCard>
    );
}

export default function DashboardPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [quickRoomNumber, setQuickRoomNumber] = useState("");
    const [quickGuestName, setQuickGuestName] = useState("");
    const { notify } = useFeedback();

    useEffect(() => {
        api("/api/admin/state").then(setData).catch(console.error).finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {[0, 1, 2, 3].map(i => <SkeletonStatCard key={i} />)}
                </div>
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    <div className="lg:col-span-2"><SkeletonCard rows={5} /></div>
                    <div className="space-y-6"><SkeletonCard rows={3} /></div>
                </div>
            </div>
        );
    }

    if (!data?.metrics) {
        return (
            <BentoCard>
                <div style={{ color: "#9a3412" }}>Error loading dashboard. Verify the admin panel API is running and reachable.</div>
            </BentoCard>
        );
    }

    const { metrics, auditLogs, systemHealth, currentUser, pendingActivations, notifications, workflowSteps } = data;
    const activityFeed = auditLogs?.length ? auditLogs : notifications || [];

    const stats = [
        { label: "TVs Online", value: metrics.onlineTvs, subtext: "Current property sync" },
        { label: "Rooms Occupied", value: metrics.occupiedRooms, subtext: "Live guest sessions" },
        { label: "Pending Bindings", value: metrics.pendingBindings, subtext: "Waiting activation codes" },
        { label: "Unbound Devices", value: metrics.unboundDevices, subtext: "Needs front desk action" },
    ];

    const receptionistQuickCheckIn = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            await api(`/api/admin/rooms/${encodeURIComponent(quickRoomNumber.trim())}/checkin`, {
                method: "POST",
                body: JSON.stringify({
                    guestName: quickGuestName,
                }),
            });
            notify({ tone: "success", message: "Guest session saved." });
            setQuickRoomNumber("");
            setQuickGuestName("");
            const nextState = await api("/api/admin/state");
            setData(nextState);
        } catch (error: any) {
            notify({ tone: "error", message: error.message || "Unable to save guest session." });
        }
    };

    if (currentUser?.role === "RECEPTIONIST") {
        const pendingItems = pendingActivations ? Object.values(pendingActivations) : [];
        return (
            <div className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {stats.map((s, i) => (
                        <StatCard key={s.label} {...s} delay={i * 0.07} />
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                    <BentoCard title="Pending Device Activation" eyebrow="Front Desk Queue">
                        <div className="space-y-3 mt-4">
                            {pendingItems.length === 0 ? (
                                <div className="text-sm py-4" style={{ color: "rgba(62,59,51,0.4)" }}>No pending activation codes.</div>
                            ) : (
                                pendingItems.map((item: any) => (
                                    <div key={item.pollToken} className="flex items-center justify-between p-4 rounded-2xl border border-luxury-100 bg-luxury-50">
                                        <div>
                                            <div className="font-mono font-bold tracking-widest text-gold-600">{item.activationCode}</div>
                                            <div className="text-xs font-mono text-luxury-800/50 mt-1">{item.macAddress || item.deviceId}</div>
                                        </div>
                                        <div className="text-xs font-medium text-luxury-800/50">
                                            {new Date(item.createdAt).toLocaleTimeString()}
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </BentoCard>

                    <BentoCard title="Quick Guest Session" eyebrow="Reception Check-In">
                        <form onSubmit={receptionistQuickCheckIn} className="space-y-4 mt-4">
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Room Number</label>
                                <input value={quickRoomNumber} onChange={(event) => setQuickRoomNumber(event.target.value)} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" placeholder="101" required />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Guest Name</label>
                                <input value={quickGuestName} onChange={(event) => setQuickGuestName(event.target.value)} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50" placeholder="Guest Name" />
                            </div>
                            <div className="flex justify-end pt-2">
                                <PillButton primary type="submit">Check In</PillButton>
                            </div>
                            <div className="text-xs text-luxury-800/50">
                                The room must already exist before reception can start a guest session.
                            </div>
                        </form>
                    </BentoCard>
                </div>

                <BentoCard title="Room Status Summary" eyebrow="Operations Snapshot">
                    <div className="mt-4 flex flex-wrap gap-3">
                        <span className="status-pill status-pill-occupied">Occupied {metrics.occupiedRooms}</span>
                        <span className="status-pill status-pill-vacant">Vacant {metrics.vacantRooms}</span>
                        <span className="status-pill status-pill-warning">Offline {metrics.offlineRooms}</span>
                    </div>
                </BentoCard>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {stats.map((s, i) => (
                    <StatCard key={s.label} {...s} delay={i * 0.07} />
                ))}
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <BentoCard title="Live Activity Feed" eyebrow="Recent Hotel Operations" className="lg:col-span-2" delay={0.28}>
                    <div className="space-y-1 max-h-96 overflow-y-auto pr-1 mt-4">
                        {(!activityFeed || activityFeed.length === 0) && (
                            <div className="text-sm py-4" style={{ color: "rgba(62,59,51,0.4)" }}>No recent activity.</div>
                        )}
                        {activityFeed?.map((log: any, i: number) => (
                            <motion.div
                                key={i}
                                initial={{ opacity: 0, x: -8 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: 0.35 + i * 0.035, duration: 0.3 }}
                                className="flex items-center justify-between py-3 border-b"
                                style={{ borderColor: "rgba(229,225,216,0.6)" }}
                            >
                                <div className="flex items-center gap-3">
                                    <span
                                        className="role-pill"
                                        style={{ fontSize: 10, background: i === 0 ? "rgba(201,168,76,0.08)" : undefined, borderColor: i === 0 ? "rgba(201,168,76,0.3)" : undefined, color: i === 0 ? "#AB8B39" : undefined }}
                                    >
                                        {log.actorName || "System"}
                                    </span>
                                    <span className="text-sm font-medium" style={{ color: "#292620" }}>{log.action || log.message}</span>
                                </div>
                                <div className="font-mono text-xs" style={{ color: "rgba(62,59,51,0.4)" }}>
                                    {new Date(log.createdAt).toLocaleTimeString()}
                                </div>
                            </motion.div>
                        ))}
                    </div>
                </BentoCard>

                <div className="space-y-6">
                    <BentoCard title="Receptionist Workflow" eyebrow="Current Process" delay={0.32}>
                        <div className="mt-4 space-y-4">
                            {(workflowSteps || []).map((step: any) => (
                                <div key={step.id} className="flex items-center justify-between py-3 border-b border-luxury-100 last:border-0">
                                    <span className="text-sm font-medium text-luxury-900">{step.label}</span>
                                    <span className={`status-pill ${step.status === "done" ? "status-pill-occupied" : "status-pill-vacant"}`}>
                                        {step.status}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </BentoCard>

                    <BentoCard title="System Health" eyebrow="Operations Signal" delay={0.36}>
                        <div className="mt-4 space-y-4">
                            <div className="flex justify-between items-center text-sm">
                                <span style={{ color: "rgba(62,59,51,0.7)", fontWeight: 500 }}>API Status</span>
                                <span className="flex items-center gap-2 font-bold" style={{ color: "#16a34a" }}>
                                    <span className="live-dot" />
                                    {systemHealth?.apiStatus || "Healthy"}
                                </span>
                            </div>
                            <div className="flex justify-between items-center text-sm" style={{ borderTop: "1px solid rgba(229,225,216,0.6)", paddingTop: 16 }}>
                                <span style={{ color: "rgba(62,59,51,0.7)", fontWeight: 500 }}>Last Push</span>
                                <span className="font-mono text-xs" style={{ color: "rgba(62,59,51,0.5)" }}>
                                    {systemHealth?.lastPushTime ? new Date(systemHealth.lastPushTime).toLocaleTimeString() : "Never"}
                                </span>
                            </div>
                            <div className="flex justify-between items-center text-sm" style={{ borderTop: "1px solid rgba(229,225,216,0.6)", paddingTop: 16 }}>
                                <span style={{ color: "rgba(62,59,51,0.7)", fontWeight: 500 }}>Launcher</span>
                                <span className="font-mono text-xs" style={{ color: "rgba(62,59,51,0.5)" }}>
                                    {systemHealth?.launcherVersion || "1.0.0"}
                                </span>
                            </div>
                        </div>
                    </BentoCard>
                </div>
            </div>
        </div>
    );
}
