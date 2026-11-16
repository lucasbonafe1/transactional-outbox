package com.example.orders.listener;

import com.example.orders.model.dto.inbox.InboundEvent;
import com.example.orders.service.OrderStatusService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;


@Component
public class StockEventListener {

    private static final Logger log = LoggerFactory.getLogger(StockEventListener.class);

    private final JsonMapper jsonMapper;
    private final OrderStatusService service;

    public StockEventListener(JsonMapper jsonMapper, OrderStatusService service) {
        this.jsonMapper = jsonMapper;
        this.service = service;
    }

    @KafkaListener(topics = "${orders.stock-events.topic}")
    public void onMessage(ConsumerRecord<String, String> record) {
        InboundEvent event = jsonMapper.readValue(record.value(), InboundEvent.class);

        log.info("Recebido {} eventId={} pedido={}", event.eventType(), event.eventId(), event.aggregateId());
        service.handle(event);
    }
}