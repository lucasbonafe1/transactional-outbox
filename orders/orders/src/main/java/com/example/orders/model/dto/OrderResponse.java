package com.example.orders.model.dto;

import com.example.orders.model.Order;
import com.example.orders.model.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID customerId,
        Status status,
        BigDecimal total,
        int version,
        OffsetDateTime createdAt,
        List<Item> items) {

    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.items().stream()
                .map(i -> new Item(i.sku(), i.quantity(), i.unitPrice()))
                .toList();
        return new OrderResponse(order.id(), order.customerId(), order.status(), order.total(),
                order.version(), order.createdAt(), items);
    }
}
