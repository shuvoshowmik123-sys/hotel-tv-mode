/**
 * post-build.cjs
 *
 * Two jobs:
 * 1. Mirror frontend/.next → repo-root/.next so Vercel's post-build adapter
 *    (which runs at the repo root, not the rootDirectory) can find the output.
 * 2. Force-create routes-manifest-deterministic.json in BOTH locations.
 *    Vercel's onBuildComplete hook may create/remove this file during packaging;
 *    we always recreate it as the very last step.
 */
const fs = require("fs");
const path = require("path");

// __dirname = /vercel/path0/frontend/scripts  (repo root = /vercel/path0/)
const frontendNextDir = path.join(__dirname, "..", ".next"); // frontend/.next
const rootNextDir = path.join(__dirname, "..", "..", ".next"); // repo-root/.next

console.log(`[post-build] frontendNextDir : ${frontendNextDir}`);
console.log(`[post-build] rootNextDir     : ${rootNextDir}`);

// ── 1. Mirror entire .next folder to repo root ────────────────────────────
function copyDir(src, dest) {
    fs.mkdirSync(dest, { recursive: true });
    for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
        const s = path.join(src, entry.name);
        const d = path.join(dest, entry.name);
        if (entry.isDirectory()) {
            copyDir(s, d);
        } else if (entry.isSymbolicLink()) {
            try { fs.rmSync(d, { force: true, recursive: true }); } catch { }
            fs.symlinkSync(fs.readlinkSync(s), d);
        } else {
            fs.copyFileSync(s, d);
        }
    }
}

if (!fs.existsSync(frontendNextDir)) {
    console.error(`[post-build] ERROR: ${frontendNextDir} not found — did next build run?`);
    process.exit(1);
}

try { fs.rmSync(rootNextDir, { recursive: true, force: true }); } catch { }
copyDir(frontendNextDir, rootNextDir);
console.log(`[post-build] Mirrored .next to ${rootNextDir}`);

// ── 2. Force-create routes-manifest-deterministic.json in both dirs ───────
function forceDeterministicManifest(baseDir) {
    const src = path.join(baseDir, "routes-manifest.json");
    const dest = path.join(baseDir, "routes-manifest-deterministic.json");
    if (!fs.existsSync(src)) {
        console.warn(`[post-build] WARN: routes-manifest.json not found in ${baseDir}`);
        return;
    }
    try { fs.rmSync(dest, { force: true }); } catch { }
    fs.copyFileSync(src, dest);
    console.log(`[post-build] Created ${dest} (${fs.statSync(dest).size} bytes)`);
}

forceDeterministicManifest(frontendNextDir);
forceDeterministicManifest(rootNextDir);
console.log("[post-build] Done.");
