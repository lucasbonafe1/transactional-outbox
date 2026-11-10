package com.example.orders.service;

import com.example.orders.model.OutboxEvent;
import com.example.orders.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final String topic;
    private final int batchSize;
    private final long sendTimeoutMs;

    public OutboxPublisher(OutboxRepository outbox,
                           KafkaTemplate<String, String> kafka,
                           @Value("${outbox.topic}") String topic,
                           @Value("${outbox.relay.batch-size}") int batchSize,
                           @Value("${outbox.relay.send-timeout-ms}") long sendTimeoutMs) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
        this.batchSize = batchSize;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Transactional
    public int publishPending() {
        List<OutboxEvent> events = outbox.findPendingForUpdate(batchSize);
        int published = 0;

        for (OutboxEvent event : events) {
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.aggregateId().toString(), event.payload());

                record.headers().add("eventId", event.id().toString().getBytes(StandardCharsets.UTF_8));
                record.headers().add("eventType", event.eventType().getBytes(StandardCharsets.UTF_8));

                // Espera o ack do broker. Só depois disso o evento pode ser marcado como publicado.
                kafka.send(record).get(sendTimeoutMs, TimeUnit.MILLISECONDS);

                outbox.markPublished(event.id());
                published++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                String reason = String.valueOf(e.getMessage());
                outbox.recordFailure(event.id(), reason.length() > 500 ? reason.substring(0, 500) : reason);

                log.warn("Falha ao publicar o evento {} (tentativa {}): {}", event.id(), event.attempts() + 1, reason);
                break; // não passa para o próximo: preservaria a ordem quebrada
            }
        }
        return published;
    }
}
