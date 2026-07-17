import { describe, expect, it } from "vitest";
import type { Card, CardDataSource } from "../src/cards/card.repository.js";
import { CardService } from "../src/cards/card.service.js";

class FakeCardRepository implements CardDataSource {
  blockCalls = 0;

  constructor(public card: Card | null) {}

  async findById(): Promise<Card | null> {
    return this.card;
  }

  async block(): Promise<Card | null> {
    this.blockCalls += 1;
    if (!this.card) return null;
    this.card = {
      ...this.card,
      status: "BLOCKED",
      blockedAt: this.card.blockedAt ?? "2026-07-16T12:00:00.000Z",
    };
    return this.card;
  }

  async updateThreshold(_cardId: string, threshold: number): Promise<Card | null> {
    if (this.card) this.card = { ...this.card, lowBalanceThreshold: threshold };
    return this.card;
  }
}

const activeCard = (): Card => ({
  id: "00000000-0000-0000-0000-000000000001",
  lastFour: "1234",
  status: "ACTIVE",
  balance: 20_000,
  lowBalanceThreshold: 5_000,
  blockedAt: null,
});

describe("CardService", () => {
  it("bloquea una tarjeta activa", async () => {
    const repository = new FakeCardRepository(activeCard());
    const result = await new CardService(repository).blockCard(activeCard().id, true);
    expect(result.status).toBe("BLOCKED");
    expect(result.blockedAt).not.toBeNull();
  });

  it("responde de forma idempotente cuando ya está bloqueada", async () => {
    const repository = new FakeCardRepository({
      ...activeCard(),
      status: "BLOCKED",
      blockedAt: "2026-07-16T12:00:00.000Z",
    });
    const result = await new CardService(repository).blockCard(activeCard().id, true);
    expect(result.status).toBe("BLOCKED");
    expect(result.blockedAt).toBe("2026-07-16T12:00:00.000Z");
  });

  it("confirmed false no cambia el estado ni llama al repositorio", async () => {
    const repository = new FakeCardRepository(activeCard());
    await expect(new CardService(repository).blockCard(activeCard().id, false)).rejects.toMatchObject({
      code: "CONFIRMATION_REQUIRED",
      status: 400,
    });
    expect(repository.blockCalls).toBe(0);
    expect(repository.card?.status).toBe("ACTIVE");
  });
});
