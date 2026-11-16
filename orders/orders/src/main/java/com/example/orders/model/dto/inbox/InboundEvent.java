package com.example.orders.model.dto.inbox;

import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InboundEvent(
        UUID eventId,
        String eventType,
        String aggregateType,
        UUID aggregateId,        // o orderId
        int aggregateVersion,
        OffsetDateTime occurredAt,
        JsonNode data) {
}
