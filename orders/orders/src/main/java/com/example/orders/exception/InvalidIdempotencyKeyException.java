package com.example.orders.exception;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }
}
