const path = require("path");

process.env.ADMIN_DISABLE_STATIC = "1";
require("dotenv").config({ path: path.join(__dirname, "..", ".env.local") });
require("dotenv").config();

const express = require("express");
const next = require("next");
const { app: adminApiApp, ensureDatabase } = require("../server.js");

const dev = process.env.NODE_ENV !== "production";
const port = 3000;
const hostname = "0.0.0.0";
process.env.PORT = String(port);

async function start() {
  await ensureDatabase();

  const nextApp = next({ dev, dir: __dirname, hostname, port });
  const handle = nextApp.getRequestHandler();

  await nextApp.prepare();

  const server = express();
  server.disable("x-powered-by");

  server.use(adminApiApp);

  server.all(/.*/, (req, res) => handle(req, res));

  server.listen(port, hostname, () => {
    console.log(`Asteria Grand unified app listening on http://localhost:${port}`);
  });
}

start().catch((error) => {
  console.error("Failed to start unified admin panel:", error);
  process.exit(1);
});
