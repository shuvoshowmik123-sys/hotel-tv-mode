/**
 * post-build.cjs
 * Creates routes-manifest-deterministic.json inside .next/ after Next.js build.
 * Vercel's post-build processor requires this file to be present.
 */
const fs = require("fs");
const path = require("path");

const nextDir = path.join(__dirname, "..", ".next");
const src = path.join(nextDir, "routes-manifest.json");
const dest = path.join(nextDir, "routes-manifest-deterministic.json");

if (!fs.existsSync(src)) {
    console.error("[post-build] routes-manifest.json not found — did next build run?");
    process.exit(1);
}

if (!fs.existsSync(dest)) {
    fs.copyFileSync(src, dest);
    console.log("[post-build] Created routes-manifest-deterministic.json");
} else {
    console.log("[post-build] routes-manifest-deterministic.json already exists, skipping.");
}
