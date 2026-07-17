CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS cards (
    id UUID PRIMARY KEY,
    user_id VARCHAR NOT NULL,
    last_four VARCHAR(4) NOT NULL CHECK (last_four ~ '^[0-9]{4}$'),
    status VARCHAR NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    balance BIGINT NOT NULL CHECK (balance >= 0),
    low_balance_threshold BIGINT NOT NULL CHECK (low_balance_threshold > 0),
    blocked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (
        (status = 'ACTIVE' AND blocked_at IS NULL)
        OR (status = 'BLOCKED' AND blocked_at IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES cards(id),
    amount BIGINT NOT NULL CHECK (amount > 0),
    merchant VARCHAR NOT NULL CHECK (BTRIM(merchant) <> ''),
    status VARCHAR NOT NULL CHECK (status IN ('APPROVED', 'REJECTED')),
    rejection_reason VARCHAR NULL,
    request_id VARCHAR NULL,
    balance_before BIGINT NOT NULL CHECK (balance_before >= 0),
    balance_after BIGINT NOT NULL CHECK (balance_after >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (
        (status = 'APPROVED' AND rejection_reason IS NULL)
        OR (status = 'REJECTED' AND rejection_reason IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS transactions_card_request_id_unique
    ON transactions(card_id, request_id)
    WHERE request_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS security_alerts (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES cards(id),
    alert_type VARCHAR NOT NULL,
    message VARCHAR NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
