import { Router } from "express";
import { getPool } from "../db/pool.js";
import { PurchaseController } from "./purchase.controller.js";
import { PgPurchaseRepository } from "./purchase.repository.js";
import { PurchaseService } from "./purchase.service.js";

export function createPurchaseRouter(): Router {
  const router = Router({ mergeParams: true });
  const controller = new PurchaseController(
    new PurchaseService(new PgPurchaseRepository(getPool())),
  );
  router.post("/", controller.create);
  return router;
}
