const fs = require("fs");
const path = require("path");

const sourceDir = path.join(__dirname, "..", ".next");
const targetDir = path.join(__dirname, "..", "..", ".next");
const ROUTES_MANIFEST = "routes-manifest.json";
const DETERMINISTIC_MANIFEST = "routes-manifest-deterministic.json";

function copyDirectory(source, target) {
  fs.mkdirSync(target, { recursive: true });
  for (const entry of fs.readdirSync(source, { withFileTypes: true })) {
    const sourcePath = path.join(source, entry.name);
    const targetPath = path.join(target, entry.name);
    if (entry.isDirectory()) {
      copyDirectory(sourcePath, targetPath);
    } else if (entry.isSymbolicLink()) {
      const link = fs.readlinkSync(sourcePath);
      try {
        fs.rmSync(targetPath, { force: true, recursive: true });
      } catch {}
      fs.symlinkSync(link, targetPath);
    } else {
      fs.copyFileSync(sourcePath, targetPath);
    }
  }
}

function ensureDeterministicManifest(baseDir) {
  const routesManifestPath = path.join(baseDir, ROUTES_MANIFEST);
  const deterministicManifestPath = path.join(baseDir, DETERMINISTIC_MANIFEST);

  if (fs.existsSync(routesManifestPath) && !fs.existsSync(deterministicManifestPath)) {
    fs.copyFileSync(routesManifestPath, deterministicManifestPath);
  }
}

if (!fs.existsSync(sourceDir)) {
  console.log("[mirror-vercel-output] Skipping because .next was not produced.");
  process.exit(0);
}

try {
  fs.rmSync(targetDir, { recursive: true, force: true });
} catch {}

copyDirectory(sourceDir, targetDir);
ensureDeterministicManifest(sourceDir);
ensureDeterministicManifest(targetDir);
console.log(`[mirror-vercel-output] Mirrored ${sourceDir} -> ${targetDir}`);
