package com.example.orders.mapper;

import com.example.events.orders.OrderCancelled;
import com.example.events.orders.OrderConfirmed;
import com.example.events.orders.OrderCreated;
import com.example.events.orders.OrderLine;
import com.example.orders.model.OutboxEvent;
import com.example.orders.model.dto.OrderCreatedData;
import com.example.orders.model.dto.inbox.OrderCancelledData;
import com.example.orders.model.dto.inbox.OrderConfirmedData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OrderEventAvroMapper {

    /** Só interessa o campo "data" do envelope JSON guardado na outbox. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stored(JsonNode data) {
    }

    private final JsonMapper jsonMapper;

    public OrderEventAvroMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Object toAvro(OutboxEvent e) {
        JsonNode data = jsonMapper.readValue(e.payload(), Stored.class).data();
        String eventId = e.id().toString();
        String orderId = e.aggregateId().toString();
        long occurredAt = e.createdAt().toInstant().toEpochMilli();

        return switch (e.eventType()) {
            case "OrderCreated" -> {
                OrderCreatedData d = jsonMapper.treeToValue(data, OrderCreatedData.class);
                yield OrderCreated.newBuilder()
                        .setEventId(eventId).setOrderId(orderId)
                        .setAggregateVersion(e.aggregateVersion()).setOccurredAt(occurredAt)
                        .setCustomerId(d.customerId().toString())
                        .setTotal(money(d.total()))
                        .setItems(d.items().stream()
                                .map(i -> OrderLine.newBuilder()
                                        .setSku(i.sku()).setQuantity(i.quantity())
                                        .setUnitPrice(money(i.unitPrice())).build())
                                .toList())
                        .build();
            }
            case "OrderConfirmed" -> {
                OrderConfirmedData d = jsonMapper.treeToValue(data, OrderConfirmedData.class);
                yield OrderConfirmed.newBuilder()
                        .setEventId(eventId).setOrderId(orderId)
                        .setAggregateVersion(e.aggregateVersion()).setOccurredAt(occurredAt)
                        .setCustomerId(d.customerId().toString())
                        .setTotal(money(d.total()))
                        .build();
            }
            case "OrderCancelled" -> {
                OrderCancelledData d = jsonMapper.treeToValue(data, OrderCancelledData.class);
                yield OrderCancelled.newBuilder()
                        .setEventId(eventId).setOrderId(orderId)
                        .setAggregateVersion(e.aggregateVersion()).setOccurredAt(occurredAt)
                        .setCustomerId(d.customerId().toString())
                        .setReason(d.reason())
                        .build();
            }
            default -> throw new IllegalArgumentException("Sem contrato Avro para o evento " + e.eventType());
        };
    }

    /** Sempre com 2 casas: 359.5 vira "359.50". */
    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}