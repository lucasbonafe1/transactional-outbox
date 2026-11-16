package com.example.orders.model.dto.inbox;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderConfirmedData(UUID orderId, UUID customerId, BigDecimal total) {
}
