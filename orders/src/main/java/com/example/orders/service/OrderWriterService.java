package com.example.orders.service;

import com.example.orders.model.EventEnvelope;
import com.example.orders.model.IdempotencyRecord;
import com.example.orders.model.Order;
import com.example.orders.model.dto.CreateOrderRequest;
import com.example.orders.model.dto.OrderCreatedData;
import com.example.orders.model.dto.OrderResponse;
import com.example.orders.repository.IdempotencyKeyRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.OutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Component
public class OrderWriterService {
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final JsonMapper jsonMapper;

    public OrderWriterService(OrderRepository orderRepository, OutboxRepository outboxRepository, IdempotencyKeyRepository idempotencyKeys, JsonMapper jsonMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.idempotencyKeys = idempotencyKeys;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public OrderResponse createOrder(String key, String requestHash, CreateOrderRequest request) {
        Order order = Order.create(UUID.randomUUID(), request);
        OrderResponse response = OrderResponse.from(order);

        idempotencyKeys.insert(new IdempotencyRecord(
                key, requestHash, order.id(), jsonMapper.writeValueAsString(response)));
        orderRepository.insert(order);
        outboxRepository.insert(EventEnvelope.of(
                "OrderCreated", "Order", order.id(), order.version(), OrderCreatedData.from(order)));

        return response;
    }
}
