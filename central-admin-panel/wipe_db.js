// Deploy touch: harmless tracked change to trigger a fresh Vercel deployment.
require("dotenv").config({ path: require("path").join(__dirname, ".env.local") });
const { Pool } = require("pg");

async function run() {
    const pool = new Pool({
        connectionString: process.env.DATABASE_URL,
        ssl: { rejectUnauthorized: false }
    });

    try {
        console.log("Dropping tables...");
        await pool.query(`
      DROP TABLE IF EXISTS launcher_store CASCADE;
      DROP TABLE IF EXISTS pending_activations CASCADE;
      DROP TABLE IF EXISTS device_bindings CASCADE;
      DROP TABLE IF EXISTS device_status_reports CASCADE;
    `);
        console.log("Database reset complete. Next Vercel boot will initialize empty state.");
    } catch (err) {
        console.error(err);
    } finally {
        pool.end();
    }
}

run();
