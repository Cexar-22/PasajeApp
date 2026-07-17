import { randomUUID } from "node:crypto";
import type { Pool, PoolClient, QueryResultRow } from "pg";
import type { CardStatus } from "../cards/card.repository.js";

export interface PurchaseCard {
  id: string;
  status: CardStatus;
  balance: number;
  lowBalanceThreshold: number;
}

export interface PurchaseRecord {
  status: "APPROVED" | "REJECTED";
  rejectionReason: string | null;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
}

export interface PurchaseTransactionDataSource {
  findCardForUpdate(cardId: string): Promise<PurchaseCard | null>;
  findByRequestId(cardId: string, requestId: string): Promise<PurchaseRecord | null>;
  rejectPurchase(input: {
    cardId: string;
    amount: number;
    merchant: string;
    requestId: string;
    reason: "CARD_BLOCKED" | "INSUFFICIENT_FUNDS";
    balance: number;
  }): Promise<void>;
  createSecurityAlert(cardId: string): Promise<void>;
  approvePurchase(input: {
    cardId: string;
    amount: number;
    merchant: string;
    requestId: string;
    previousBalance: number;
    currentBalance: number;
  }): Promise<void>;
}

export interface PurchaseDataSource {
  withTransaction<T>(work: (transaction: PurchaseTransactionDataSource) => Promise<T>): Promise<T>;
}

interface PurchaseCardRow extends QueryResultRow {
  id: string;
  status: CardStatus;
  balance: string;
  low_balance_threshold: string;
}

interface PurchaseRecordRow extends QueryResultRow {
  status: "APPROVED" | "REJECTED";
  rejection_reason: string | null;
  amount: string;
  balance_before: string;
  balance_after: string;
}

class PgPurchaseTransactionRepository implements PurchaseTransactionDataSource {
  constructor(private readonly client: PoolClient) {}

  async findCardForUpdate(cardId: string): Promise<PurchaseCard | null> {
    const result = await this.client.query<PurchaseCardRow>(
      `SELECT id, status, balance, low_balance_threshold
       FROM cards WHERE id = $1 FOR UPDATE`,
      [cardId],
    );
    const row = result.rows[0];
    return row
      ? {
          id: row.id,
          status: row.status,
          balance: Number(row.balance),
          lowBalanceThreshold: Number(row.low_balance_threshold),
        }
      : null;
  }

  async findByRequestId(cardId: string, requestId: string): Promise<PurchaseRecord | null> {
    const result = await this.client.query<PurchaseRecordRow>(
      `SELECT status, rejection_reason, amount, balance_before, balance_after
       FROM transactions WHERE card_id = $1 AND request_id = $2`,
      [cardId, requestId],
    );
    const row = result.rows[0];
    return row
      ? {
          status: row.status,
          rejectionReason: row.rejection_reason,
          amount: Number(row.amount),
          balanceBefore: Number(row.balance_before),
          balanceAfter: Number(row.balance_after),
        }
      : null;
  }

  async rejectPurchase(input: {
    cardId: string;
    amount: number;
    merchant: string;
    requestId: string;
    reason: "CARD_BLOCKED" | "INSUFFICIENT_FUNDS";
    balance: number;
  }): Promise<void> {
    await this.client.query(
      `INSERT INTO transactions(
         id, card_id, amount, merchant, status, rejection_reason,
         request_id, balance_before, balance_after
       ) VALUES ($1, $2, $3, $4, 'REJECTED', $5, $6, $7, $7)`,
      [
        randomUUID(),
        input.cardId,
        input.amount,
        input.merchant,
        input.reason,
        input.requestId,
        input.balance,
      ],
    );
  }

  async createSecurityAlert(cardId: string): Promise<void> {
    await this.client.query(
      `INSERT INTO security_alerts(id, card_id, alert_type, message)
       VALUES ($1, $2, 'PURCHASE_ATTEMPT_BLOCKED_CARD', $3)`,
      [randomUUID(), cardId, "Intento de compra rechazado porque la tarjeta está bloqueada."],
    );
  }

  async approvePurchase(input: {
    cardId: string;
    amount: number;
    merchant: string;
    requestId: string;
    previousBalance: number;
    currentBalance: number;
  }): Promise<void> {
    await this.client.query(
      "UPDATE cards SET balance = $2, updated_at = NOW() WHERE id = $1",
      [input.cardId, input.currentBalance],
    );
    await this.client.query(
      `INSERT INTO transactions(
         id, card_id, amount, merchant, status, rejection_reason,
         request_id, balance_before, balance_after
       ) VALUES ($1, $2, $3, $4, 'APPROVED', NULL, $5, $6, $7)`,
      [
        randomUUID(),
        input.cardId,
        input.amount,
        input.merchant,
        input.requestId,
        input.previousBalance,
        input.currentBalance,
      ],
    );
  }
}

export class PgPurchaseRepository implements PurchaseDataSource {
  constructor(private readonly pool: Pool) {}

  async withTransaction<T>(
    work: (transaction: PurchaseTransactionDataSource) => Promise<T>,
  ): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query("BEGIN");
      const result = await work(new PgPurchaseTransactionRepository(client));
      await client.query("COMMIT");
      return result;
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }
}
