"use client";

import React, { useEffect, useState } from "react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { api } from "../../../lib/api";

export default function AuditPage() {
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

    const exportCsv = () => {
        if (!data?.auditLogs) return;
        const csv = [
            "Actor,Role,Action,Time",
            ...data.auditLogs.map((row: any) => `"${row.actorName}","${row.actorRole}","${String(row.action).replaceAll('"', '""')}","${row.createdAt}"`)
        ].join("\n");
        const blob = new Blob([csv], { type: "text/csv" });
        const link = document.createElement("a");
        link.href = URL.createObjectURL(blob);
        link.download = "asteria-audit-log.csv";
        link.click();
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="space-y-6 max-w-5xl mx-auto">
            <BentoCard
                title="Audit Log"
                eyebrow="Role-Based Activity"
                actions={
                    <PillButton onClick={exportCsv} primary>Export CSV</PillButton>
                }
            >
                <div className="mt-6 space-y-4 max-h-[70vh] overflow-y-auto pr-4">
                    {data.auditLogs?.length === 0 && <div className="text-luxury-800/50">No audit logs available or insufficient permission.</div>}
                    {data.auditLogs?.map((log: any, i: number) => (
                        <div key={i} className="flex justify-between items-center py-4 border-b border-luxury-100 last:border-0 hover:bg-luxury-50/50 transition-colors px-2 rounded-xl">
                            <div className="flex gap-4 items-center">
                                <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider shadow-sm border ${log.actorRole === 'SUPER_ADMIN' ? 'bg-gold-50 text-gold-700 border-gold-200' :
                                        log.actorRole === 'SYSTEM' ? 'bg-luxury-200 text-luxury-800 border-luxury-300' : 'bg-white text-luxury-800 border-luxury-200'
                                    }`}>
                                    {log.actorName || "SYSTEM"}
                                </span>
                                <span className="text-sm font-medium text-luxury-900">{log.action}</span>
                            </div>
                            <span className="font-mono text-xs text-luxury-800/50 whitespace-nowrap ml-4">
                                {new Date(log.createdAt).toLocaleString()}
                            </span>
                        </div>
                    ))}
                </div>
            </BentoCard>
        </div>
    );
}
