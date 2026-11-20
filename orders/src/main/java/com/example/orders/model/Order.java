package com.example.orders.model;

import com.example.orders.model.dto.CreateOrderRequest;
import com.example.orders.model.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public record Order(
        UUID id,
        UUID customerId,
        Status status,
        BigDecimal total,
        int version,
        OffsetDateTime createdAt,
        List<OrderItem> items) {

    public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {
    }

    /**
     * Monta um pedido novo a partir da requisição. O total é sempre calculado aqui, no servidor.
     */
    public static Order create(UUID id, CreateOrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.sku(), i.quantity(), i.unitPrice()))
                .toList();

        BigDecimal total = items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Truncado em microssegundos, a precisão do PostgreSQL, para que a resposta
        // devolvida agora seja idêntica ao que for lido do banco depois.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        return new Order(id, request.customerId(), Status.CREATED, total, 1, now, items);
    }
}