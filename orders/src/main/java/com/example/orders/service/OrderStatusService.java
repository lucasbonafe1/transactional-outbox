package com.example.orders.service;

import com.example.orders.model.EventEnvelope;
import com.example.orders.model.dto.inbox.OrderCancelledData;
import com.example.orders.model.dto.inbox.OrderConfirmedData;
import com.example.orders.model.enums.Status;
import com.example.orders.repository.InboxRepository;
import com.example.orders.repository.OrderRepository;
import com.example.orders.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    private final JdbcClient jdbc;
    private final OutboxRepository outbox;
    private final InboxRepository inboxRepository;
    private final JsonMapper jsonMapper;
    private final OrderRepository orderRepository;

    public OrderStatusService(JdbcClient jdbc, OutboxRepository outbox, InboxRepository inboxRepository, JsonMapper jsonMapper, OrderRepository orderRepository) {
        this.jdbc = jdbc;
        this.outbox = outbox;
        this.inboxRepository = inboxRepository;
        this.jsonMapper = jsonMapper;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void handle(UUID eventId, String type, UUID orderId, String reason) {
        if (!type.equals("StockReserved") && !type.equals("StockRejected")) {
            return;
        }

        // 1. Inbox
        boolean hasProcessed = inboxRepository.hasProcessedEventsSaved(eventId, type);
        if (!hasProcessed) {
            log.info("Evento {} já processado; ignorando", eventId);
            return;
        }

        if (type.equals("StockReserved")) {
            changeStatusAndRecordEvent(orderId, Status.CONFIRMED, null, "OrderConfirmed");
        } else {
            changeStatusAndRecordEvent(orderId, Status.CANCELLED, reason, "OrderCancelled");
        }
    }

    private void changeStatusAndRecordEvent(UUID orderId, Status status, String reason, String eventType) {
        orderRepository.changeStatusIfCreated(orderId, status).ifPresentOrElse(
                t -> outbox.insert(EventEnvelope.of(eventType, "Order", orderId, t.version(),
                        eventType.equals("OrderCancelled") ? new OrderCancelledData(orderId, t.customerId(), reason)
                                : new OrderConfirmedData(orderId, t.customerId(), t.total()))),
                () -> log.warn("Pedido {} não está em CREATED; mudança para {} ignorada", orderId, status));
    }
}