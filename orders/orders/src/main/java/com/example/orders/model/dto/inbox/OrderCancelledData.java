package com.example.orders.model.dto.inbox;

import java.util.UUID;

public record OrderCancelledData(UUID orderId, UUID customerId, String reason) {
}
