import { createServer } from "node:http";
import { createApp } from "./app.js";
import { getEnv } from "./config/env.js";
import { closePool } from "./db/pool.js";

const server = createServer(createApp());
const { port } = getEnv();

server.listen(port, () => {
  console.log(`API Pasaje escuchando en el puerto ${port}`);
});

let shuttingDown = false;
async function shutdown(): Promise<void> {
  if (shuttingDown) return;
  shuttingDown = true;
  server.close(async () => {
    await closePool();
    process.exit(0);
  });
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
