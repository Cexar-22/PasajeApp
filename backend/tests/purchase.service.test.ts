import { describe, expect, it } from "vitest";
import type {
  PurchaseCard,
  PurchaseDataSource,
  PurchaseRecord,
  PurchaseTransactionDataSource,
} from "../src/purchases/purchase.repository.js";
import { PurchaseService } from "../src/purchases/purchase.service.js";

class FakeTransaction implements PurchaseTransactionDataSource {
  records = new Map<string, PurchaseRecord>();
  rejectedCount = 0;
  approvedCount = 0;
  alertCount = 0;

  constructor(public card: PurchaseCard | null) {}

  async findCardForUpdate(): Promise<PurchaseCard | null> {
    return this.card;
  }

  async findByRequestId(_cardId: string, requestId: string): Promise<PurchaseRecord | null> {
    return this.records.get(requestId) ?? null;
  }

  async rejectPurchase(input: {
    requestId: string;
    reason: "CARD_BLOCKED" | "INSUFFICIENT_FUNDS";
    amount: number;
    balance: number;
  }): Promise<void> {
    this.rejectedCount += 1;
    this.records.set(input.requestId, {
      status: "REJECTED",
      rejectionReason: input.reason,
      amount: input.amount,
      balanceBefore: input.balance,
      balanceAfter: input.balance,
    });
  }

  async createSecurityAlert(): Promise<void> {
    this.alertCount += 1;
  }

  async approvePurchase(input: {
    requestId: string;
    amount: number;
    previousBalance: number;
    currentBalance: number;
  }): Promise<void> {
    this.approvedCount += 1;
    this.records.set(input.requestId, {
      status: "APPROVED",
      rejectionReason: null,
      amount: input.amount,
      balanceBefore: input.previousBalance,
      balanceAfter: input.currentBalance,
    });
    if (this.card) this.card.balance = input.currentBalance;
  }
}

class FakePurchaseRepository implements PurchaseDataSource {
  constructor(readonly transaction: FakeTransaction) {}
  async withTransaction<T>(
    work: (transaction: PurchaseTransactionDataSource) => Promise<T>,
  ): Promise<T> {
    return work(this.transaction);
  }
}

const card = (status: "ACTIVE" | "BLOCKED" = "ACTIVE"): PurchaseCard => ({
  id: "00000000-0000-0000-0000-000000000001",
  status,
  balance: 20_000,
  lowBalanceThreshold: 5_000,
});

const input = (requestId = "purchase-1") => ({
  cardId: card().id,
  amount: 3_000,
  merchant: "Comercio de prueba",
  requestId,
});

describe("PurchaseService", () => {
  it("rechaza una compra con tarjeta bloqueada", async () => {
    const transaction = new FakeTransaction(card("BLOCKED"));
    const result = await new PurchaseService(new FakePurchaseRepository(transaction)).process(input());
    expect(result).toMatchObject({ kind: "REJECTED", code: "CARD_BLOCKED", balanceChanged: false });
  });

  it("una compra bloqueada no modifica el saldo", async () => {
    const transaction = new FakeTransaction(card("BLOCKED"));
    await new PurchaseService(new FakePurchaseRepository(transaction)).process(input());
    expect(transaction.card?.balance).toBe(20_000);
  });

  it("registra la transacción bloqueada como REJECTED", async () => {
    const transaction = new FakeTransaction(card("BLOCKED"));
    await new PurchaseService(new FakePurchaseRepository(transaction)).process(input());
    expect(transaction.rejectedCount).toBe(1);
    expect(transaction.records.get("purchase-1")?.status).toBe("REJECTED");
    expect(transaction.records.get("purchase-1")?.rejectionReason).toBe("CARD_BLOCKED");
  });

  it("genera una alerta de seguridad para la compra bloqueada", async () => {
    const transaction = new FakeTransaction(card("BLOCKED"));
    await new PurchaseService(new FakePurchaseRepository(transaction)).process(input());
    expect(transaction.alertCount).toBe(1);
  });

  it("procesa y descuenta una compra con tarjeta activa", async () => {
    const transaction = new FakeTransaction(card());
    const result = await new PurchaseService(new FakePurchaseRepository(transaction)).process(input());
    expect(result).toMatchObject({
      kind: "APPROVED",
      previousBalance: 20_000,
      currentBalance: 17_000,
      lowBalance: false,
    });
    expect(transaction.card?.balance).toBe(17_000);
    expect(transaction.approvedCount).toBe(1);
  });

  it("calcula saldo bajo solo cuando queda estrictamente bajo el umbral", async () => {
    const exactThreshold = new FakeTransaction({ ...card(), balance: 8_000 });
    const exactResult = await new PurchaseService(new FakePurchaseRepository(exactThreshold)).process(input());
    expect(exactResult).toMatchObject({ lowBalance: false });

    const belowThreshold = new FakeTransaction({ ...card(), balance: 7_999 });
    const belowResult = await new PurchaseService(new FakePurchaseRepository(belowThreshold)).process(
      input("purchase-2"),
    );
    expect(belowResult).toMatchObject({ lowBalance: true });
  });

  it("una requestId duplicada no descuenta ni registra dos veces", async () => {
    const transaction = new FakeTransaction(card());
    const service = new PurchaseService(new FakePurchaseRepository(transaction));
    await service.process(input());
    const duplicate = await service.process(input());
    expect(transaction.card?.balance).toBe(17_000);
    expect(transaction.approvedCount).toBe(1);
    expect(duplicate).toMatchObject({ kind: "APPROVED", duplicate: true });
  });

  it("una requestId bloqueada duplicada no genera otra alerta", async () => {
    const transaction = new FakeTransaction(card("BLOCKED"));
    const service = new PurchaseService(new FakePurchaseRepository(transaction));
    await service.process(input());
    await service.process(input());
    expect(transaction.rejectedCount).toBe(1);
    expect(transaction.alertCount).toBe(1);
  });
});
