package com.example.stock.producer;

import com.example.events.stock.ReservedItem;
import com.example.events.stock.StockRejected;
import com.example.events.stock.StockReserved;
import com.example.stock.model.StockDecision;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class StockEventPublisher {
    private final KafkaTemplate<String, Object> kafka;
    private final JsonMapper jsonMapper;
    private final String topic;

    public StockEventPublisher(KafkaTemplate<String, Object> kafka, JsonMapper jsonMapper, @Value("${stock.events.topic}") String topic) {
        this.kafka = kafka;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    public void publish(UUID inputEventId, StockDecision decision) {
        UUID eventId = UUID.nameUUIDFromBytes(("stock-decision:" + inputEventId).getBytes(StandardCharsets.UTF_8));
        String orderId = decision.orderId().toString();
        long occurredAt = decision.decidedAt().toInstant().toEpochMilli();

        String type;
        Object value;
        if (decision.reserved()) {
            type = "StockReserved";
            value = StockReserved.newBuilder()
                    .setEventId(eventId.toString()).setOrderId(orderId)
                    .setAggregateVersion(1).setOccurredAt(occurredAt)
                    .setItems(decision.items().stream()
                            .map(i -> ReservedItem.newBuilder().setSku(i.sku()).setQuantity(i.quantity()).build())
                            .toList())
                    .build();
        } else {
            type = "StockRejected";
            value = StockRejected.newBuilder()
                    .setEventId(eventId.toString()).setOrderId(orderId)
                    .setAggregateVersion(1).setOccurredAt(occurredAt)
                    .setReason(decision.reason())
                    .build();
        }

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, orderId, value);
        record.headers().add("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", type.getBytes(StandardCharsets.UTF_8));

        try {
            kafka.send(record).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao publicar " + type, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Falha ao publicar " + type + " do pedido " + orderId, e);
        }
    }
}
