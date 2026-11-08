-- Todas as tabelas no MESMO banco: é o que permite gravar pedido e evento
-- em uma única transação local.

CREATE TABLE orders (
                        id          UUID          PRIMARY KEY,
                        customer_id UUID          NOT NULL,
                        status      VARCHAR(20)   NOT NULL,
                        total       NUMERIC(12,2) NOT NULL,
                        version     INT           NOT NULL,
                        created_at  TIMESTAMPTZ   NOT NULL
);

CREATE TABLE order_items (
                             order_id    UUID          NOT NULL REFERENCES orders (id),
                             line_number INT           NOT NULL,
                             sku         VARCHAR(50)   NOT NULL,
                             quantity    INT           NOT NULL CHECK (quantity > 0),
                             unit_price  NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
                             PRIMARY KEY (order_id, line_number)
);

CREATE TABLE outbox_events (
                               id                UUID         PRIMARY KEY,  -- eventId: viaja na mensagem e é a base da deduplicação
                               aggregate_type    VARCHAR(50)  NOT NULL,     -- 'Order'
                               aggregate_id      UUID         NOT NULL,     -- orderId: vira a chave da mensagem no Kafka
                               aggregate_version INT          NOT NULL,     -- versão do pedido depois do evento
                               event_type        VARCHAR(100) NOT NULL,     -- 'OrderCreated'
                               payload           JSONB        NOT NULL,     -- o envelope inteiro, como será publicado
                               created_at        TIMESTAMPTZ  NOT NULL,
                               published_at      TIMESTAMPTZ,               -- NULL = pendente
                               attempts          INT          NOT NULL DEFAULT 0,
                               last_error        TEXT
);

-- Índice parcial: só os pendentes. Mantém a busca do relay rápida
-- mesmo com milhões de eventos já publicados.
CREATE INDEX idx_outbox_pending ON outbox_events (created_at) WHERE published_at IS NULL;