package com.example.orders.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OutboxEvent(
        UUID id,
        String aggregateType,
        UUID aggregateId,
        int aggregateVersion,
        String eventType,
        String payload,
        OffsetDateTime createdAt,
        int attempts) {
}