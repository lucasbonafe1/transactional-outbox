package com.example.orders.listener;

import com.example.events.stock.StockRejected;
import com.example.events.stock.StockReserved;
import com.example.orders.service.OrderStatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);
    private final OrderStatusService service;

    public StockEventListener(OrderStatusService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${orders.stock-events.topic}")
    public void onMessage(ConsumerRecord<String, Object> record) {
        switch (record.value()) {
            case StockReserved e -> service.handle(
                    UUID.fromString(e.getEventId()), "StockReserved", UUID.fromString(e.getOrderId()), null
            );
            case StockRejected e -> service.handle(
                    UUID.fromString(e.getEventId()), "StockRejected", UUID.fromString(e.getOrderId()), e.getReason()
            );
            case null, default -> log.warn("Mensagem inesperada em {}: {}", record.topic(), record.value());
        }
    }
}