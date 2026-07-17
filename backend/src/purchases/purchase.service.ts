import { AppError } from "../middleware/errorHandler.js";
import type { PurchaseDataSource, PurchaseRecord } from "./purchase.repository.js";

export interface PurchaseInput {
  cardId: string;
  amount: number;
  merchant: string;
  requestId: string;
}

export type PurchaseResult =
  | {
      kind: "APPROVED";
      status: "APPROVED";
      previousBalance: number;
      amount: number;
      currentBalance: number;
      lowBalance: boolean;
      duplicate: boolean;
    }
  | {
      kind: "REJECTED";
      code: "CARD_BLOCKED" | "INSUFFICIENT_FUNDS";
      message: string;
      balanceChanged: false;
      currentBalance: number;
      duplicate: boolean;
    };

function priorResult(record: PurchaseRecord, threshold: number): PurchaseResult {
  if (record.status === "APPROVED") {
    return {
      kind: "APPROVED",
      status: "APPROVED",
      previousBalance: record.balanceBefore,
      amount: record.amount,
      currentBalance: record.balanceAfter,
      lowBalance: record.balanceAfter < threshold,
      duplicate: true,
    };
  }

  const blocked = record.rejectionReason === "CARD_BLOCKED";
  return {
    kind: "REJECTED",
    code: blocked ? "CARD_BLOCKED" : "INSUFFICIENT_FUNDS",
    message: blocked
      ? "La tarjeta está bloqueada temporalmente."
      : "Saldo insuficiente para realizar la compra.",
    balanceChanged: false,
    currentBalance: record.balanceAfter,
    duplicate: true,
  };
}

export class PurchaseService {
  constructor(private readonly repository: PurchaseDataSource) {}

  async process(input: PurchaseInput): Promise<PurchaseResult> {
    return this.repository.withTransaction(async (transaction) => {
      const card = await transaction.findCardForUpdate(input.cardId);
      if (!card) throw new AppError(404, "CARD_NOT_FOUND", "Tarjeta no encontrada.");

      const existing = await transaction.findByRequestId(input.cardId, input.requestId);
      if (existing) return priorResult(existing, card.lowBalanceThreshold);

      if (card.status === "BLOCKED") {
        await transaction.rejectPurchase({
          ...input,
          reason: "CARD_BLOCKED",
          balance: card.balance,
        });
        await transaction.createSecurityAlert(card.id);
        return {
          kind: "REJECTED",
          code: "CARD_BLOCKED",
          message: "La tarjeta está bloqueada temporalmente.",
          balanceChanged: false,
          currentBalance: card.balance,
          duplicate: false,
        };
      }

      if (input.amount > card.balance) {
        await transaction.rejectPurchase({
          ...input,
          reason: "INSUFFICIENT_FUNDS",
          balance: card.balance,
        });
        return {
          kind: "REJECTED",
          code: "INSUFFICIENT_FUNDS",
          message: "Saldo insuficiente para realizar la compra.",
          balanceChanged: false,
          currentBalance: card.balance,
          duplicate: false,
        };
      }

      const previousBalance = card.balance;
      const currentBalance = previousBalance - input.amount;
      await transaction.approvePurchase({
        ...input,
        previousBalance,
        currentBalance,
      });
      return {
        kind: "APPROVED",
        status: "APPROVED",
        previousBalance,
        amount: input.amount,
        currentBalance,
        lowBalance: currentBalance < card.lowBalanceThreshold,
        duplicate: false,
      };
    });
  }
}
