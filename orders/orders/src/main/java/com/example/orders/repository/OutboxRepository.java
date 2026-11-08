package com.example.orders.repository;

import com.example.orders.model.EventEnvelope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class OutboxRepository {

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;

    public OutboxRepository(JdbcClient jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    /** Não abre transação: precisa participar da transação de quem chama. */
    public void insert(EventEnvelope event) {
        jdbc.sql("""
                INSERT INTO outbox_events
                    (id, aggregate_type, aggregate_id, aggregate_version, event_type, payload, created_at)
                VALUES
                    (:id, :aggregateType, :aggregateId, :aggregateVersion, :eventType,
                     CAST(:payload AS jsonb), :createdAt)
                """)
                .param("id", event.eventId())
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("aggregateVersion", event.aggregateVersion())
                .param("eventType", event.eventType())
                .param("payload", jsonMapper.writeValueAsString(event))
                .param("createdAt", event.occurredAt())
                .update();
    }
}