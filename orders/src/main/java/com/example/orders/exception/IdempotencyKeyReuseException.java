package com.example.orders.exception;

public class IdempotencyKeyReuseException extends RuntimeException {
    public IdempotencyKeyReuseException(String key) {
        super("A Idempotency-Key '" + key + "' já foi usada com um pedido diferente");
    }
}
