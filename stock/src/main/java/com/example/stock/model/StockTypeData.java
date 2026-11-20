package com.example.stock.model;

import java.util.List;
import java.util.UUID;

public record StockTypeData(boolean reserved, UUID orderId, String reason, List<Item> items) {
    public record Item(String sku, int quantity) {
    }
}
