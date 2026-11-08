package com.example.orders.model.dto;

public record PlaceOrderResult(OrderResponse order, boolean replayed) {
}