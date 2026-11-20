CREATE TABLE stock_items (
                             sku       VARCHAR(50) PRIMARY KEY,
                             available INT         NOT NULL CHECK (available >= 0)   -- última defesa contra estoque negativo
);

CREATE TABLE stock_reservations (
                                    order_id   UUID         PRIMARY KEY,
                                    status     VARCHAR(20)  NOT NULL,                       -- RESERVED | REJECTED
                                    reason     TEXT,
                                    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- A inbox: um registro por evento já processado.
CREATE TABLE processed_events (
                                  event_id     UUID         PRIMARY KEY,
                                  event_type   VARCHAR(100) NOT NULL,
                                  processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Dados de laboratório, para ter o que reservar.
INSERT INTO stock_items (sku, available) VALUES ('CAMISETA-P', 100), ('BONE-AZUL', 50);