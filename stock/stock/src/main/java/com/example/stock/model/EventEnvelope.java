package com.example.stock.model;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        int aggregateVersion,
        OffsetDateTime occurredAt,
        JsonNode data) {
}