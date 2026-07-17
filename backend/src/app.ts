import express from "express";
import { createCardRouter } from "./cards/card.routes.js";
import { getPool } from "./db/pool.js";
import { AppError, errorHandler, notFoundHandler } from "./middleware/errorHandler.js";
import { createPurchaseRouter } from "./purchases/purchase.routes.js";

export function createApp() {
  const app = express();
  app.disable("x-powered-by");
  app.use(express.json({ limit: "16kb" }));

  app.get("/health", async (_request, response, next) => {
    try {
      await getPool().query("SELECT 1");
      response.json({ status: "ok" });
    } catch (_error) {
      next(new AppError(503, "DATABASE_UNAVAILABLE", "La base de datos no está disponible."));
    }
  });

  app.use("/api/cards", createCardRouter());
  app.use("/api/cards/:cardId/purchases", createPurchaseRouter());
  app.use(notFoundHandler);
  app.use(errorHandler);
  return app;
}
