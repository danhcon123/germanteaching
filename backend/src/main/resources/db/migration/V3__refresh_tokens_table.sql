-- V3__create_refresh_tokens_table.sql
CREATE TABLE refresh_tokens (
    id            BIGSERIAL            PRIMARY KEY,
    token         VARCHAR(255)         NOT NULL UNIQUE,
    expiry_date   TIMESTAMPTZ          NOT NULL,
    created_at    TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    user_id       INTEGER              NOT NULL,
    is_revoked    BOOLEAN              NOT NULL DEFAULT FALSE,
    revoked_at    TIMESTAMPTZ,

    CONSTRAINT fk_refresh_token_user
      FOREIGN KEY (user_id)
      REFERENCES users(user_id)
      ON DELETE CASCADE
);