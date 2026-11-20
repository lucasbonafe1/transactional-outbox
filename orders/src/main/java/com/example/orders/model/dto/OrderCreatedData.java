package com.example.orders.model.dto;

import com.example.orders.model.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedData(UUID customerId, BigDecimal total, List<Item> items) {

    public record Item(String sku, int quantity, BigDecimal unitPrice) {}

    public static OrderCreatedData from(Order order) {
        return new OrderCreatedData(
                order.customerId(),
                order.total(),
                order.items().stream()
                        .map(i -> new Item(i.sku(), i.quantity(), i.unitPrice()))
                        .toList());
    }
}