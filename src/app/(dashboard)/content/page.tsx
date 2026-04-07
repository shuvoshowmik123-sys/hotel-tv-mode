"use client";

import React, { useEffect, useState } from "react";
import { AnimatePresence, motion } from "motion/react";
import { BentoCard } from "../../../components/BentoCard";
import { PillButton } from "../../../components/UIElements";
import { ConfirmModal } from "../../../components/ConfirmModal";
import { useFeedback } from "../../../components/FeedbackProvider";
import { api } from "../../../lib/api";
import { canModuleAction } from "../../../lib/permissions";

export default function ContentPage() {
    const [data, setData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [deleteTarget, setDeleteTarget] = useState<any>(null);
    const [previewIndex, setPreviewIndex] = useState(-1);
    const [previewProgress, setPreviewProgress] = useState(0);
    const [previewLoaded, setPreviewLoaded] = useState(false);
    const [previewFailed, setPreviewFailed] = useState(false);
    const [uploadingLabel, setUploadingLabel] = useState("");
    const [startupFileName, setStartupFileName] = useState("");
    const [backgroundFileName, setBackgroundFileName] = useState("");
    const { notify } = useFeedback();

    const load = async () => {
        setLoadError("");
        try {
            const state = await api("/api/admin/state");
            setData(state);
        } catch (err: any) {
            setLoadError(err.message || "Unable to load content settings.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const handleUpload = async (e: React.FormEvent, kind: "startup" | "background") => {
        e.preventDefault();
        try {
            const formEl = e.currentTarget as HTMLFormElement;
            const fd = new FormData(formEl);
            const file = fd.get("file");
            const bucket = `${fd.get("bucket") || "home"}`.trim() || "home";
            if (!(file instanceof File) || !file.size) {
                notify({ tone: "warning", message: "Please choose an image before uploading." });
                return;
            }
            setUploadingLabel(kind === "startup" ? "Uploading startup asset..." : "Uploading background asset...");
            const auth = await api("/api/admin/upload-auth", {
                method: "POST",
                body: JSON.stringify({
                    kind,
                    bucket,
                    originalName: file.name,
                }),
            });
            const uploadBody = new FormData();
            uploadBody.append("file", file);
            uploadBody.append("fileName", auth.fileName);
            uploadBody.append("folder", auth.folder);
            uploadBody.append("publicKey", auth.publicKey);
            uploadBody.append("signature", auth.signature);
            uploadBody.append("expire", String(auth.expire));
            uploadBody.append("token", auth.token);
            uploadBody.append("useUniqueFileName", "false");

            const uploadResponse = await fetch(auth.uploadUrl, {
                method: "POST",
                body: uploadBody,
            });
            const uploadResult = await uploadResponse.json().catch(() => ({}));
            if (!uploadResponse.ok) {
                throw new Error(uploadResult.message || uploadResult.error || "ImageKit upload failed.");
            }

            await api("/api/admin/assets/register", {
                method: "POST",
                body: JSON.stringify({
                    kind,
                    bucket,
                    originalUrl: uploadResult.url,
                    mimeType: file.type,
                }),
            });
            notify({
                tone: "success",
                message: kind === "startup" ? "Startup asset uploaded and optimized." : "Background uploaded and optimized for launcher sync.",
            });
            formEl.reset();
            if (kind === "startup") {
                setStartupFileName("");
            } else {
                setBackgroundFileName("");
            }
            await load();
        } catch (error: any) {
            notify({ tone: "error", message: error.message || "Failed to upload asset." });
        } finally {
            setUploadingLabel("");
        }
    };

    const handleConfigUpdate = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const form = Object.fromEntries(new FormData(e.currentTarget as HTMLFormElement));
            await api("/api/admin/config", {
                method: "POST",
                body: JSON.stringify({
                    hotel: { hotelName: form.hotelName },
                    popup: { helpTitle: form.helpTitle, callNumber: form.callNumber, ratingText: form.ratingText }
                })
            });
            notify({ tone: "success", message: "Guest-facing content saved." });
            await load();
        } catch (error: any) {
            notify({ tone: "error", message: error.message || "Failed to save content." });
        }
    };

    const toggleDestination = async (key: string) => {
        try {
            const visibility = {
                ...(data.visibility || {}),
                destinations: {
                    ...(data.visibility?.destinations || {}),
                    [key]: !data.visibility?.destinations?.[key],
                },
            };
            await api("/api/admin/config", { method: "POST", body: JSON.stringify({ visibility }) });
            notify({ tone: "success", message: `${key.replace(/([A-Z])/g, " $1").trim()} visibility updated.` });
            await load();
        } catch (error: any) {
            notify({ tone: "error", message: error.message || "Failed to update visibility." });
        }
    };

    const assetDisplayName = (asset: any) => {
        const source = `${asset?.url || ""}`;
        if (!source) return "Untitled asset";
        try {
            const parsed = new URL(source);
            const fileName = decodeURIComponent(parsed.pathname.split("/").pop() || "").trim();
            return fileName || "Untitled asset";
        } catch {
            const parts = source.split("/");
            return decodeURIComponent(parts[parts.length - 1] || source);
        }
    };
    const assets = data?.assets || [];
    const startupAsset = assets.find((asset: any) => asset.kind === "startup");
    const backgroundAssets = assets.filter((asset: any) => asset.kind === "background");
    const galleryAssets = startupAsset ? [startupAsset, ...backgroundAssets] : backgroundAssets;
    const previewTarget = previewIndex >= 0 ? galleryAssets[previewIndex] ?? null : null;
    const buckets = ["home", "roomService", "foodMenu", "inputs"];
    const currentRole = data?.currentUser?.role;
    const canCreateContent = canModuleAction(currentRole, "content", "create");
    const canEditContent = canModuleAction(currentRole, "content", "edit");
    const canDeleteContent = canModuleAction(currentRole, "content", "delete");

    useEffect(() => {
        if (!previewTarget) {
            setPreviewProgress(0);
            setPreviewLoaded(false);
            setPreviewFailed(false);
            return;
        }

        setPreviewProgress(0);
        setPreviewLoaded(false);
        setPreviewFailed(false);

        const timer = window.setInterval(() => {
            setPreviewProgress((current) => {
                if (current >= 92) {
                    return current;
                }
                return Math.min(92, current + Math.max(4, Math.round((100 - current) / 8)));
            });
        }, 90);

        return () => window.clearInterval(timer);
    }, [previewTarget?.id]);

    useEffect(() => {
        if (!previewTarget && previewIndex !== -1) {
            setPreviewIndex(-1);
        }
    }, [previewIndex, previewTarget]);

    const openPreview = (asset: any) => {
        const index = galleryAssets.findIndex((entry: any) => entry.id === asset.id);
        if (index >= 0) {
            setPreviewIndex(index);
        }
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="space-y-6">
            {loadError && <div className="page-inline-error">{loadError}</div>}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <BentoCard title="Startup Asset" eyebrow="Launcher Boot Media">
                    {canCreateContent ? (
                        <form className="mt-4 space-y-4" onSubmit={e => handleUpload(e, "startup")}>
                            <div className="border-2 border-dashed border-luxury-200 rounded-2xl p-6 text-center hover:bg-luxury-50 transition-colors">
                                <input
                                    id="startup-file-input"
                                    type="file"
                                    name="file"
                                    accept="video/mp4,image/*"
                                    className="sr-only"
                                    required
                                    onChange={(event) => setStartupFileName(event.target.files?.[0]?.name || "")}
                                />
                                <label htmlFor="startup-file-input" className="upload-pick-button">
                                    Choose startup file
                                </label>
                                <div className="mt-4 flex min-h-6 items-center justify-center">
                                    {startupFileName ? (
                                        <span className="upload-file-chip truncate max-w-full" title={startupFileName}>
                                            {startupFileName}
                                        </span>
                                    ) : (
                                        <span className="text-sm text-luxury-800/55">No file selected yet</span>
                                    )}
                                </div>
                            </div>
                            <PillButton primary type="submit" className="w-full" disabled={Boolean(uploadingLabel)}>
                                {uploadingLabel && uploadingLabel.includes("startup") ? uploadingLabel : "Upload Startup Asset"}
                            </PillButton>
                        </form>
                    ) : (
                        <div className="mt-4 rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4 text-sm text-luxury-800/55">
                            This role can review startup media but cannot upload or replace it.
                        </div>
                    )}
                    <div className="mt-4 text-sm text-luxury-800/60">
                        {startupAsset ? (
                            <div className="space-y-3">
                                <button
                                    type="button"
                                    onClick={() => openPreview(startupAsset)}
                                    className="flex w-full items-center justify-between gap-3 rounded-xl border border-luxury-100 bg-white px-4 py-3 text-left transition-colors hover:bg-luxury-50"
                                >
                                    <div className="min-w-0">
                                        <div className="truncate text-sm font-semibold text-luxury-900">{assetDisplayName(startupAsset)}</div>
                                        <div className="mt-1 text-xs text-luxury-800/50">Startup media preview</div>
                                    </div>
                                    <span className="rounded-full bg-gold-500/10 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-gold-700">
                                        View
                                    </span>
                                </button>
                                {canDeleteContent && <PillButton type="button" onClick={() => setDeleteTarget(startupAsset)}>Delete Asset</PillButton>}
                            </div>
                        ) : (
                            "No startup asset has been uploaded yet."
                        )}
                    </div>
                </BentoCard>

                <BentoCard title="Background Slideshow" eyebrow="Ambient Media">
                    {canCreateContent ? (
                        <form className="mt-4 space-y-4" onSubmit={e => handleUpload(e, "background")}>
                            <select name="bucket" className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-2 text-sm focus:ring-2 focus:ring-gold-500/50">
                                <option value="home">Home Screen</option>
                                <option value="roomService">Room Service</option>
                                <option value="foodMenu">Food Menu</option>
                                <option value="inputs">TV Inputs</option>
                            </select>
                            <div className="border-2 border-dashed border-luxury-200 rounded-2xl p-6 text-center hover:bg-luxury-50 transition-colors">
                                <input
                                    id="background-file-input"
                                    type="file"
                                    name="file"
                                    accept="image/*"
                                    className="sr-only"
                                    required
                                    onChange={(event) => setBackgroundFileName(event.target.files?.[0]?.name || "")}
                                />
                                <label htmlFor="background-file-input" className="upload-pick-button">
                                    Choose background image
                                </label>
                                <div className="mt-4 flex min-h-6 items-center justify-center">
                                    {backgroundFileName ? (
                                        <span className="upload-file-chip truncate max-w-full" title={backgroundFileName}>
                                            {backgroundFileName}
                                        </span>
                                    ) : (
                                        <span className="text-sm text-luxury-800/55">No file selected yet</span>
                                    )}
                                </div>
                            </div>
                            <PillButton primary type="submit" className="w-full" disabled={Boolean(uploadingLabel)}>
                                {uploadingLabel && uploadingLabel.includes("background") ? uploadingLabel : "Upload Background"}
                            </PillButton>
                        </form>
                    ) : (
                        <div className="mt-4 rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4 text-sm text-luxury-800/55">
                            Background uploads are limited to roles with content publishing access.
                        </div>
                    )}
                </BentoCard>

                <BentoCard title="Stored Assets" eyebrow="Current Media Library">
                    <div className="mt-4 space-y-4 max-h-[320px] overflow-y-auto pr-1">
                        {!startupAsset && backgroundAssets.length === 0 && (
                            <div className="rounded-xl border-2 border-dashed border-luxury-200 p-5 text-sm text-luxury-800/50">
                                No uploaded assets yet.
                            </div>
                        )}
                        {buckets.map((bucket) => {
                            const bucketEntries = backgroundAssets.filter((asset: any) => asset.bucket === bucket);
                            return (
                                <div key={bucket} className="rounded-2xl border border-luxury-100 bg-luxury-50/70 p-4">
                                    <div className="text-xs font-bold uppercase tracking-wider text-luxury-800/50 mb-2">
                                        {bucket.replace(/([A-Z])/g, " $1").trim()}
                                    </div>
                                    {bucketEntries.length === 0 ? (
                                        <div className="text-sm text-luxury-800/50">No backgrounds stored.</div>
                                    ) : (
                                        <div className="space-y-2">
                                            {bucketEntries.map((asset: any) => (
                                                <div key={asset.id} className="flex items-center justify-between gap-3 rounded-xl bg-white px-3 py-2 border border-luxury-100">
                                                    <button
                                                        type="button"
                                                        onClick={() => openPreview(asset)}
                                                        className="min-w-0 flex-1 text-left"
                                                    >
                                                        <div className="truncate text-sm font-semibold text-luxury-900">{assetDisplayName(asset)}</div>
                                                        <div className="mt-0.5 text-xs text-luxury-800/45">Click to preview inside the panel</div>
                                                    </button>
                                                    {canDeleteContent && (
                                                        <PillButton type="button" className="!py-1.5 !px-3 !text-xs" onClick={() => setDeleteTarget(asset)}>
                                                            Delete
                                                        </PillButton>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </BentoCard>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <BentoCard title="Tile Visibility" eyebrow="Launcher Destinations">
                    <div className="mt-4 space-y-2">
                        {["home", "roomService", "foodMenu", "inputs"].map(key => {
                            const isOn = data.visibility?.destinations?.[key];
                            return (
                                <div key={key} className="flex justify-between items-center p-4 border border-luxury-100 rounded-xl bg-luxury-50 hover:bg-luxury-100 transition-colors">
                                    <div>
                                        <div className="font-medium text-luxury-900 capitalize">{key.replace(/([A-Z])/g, " $1").trim()}</div>
                                        <div className="text-xs text-luxury-800/60">Destination tile visible on TV</div>
                                    </div>
                                    <button
                                        type="button"
                                        onClick={() => canEditContent && toggleDestination(key)}
                                        disabled={!canEditContent}
                                        className={`w-12 h-6 rounded-full transition-colors relative ${isOn ? "bg-gold-500" : "bg-luxury-200"} ${canEditContent ? "" : "opacity-60 cursor-not-allowed"}`}
                                    >
                                        <span className={`absolute top-1 w-4 h-4 rounded-full bg-white transition-all ${isOn ? "left-7" : "left-1"}`} />
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
                            <input name="hotelName" defaultValue={data.hotel?.hotelName} disabled={!canEditContent} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50 disabled:opacity-60" />
                        </div>
                        <div>
                            <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Help Title</label>
                            <input name="helpTitle" defaultValue={data.popup?.helpTitle} disabled={!canEditContent} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50 disabled:opacity-60" />
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Call Number</label>
                                <input name="callNumber" defaultValue={data.popup?.callNumber} disabled={!canEditContent} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50 disabled:opacity-60" />
                            </div>
                            <div>
                                <label className="block text-xs font-bold uppercase tracking-wider text-luxury-800/60 mb-1">Rating Prompt Text</label>
                                <input name="ratingText" defaultValue={data.popup?.ratingText} disabled={!canEditContent} className="w-full bg-luxury-50 border border-luxury-200 rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-gold-500/50 disabled:opacity-60" />
                            </div>
                        </div>
                        <div className="flex justify-end pt-4">
                            {canEditContent ? <PillButton primary type="submit">Push Changes to TVs</PillButton> : <span className="text-sm text-luxury-800/50">View only</span>}
                        </div>
                    </form>
                </BentoCard>
            </div>

            <ConfirmModal
                open={Boolean(deleteTarget)}
                title="Delete Asset"
                description={deleteTarget ? `Remove this ${deleteTarget.kind === "startup" ? "startup" : `${deleteTarget.bucket} background`} asset from the panel and launcher sync?` : ""}
                confirmLabel="Delete Asset"
                onCancel={() => setDeleteTarget(null)}
                onConfirm={async () => {
                    if (!deleteTarget) return;
                    try {
                        await api(`/api/admin/assets/${deleteTarget.id}`, { method: "DELETE" });
                        notify({ tone: "success", message: "Asset deleted successfully." });
                        setDeleteTarget(null);
                        await load();
                    } catch (error: any) {
                        notify({ tone: "error", message: error.message || "Failed to delete asset." });
                        setDeleteTarget(null);
                    }
                }}
            />

            <AnimatePresence>
                {previewTarget && (
                    <>
                        <motion.div
                            className="fixed inset-0 z-40 bg-black/45 backdrop-blur-sm"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            onClick={() => setPreviewIndex(-1)}
                        />
                        <motion.div
                            className="fixed inset-0 z-50 p-4 lg:p-8"
                            initial={{ opacity: 0, y: 12 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: 12 }}
                        >
                            <div className="mx-auto flex h-full max-w-6xl items-center justify-center" onClick={(event) => event.stopPropagation()}>
                                <div className="flex h-[min(88vh,920px)] w-full flex-col overflow-hidden rounded-[32px] border border-luxury-200 bg-white shadow-[0_30px_90px_-45px_rgba(0,0,0,0.35)]">
                                    <div className="flex flex-wrap items-center justify-between gap-4 border-b border-luxury-100 px-6 py-5">
                                        <div className="min-w-0">
                                            <div className="text-xs font-bold uppercase tracking-[0.24em] text-luxury-800/45">
                                                {previewTarget.kind === "startup" ? "Startup media" : `${previewTarget.bucket} background`}
                                            </div>
                                            <div className="mt-1 truncate text-xl font-semibold text-luxury-950">
                                                {assetDisplayName(previewTarget)}
                                            </div>
                                            <div className="mt-1 text-sm text-luxury-800/55">
                                                Preview {previewIndex + 1} of {galleryAssets.length} inside the central admin panel.
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-2">
                                            <PillButton
                                                type="button"
                                                onClick={() => setPreviewIndex((current) => Math.max(0, current - 1))}
                                                className="!px-4"
                                                disabled={previewIndex <= 0}
                                            >
                                                Previous
                                            </PillButton>
                                            <PillButton
                                                type="button"
                                                onClick={() => setPreviewIndex((current) => Math.min(galleryAssets.length - 1, current + 1))}
                                                className="!px-4"
                                                disabled={previewIndex >= galleryAssets.length - 1}
                                            >
                                                Next
                                            </PillButton>
                                            <PillButton type="button" onClick={() => setPreviewIndex(-1)}>Close</PillButton>
                                        </div>
                                    </div>
                                    <div className="relative flex min-h-0 flex-1 items-center justify-center overflow-hidden bg-luxury-50 px-5 py-5 lg:px-8 lg:py-8">
                                        <div className="relative flex h-full w-full items-center justify-center overflow-hidden rounded-[28px] border border-luxury-100 bg-white">
                                            {!previewFailed && (
                                                <img
                                                    key={previewTarget.id}
                                                    src={previewTarget.url}
                                                    alt={assetDisplayName(previewTarget)}
                                                    onLoad={() => {
                                                        setPreviewProgress(100);
                                                        setPreviewLoaded(true);
                                                    }}
                                                    onError={() => {
                                                        setPreviewProgress(100);
                                                        setPreviewFailed(true);
                                                        setPreviewLoaded(false);
                                                    }}
                                                    className={`h-full w-full object-contain transition-opacity duration-300 ${previewLoaded ? "opacity-100" : "opacity-0"}`}
                                                />
                                            )}

                                            {!previewLoaded && !previewFailed && (
                                                <div className="absolute inset-0 flex flex-col items-center justify-center gap-5 bg-white px-6 text-center">
                                                    <div className="text-sm font-semibold uppercase tracking-[0.28em] text-luxury-800/40">
                                                        Loading Preview
                                                    </div>
                                                    <div className="text-5xl font-semibold text-luxury-950">{previewProgress}%</div>
                                                    <div className="h-2.5 w-full max-w-md overflow-hidden rounded-full bg-luxury-100">
                                                        <motion.div
                                                            className="h-full rounded-full bg-gradient-to-r from-gold-500 via-gold-400 to-gold-300"
                                                            animate={{ width: `${previewProgress}%` }}
                                                            transition={{ ease: "easeOut", duration: 0.2 }}
                                                        />
                                                    </div>
                                                    <div className="max-w-md text-sm text-luxury-800/55">
                                                        Preparing the image inside the panel viewer so staff can browse without leaving the dashboard.
                                                    </div>
                                                </div>
                                            )}

                                            {previewFailed && (
                                                <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-white px-6 text-center">
                                                    <div className="text-sm font-semibold uppercase tracking-[0.28em] text-luxury-800/40">
                                                        Preview Unavailable
                                                    </div>
                                                    <div className="text-2xl font-semibold text-luxury-950">
                                                        This asset could not be displayed.
                                                    </div>
                                                    <div className="max-w-lg text-sm text-luxury-800/55">
                                                        The image link may still be propagating or the asset needs to be reprocessed. You can close this viewer and try again.
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </motion.div>
                    </>
                )}
            </AnimatePresence>
        </div>
    );
}
