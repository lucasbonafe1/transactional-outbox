package com.example.stock.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record StockDecision (
        UUID orderId,
        StatusEnum status,
        String reason,
        OffsetDateTime decidedAt,
        List<StockTypeData.Item> items) {

    public boolean reserved() {
        return StatusEnum.RESERVED.equals(status);
    }
}
