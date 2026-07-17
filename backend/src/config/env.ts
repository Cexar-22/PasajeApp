import dotenv from "dotenv";

dotenv.config({ quiet: true });

export interface AppEnv {
  databaseUrl: string;
  port: number;
  demoCardId: string;
}

let cachedEnv: AppEnv | undefined;

export function getEnv(): AppEnv {
  if (cachedEnv) return cachedEnv;

  const databaseUrl = process.env.DATABASE_URL?.trim();
  if (!databaseUrl) {
    throw new Error("DATABASE_URL no está configurada");
  }

  const port = Number(process.env.PORT ?? "3000");
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("PORT debe ser un puerto válido");
  }

  const demoCardId =
    process.env.DEMO_CARD_ID?.trim() ||
    "00000000-0000-0000-0000-000000000001";
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(demoCardId)) {
    throw new Error("DEMO_CARD_ID debe ser un UUID válido");
  }

  cachedEnv = {
    databaseUrl,
    port,
    demoCardId,
  };
  return cachedEnv;
}

export function resetEnvForTests(): void {
  cachedEnv = undefined;
}
