package com.example.orders.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Corpo do POST /orders. */
public record CreateOrderRequest(
        @NotNull UUID customerId,
        @NotEmpty List<@Valid Item> items) {

    public record Item(
            @NotBlank String sku,
            @Positive int quantity,
            @NotNull @PositiveOrZero BigDecimal unitPrice) {
    }
}
