package com.example.orders.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        int aggregateVersion,
        OffsetDateTime occurredAt,
        Object data) {

    public static EventEnvelope of(String eventType, String aggregateType, UUID aggregateId,
                                   int aggregateVersion, Object data) {
        return new EventEnvelope(UUID.randomUUID(), eventType, aggregateType, aggregateId,
                aggregateVersion, OffsetDateTime.now(ZoneOffset.UTC), data);
    }
}