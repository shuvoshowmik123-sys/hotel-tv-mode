const fs = require("fs");
const path = require("path");

const nextDir = path.join(__dirname, "..", ".next");
const src = path.join(nextDir, "routes-manifest.json");
const dest = path.join(nextDir, "routes-manifest-deterministic.json");

console.log(`[post-build] nextDir: ${nextDir}`);

if (!fs.existsSync(src)) {
    console.error(`[post-build] ERROR: routes-manifest.json not found at ${src}`);
    process.exit(1);
}

// Always force-recreate — never skip.
// Vercel's onBuildComplete hook may create this file and then remove it
// when packaging the output. We must write it AFTER that step completes.
try { fs.rmSync(dest, { force: true }); } catch { }
fs.copyFileSync(src, dest);
console.log(`[post-build] Created: ${dest}`);

if (!fs.existsSync(dest)) {
    console.error(`[post-build] FAILED to verify ${dest}`);
    process.exit(1);
}
console.log(`[post-build] Verified OK (${fs.statSync(dest).size} bytes)`);
