import { Router } from "express";
import { getPool } from "../db/pool.js";
import { CardController } from "./card.controller.js";
import { PgCardRepository } from "./card.repository.js";
import { CardService } from "./card.service.js";

export function createCardRouter(): Router {
  const router = Router();
  const controller = new CardController(new CardService(new PgCardRepository(getPool())));
  router.get("/:cardId", controller.get);
  router.post("/:cardId/block", controller.block);
  router.patch("/:cardId/threshold", controller.updateThreshold);
  return router;
}
