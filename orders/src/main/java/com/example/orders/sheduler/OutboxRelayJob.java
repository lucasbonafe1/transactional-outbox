package com.example.orders.sheduler;

import com.example.orders.messaging.producer.OutboxPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayJob {
    private final OutboxPublisher publisher;

    public OutboxRelayJob(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms}")
    public void run() {
        publisher.publishPending();
    }
}