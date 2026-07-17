import type { NextFunction, Request, Response } from "express";
import { z } from "zod";
import type { PurchaseService } from "./purchase.service.js";

const cardIdSchema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  "cardId debe ser un UUID",
);
const purchaseBodySchema = z.object({
  amount: z.number().int().positive().max(Number.MAX_SAFE_INTEGER),
  merchant: z.string().trim().min(1).max(120),
  requestId: z.string().trim().min(1).max(100),
}).strict();

export class PurchaseController {
  constructor(private readonly service: PurchaseService) {}

  create = async (request: Request, response: Response, next: NextFunction): Promise<void> => {
    try {
      const cardId = cardIdSchema.parse(request.params.cardId);
      const input = purchaseBodySchema.parse(request.body);
      const result = await this.service.process({ cardId, ...input });
      if (result.kind === "REJECTED") {
        response.status(409).json({
          code: result.code,
          message: result.message,
          balanceChanged: result.balanceChanged,
          currentBalance: result.currentBalance,
          duplicate: result.duplicate,
        });
        return;
      }
      response.status(200).json({
        status: result.status,
        previousBalance: result.previousBalance,
        amount: result.amount,
        currentBalance: result.currentBalance,
        lowBalance: result.lowBalance,
        duplicate: result.duplicate,
      });
    } catch (error) {
      next(error);
    }
  };
}
