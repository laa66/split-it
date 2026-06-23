-- Split-it schema. Loaded by postgres on first start (docker-entrypoint-initdb.d).
-- No Flyway. Reset with `make db-reset`.
-- Amounts always NUMERIC(12,2), never FLOAT. Balances calculated live, never persisted.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(100) NOT NULL,
    password_hash VARCHAR(72)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE groups (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_by  UUID         NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE group_members (
    id       UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID        NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id  UUID        NOT NULL REFERENCES users(id),
    role     VARCHAR(10) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);

CREATE TABLE expenses (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     UUID           NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    paid_by      UUID           NOT NULL REFERENCES users(id),
    title        VARCHAR(200)   NOT NULL,
    amount       NUMERIC(12,2)  NOT NULL CHECK (amount > 0),
    split_type   VARCHAR(20)    NOT NULL CHECK (split_type IN ('EQUAL', 'PERCENTAGE', 'AMOUNT')),
    expense_date DATE           NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE TABLE expense_shares (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id   UUID          NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id      UUID          NOT NULL REFERENCES users(id),
    share_amount NUMERIC(12,2) NOT NULL CHECK (share_amount >= 0),
    UNIQUE (expense_id, user_id)
);

CREATE TABLE invitations (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id      UUID         NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    invited_email VARCHAR(255) NOT NULL,
    invited_by    UUID         NOT NULL REFERENCES users(id),
    token         UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    status        VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED')),
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Two-sided settlement confirmation (scenariusz 3.10).
-- Settlement is fully settled only when both payer and payee confirm.
-- settled_at is set when status transitions to CONFIRMED.
CREATE TABLE settlements (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id           UUID          NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    payer_id           UUID          NOT NULL REFERENCES users(id),
    payee_id           UUID          NOT NULL REFERENCES users(id),
    amount             NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status             VARCHAR(10)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED')),
    confirmed_by_payer BOOLEAN       NOT NULL DEFAULT FALSE,
    confirmed_by_payee BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    settled_at         TIMESTAMPTZ,
    CHECK (payer_id <> payee_id)
);

CREATE INDEX idx_group_members_user    ON group_members (user_id);
CREATE INDEX idx_group_members_group   ON group_members (group_id);
CREATE INDEX idx_expenses_group        ON expenses (group_id);
CREATE INDEX idx_expenses_group_date   ON expenses (group_id, expense_date DESC);
CREATE INDEX idx_expense_shares_expense ON expense_shares (expense_id);
CREATE INDEX idx_invitations_token     ON invitations (token);
CREATE UNIQUE INDEX idx_invitations_unique_pending ON invitations (group_id, invited_email) WHERE status = 'PENDING';
CREATE INDEX idx_settlements_group     ON settlements (group_id);
