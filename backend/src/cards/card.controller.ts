import type { NextFunction, Request, Response } from "express";
import { z } from "zod";
import type { CardService } from "./card.service.js";

const cardIdSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  "cardId debe ser un UUID",
);
const blockBodySchema = z.object({ confirmed: z.literal(true) }).strict();
const thresholdBodySchema = z.object({
  threshold: z.number().int().positive().max(Number.MAX_SAFE_INTEGER),
}).strict();

export class CardController {
  constructor(private readonly service: CardService) {}

  get = async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      const cardId = cardIdSchema.parse(request.params.cardId);
      response.json(await this.service.getCard(cardId));
    } catch (error) {
      next(error);
    }
  };

  block = async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      const cardId = cardIdSchema.parse(request.params.cardId);
      const body = blockBodySchema.parse(request.body);
      const card = await this.service.blockCard(cardId, body.confirmed);
      response.json({ id: card.id, status: card.status, blockedAt: card.blockedAt });
    } catch (error) {
      next(error);
    }
  };

  updateThreshold = async (
    request: Request,
    response: Response,
    next: NextFunction,
  ): Promise<void> => {
    try {
      const cardId = cardIdSchema.parse(request.params.cardId);
      const body = thresholdBodySchema.parse(request.body);
      const card = await this.service.updateThreshold(cardId, body.threshold);
      response.json({ id: card.id, lowBalanceThreshold: card.lowBalanceThreshold });
    } catch (error) {
      next(error);
    }
  };
}
