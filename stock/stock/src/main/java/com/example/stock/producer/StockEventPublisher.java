package com.example.stock.producer;

import com.example.stock.model.PublishedEvent;
import com.example.stock.model.StockDecision;
import com.example.stock.model.StockTypeData;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Component
public class StockEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final JsonMapper jsonMapper;
    private final String topic;

    public StockEventPublisher(KafkaTemplate<String, String> kafka, JsonMapper jsonMapper, @Value("${stock.events.topic}") String topic) {
        this.kafka = kafka;
        this.jsonMapper = jsonMapper;
        this.topic = topic;
    }

    public void publish(UUID inputEventId, StockDecision decision) {
        // Determinístico: o mesmo evento de entrada sempre gera o mesmo eventId de saída.
        UUID eventId = UUID.nameUUIDFromBytes(("stock-decision:" + inputEventId).getBytes(StandardCharsets.UTF_8));

        Object data = new StockTypeData(decision.reserved(), decision.orderId(), decision.reason(), decision.items());

        String type = decision.reserved() ? "StockReserved" : "StockRejected";

        PublishedEvent event = new PublishedEvent(eventId, type, "Reservation",
                decision.orderId(), 1, decision.decidedAt(), data);

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, decision.orderId().toString(), jsonMapper.writeValueAsString(event));
        record.headers().add("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", type.getBytes(StandardCharsets.UTF_8));

        try {
        kafka.send(record);
        } catch (IllegalStateException e){
            throw new IllegalStateException("Falha ao publicar " + type + " do pedido " + decision.orderId(), e);
        }
    }
}
