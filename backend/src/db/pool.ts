import pg from "pg";
import { getEnv } from "../config/env.js";

const { Pool } = pg;
let pool: pg.Pool | undefined;

export function getPool(): pg.Pool {
  if (pool) return pool;

  const parsedUrl = new URL(getEnv().databaseUrl);
  if (parsedUrl.protocol !== "postgres:" && parsedUrl.protocol !== "postgresql:") {
    throw new Error("DATABASE_URL debe usar PostgreSQL");
  }

  // pg recibe la política TLS explícitamente para conservar la validación del certificado.
  parsedUrl.searchParams.delete("sslmode");
  pool = new Pool({
    connectionString: parsedUrl.toString(),
    ssl: { rejectUnauthorized: true },
    max: 10,
    connectionTimeoutMillis: 10_000,
    idleTimeoutMillis: 30_000,
  });
  return pool;
}

export async function closePool(): Promise<void> {
  if (!pool) return;
  const activePool = pool;
  pool = undefined;
  await activePool.end();
}
