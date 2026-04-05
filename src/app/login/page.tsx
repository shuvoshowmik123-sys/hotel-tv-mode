"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { BentoCard } from "../../components/BentoCard";
import { PillButton } from "../../components/UIElements";
import { api } from "../../lib/api";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const router = useRouter();

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError("");

        try {
            const data = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({ email, password })
            });
            if (data.user) {
                router.push("/dashboard");
            }
        } catch (err: any) {
            setError(err.message || "Failed to login");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-luxury-50 p-4">
            <div className="w-full max-w-md">
                <BentoCard className="!p-8">
                    <div className="flex justify-center mb-6">
                        <div className="w-16 h-16 rounded-2xl bg-gold-500 text-white flex items-center justify-center font-bold text-3xl shadow-md">
                            AG
                        </div>
                    </div>

                    <div className="text-center mb-8">
                        <div className="text-[10px] font-bold uppercase tracking-widest text-luxury-800/50 mb-2">
                            Private Operations Portal
                        </div>
                        <h1 className="text-2xl font-bold text-luxury-900 tracking-tight">
                            Asteria Grand Central Admin Panel
                        </h1>
                        <p className="text-sm text-luxury-800/60 mt-3 font-medium">
                            Sign in to manage rooms, guest sessions, content, and launcher operations.
                        </p>
                    </div>

                    <form onSubmit={handleLogin} className="space-y-4">
                        <div>
                            <input
                                type="email"
                                value={email}
                                onChange={e => setEmail(e.target.value)}
                                placeholder="Email address"
                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3.5 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                required
                            />
                        </div>
                        <div>
                            <input
                                type="password"
                                value={password}
                                onChange={e => setPassword(e.target.value)}
                                placeholder="Password"
                                className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3.5 text-sm focus:outline-none focus:ring-2 focus:ring-gold-500/50 focus:border-gold-500 transition-colors"
                                required
                            />
                        </div>

                        {error && (
                            <div className="p-3 rounded-lg bg-red-50 text-red-600 text-sm">
                                {error}
                            </div>
                        )}

                        <div className="pt-2">
                            <PillButton primary type="submit" className="w-full !py-3" disabled={loading}>
                                {loading ? "Signing in..." : "Log In"}
                            </PillButton>
                        </div>

                        <div className="text-center text-xs text-luxury-800/50 pt-4 cursor-pointer hover:text-luxury-800"
                            onClick={() => {
                                setEmail("superadmin@asteriagrand.local");
                                setPassword("Asteria@2026!");
                            }}
                        >
                            Demo: superadmin@asteriagrand.local / Asteria@2026!
                        </div>
                    </form>
                </BentoCard>
            </div>
        </div>
    );
}
