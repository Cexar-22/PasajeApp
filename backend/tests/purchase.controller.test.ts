import express from "express";
import request from "supertest";
import { describe, expect, it, vi } from "vitest";
import { errorHandler } from "../src/middleware/errorHandler.js";
import { PurchaseController } from "../src/purchases/purchase.controller.js";
import type { PurchaseService } from "../src/purchases/purchase.service.js";

function testApp(process = vi.fn()) {
  const app = express();
  app.use(express.json());
  const controller = new PurchaseController({ process } as unknown as PurchaseService);
  app.post("/api/cards/:cardId/purchases", controller.create);
  app.use(errorHandler);
  return { app, process };
}

describe("PurchaseController", () => {
  it.each([
    [{ amount: 0, merchant: "Comercio", requestId: "id-1" }],
    [{ amount: 1.5, merchant: "Comercio", requestId: "id-1" }],
    [{ amount: 100, merchant: "", requestId: "id-1" }],
    [{ amount: 100, merchant: "Comercio", requestId: "" }],
  ])("responde 400 de forma controlada ante una entrada inválida", async (body) => {
    const { app, process } = testApp();
    const response = await request(app)
      .post("/api/cards/00000000-0000-0000-0000-000000000001/purchases")
      .send(body);
    expect(response.status).toBe(400);
    expect(response.body.code).toBe("VALIDATION_ERROR");
    expect(process).not.toHaveBeenCalled();
  });
});
