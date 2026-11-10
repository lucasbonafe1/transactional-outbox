package com.example.orders.sheduler;

import com.example.orders.service.OutboxPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayScheduler {
    private final OutboxPublisher publisher;

    public OutboxRelayScheduler(OutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.interval-ms}")
    public void run() {
        publisher.publishPending();
    }
}