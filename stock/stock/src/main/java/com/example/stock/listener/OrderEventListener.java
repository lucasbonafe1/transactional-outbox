package com.example.stock.listener;

import com.example.stock.model.EventEnvelope;
import com.example.stock.model.StockDecision;
import com.example.stock.producer.StockEventPublisher;
import com.example.stock.service.StockReservationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

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
    public void onMessage(ConsumerRecord<String, String> record) {
        EventEnvelope event = jsonMapper.readValue(record.value(), EventEnvelope.class);
        log.info("Recebido {} eventId={} partição={} offset={}", event.eventType(), event.eventId(), record.partition(), record.offset());

        StockDecision decision = service.handle(event); // transação: inbox + efeito, commit ao retornar

        if (decision != null) {
            publisher.publish(event.eventId(), decision); // fora da transação, antes do ack do offset
        }
    }
}