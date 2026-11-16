package com.example.orders.model.dto.inbox;

import java.util.UUID;

public record StockRejectedData(UUID orderId, String reason) {
}
