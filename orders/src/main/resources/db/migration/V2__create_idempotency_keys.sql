CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,   -- SHA-256 do corpo, para detectar reuso da chave
    order_id        UUID         NOT NULL REFERENCES orders (id) DEFERRABLE INITIALLY DEFERRED,
    response_body   JSONB        NOT NULL,   -- resposta devolvida em uma repetição
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);