import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import { getPool, closePool } from "./pool.js";
import { getEnv } from "../config/env.js";

const migrationName = "001_initial_schema.sql";
const migrationUrl = new URL(`./migrations/${migrationName}`, import.meta.url);

async function migrate(): Promise<void> {
  const sql = await readFile(fileURLToPath(migrationUrl), "utf8");
  const checksum = createHash("sha256").update(sql).digest("hex");
  const client = await getPool().connect();

  try {
    await client.query("BEGIN");
    await client.query(`
      CREATE TABLE IF NOT EXISTS schema_migrations (
        name VARCHAR PRIMARY KEY,
        checksum VARCHAR NOT NULL,
        executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
      )
    `);
    const existing = await client.query<{ checksum: string }>(
      "SELECT checksum FROM schema_migrations WHERE name = $1",
      [migrationName],
    );

    if (existing.rowCount === 0) {
      await client.query(sql);
      await client.query(
        "INSERT INTO schema_migrations(name, checksum) VALUES ($1, $2)",
        [migrationName, checksum],
      );
    } else if (existing.rows[0].checksum !== checksum) {
      throw new Error("La migración aplicada fue modificada");
    }

    await client.query(
      `INSERT INTO cards(
         id, user_id, last_four, status, balance, low_balance_threshold, blocked_at
       ) VALUES ($1, 'demo-user', '1234', 'ACTIVE', 20000, 5000, NULL)
       ON CONFLICT (id) DO NOTHING`,
      [getEnv().demoCardId],
    );

    await client.query("COMMIT");
    console.log(existing.rowCount === 0 ? "Migración aplicada" : "Migración ya aplicada");
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

migrate()
  .catch(() => {
    console.error("No fue posible ejecutar las migraciones");
    process.exitCode = 1;
  })
  .finally(closePool);
