-- NULL keeps historical requests compatible; new API requests always store a key.
ALTER TABLE relief_request ADD COLUMN idempotency_key VARCHAR(100) COLLATE utf8mb4_bin NULL;
ALTER TABLE relief_request ADD UNIQUE KEY uk_request_user_idempotency (requester_id, idempotency_key);
CREATE INDEX idx_request_user_created ON relief_request(requester_id, created_at, id);
CREATE INDEX idx_request_created ON relief_request(created_at, id);
