package com.example.stock.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedData(UUID customerId, BigDecimal total, List<Item> items) {

    public record Item(String sku, int quantity, BigDecimal unitPrice) {
    }
}