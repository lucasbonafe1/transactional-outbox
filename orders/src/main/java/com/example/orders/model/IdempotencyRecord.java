package com.example.orders.model;

import java.util.UUID;

public record IdempotencyRecord(String key, String requestHash, UUID orderId, String responseBody) {
}
