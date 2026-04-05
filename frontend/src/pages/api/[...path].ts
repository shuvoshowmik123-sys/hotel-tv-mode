import type { NextApiRequest, NextApiResponse } from "next";

export const config = {
  api: {
    bodyParser: false,
    externalResolver: true,
  },
};

let initPromise: Promise<void> | null = null;

async function getAdminApp() {
  const mod = await import("../../../admin-backend/server.js");
  if (!initPromise) {
    initPromise = Promise.resolve(mod.ensureDatabase());
  }
  await initPromise;
  return mod.app;
}

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const app = await getAdminApp();
  return app(req, res);
}
