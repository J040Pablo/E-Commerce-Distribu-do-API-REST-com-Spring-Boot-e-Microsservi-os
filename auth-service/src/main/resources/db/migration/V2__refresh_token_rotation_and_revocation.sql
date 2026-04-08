-- Add metadata required for refresh token rotation + persistent revocation
ALTER TABLE refresh_tokens
    ADD COLUMN token_hash CHAR(64) NULL,
    ADD COLUMN jwt_id VARCHAR(64) NULL,
    ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN revoked_at TIMESTAMP NULL,
    ADD COLUMN revoke_reason VARCHAR(100) NULL,
    ADD COLUMN replaced_by_jwt_id VARCHAR(64) NULL,
    ADD COLUMN last_used_at TIMESTAMP NULL;

-- Backfill legacy rows
UPDATE refresh_tokens
SET token_hash = SHA2(token, 256),
    jwt_id = CONCAT('legacy-', id)
WHERE token_hash IS NULL;

ALTER TABLE refresh_tokens
    MODIFY COLUMN token_hash CHAR(64) NOT NULL,
    MODIFY COLUMN jwt_id VARCHAR(64) NOT NULL;

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE UNIQUE INDEX uk_refresh_tokens_jwt_id ON refresh_tokens(jwt_id);
CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);
CREATE INDEX idx_refresh_tokens_last_used ON refresh_tokens(last_used_at);
