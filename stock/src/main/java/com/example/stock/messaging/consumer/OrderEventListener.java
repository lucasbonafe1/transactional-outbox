package com.example.stock.messaging.producer.listener;

import com.example.events.orders.OrderCreated;
import com.example.stock.model.ItemLine;
import com.example.stock.model.StockDecision;
import com.example.stock.messaging.producer.StockEventPublisher;
import com.example.stock.service.StockReservationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final JsonMapper jsonMapper;
    private final StockReservationService service;
    private final StockEventPublisher publisher;

    public OrderEventListener(JsonMapper jsonMapper, StockReservationService service, StockEventPublisher publisher) {
        this.jsonMapper = jsonMapper;
        this.service = service;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${stock.topic}")
    public void onMessage(ConsumerRecord<String, Object> record) {
        // O tópico também tem OrderConfirmed/OrderCancelled: este serviço só age sobre OrderCreated.
        if (!(record.value() instanceof OrderCreated event)) {
            log.debug("Tipo ignorado no tópico {}", record.topic());
            return;
        }

        UUID eventId = UUID.fromString(event.getEventId());
        UUID orderId = UUID.fromString(event.getOrderId());
        List<ItemLine> items = event.getItems().stream()
                .map(i -> new ItemLine(i.getSku(), i.getQuantity()))
                .toList();

        StockDecision decision = service.handle(eventId, orderId, items);
        publisher.publish(eventId, decision);
    }
}