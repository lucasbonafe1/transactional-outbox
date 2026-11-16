package com.example.stock.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public record PublishedEvent(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        int aggregateVersion,
        OffsetDateTime occurredAt,
        Object data) {

    public static PublishedEvent of(String eventType, String aggregateType, UUID aggregateId, int aggregateVersion, Object data) {
        return new PublishedEvent(UUID.randomUUID(), eventType, aggregateType, aggregateId,
                aggregateVersion, OffsetDateTime.now(ZoneOffset.UTC), data);
    }
}
