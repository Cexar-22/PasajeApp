import type { Pool, QueryResultRow } from "pg";

export type CardStatus = "ACTIVE" | "BLOCKED";

export interface Card {
  id: string;
  lastFour: string;
  status: CardStatus;
  balance: number;
  lowBalanceThreshold: number;
  blockedAt: string | null;
}

interface CardRow extends QueryResultRow {
  id: string;
  last_four: string;
  status: CardStatus;
  balance: string;
  low_balance_threshold: string;
  blocked_at: Date | null;
}

export interface CardDataSource {
  findById(cardId: string): Promise<Card | null>;
  block(cardId: string): Promise<Card | null>;
  updateThreshold(cardId: string, threshold: number): Promise<Card | null>;
}

function mapCard(row: CardRow): Card {
  return {
    id: row.id,
    lastFour: row.last_four,
    status: row.status,
    balance: Number(row.balance),
    lowBalanceThreshold: Number(row.low_balance_threshold),
    blockedAt: row.blocked_at?.toISOString() ?? null,
  };
}

const cardProjection = `
  id, last_four, status, balance, low_balance_threshold, blocked_at
`;

export class PgCardRepository implements CardDataSource {
  constructor(private readonly pool: Pool) {}

  async findById(cardId: string): Promise<Card | null> {
    const result = await this.pool.query<CardRow>(
      `SELECT ${cardProjection} FROM cards WHERE id = $1`,
      [cardId],
    );
    return result.rows[0] ? mapCard(result.rows[0]) : null;
  }

  async block(cardId: string): Promise<Card | null> {
    const result = await this.pool.query<CardRow>(
      `UPDATE cards
       SET status = 'BLOCKED',
           blocked_at = COALESCE(blocked_at, NOW()),
           updated_at = NOW()
       WHERE id = $1
       RETURNING ${cardProjection}`,
      [cardId],
    );
    return result.rows[0] ? mapCard(result.rows[0]) : null;
  }

  async updateThreshold(cardId: string, threshold: number): Promise<Card | null> {
    const result = await this.pool.query<CardRow>(
      `UPDATE cards
       SET low_balance_threshold = $2, updated_at = NOW()
       WHERE id = $1
       RETURNING ${cardProjection}`,
      [cardId, threshold],
    );
    return result.rows[0] ? mapCard(result.rows[0]) : null;
  }
}
