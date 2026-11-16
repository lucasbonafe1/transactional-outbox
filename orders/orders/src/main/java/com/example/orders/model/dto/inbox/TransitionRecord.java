package com.example.orders.model.dto.inbox;

import java.math.BigDecimal;
import java.util.UUID;

public record TransitionRecord(int version, UUID customerId, BigDecimal total) {
}
