package com.example.orders.repository;

import com.example.orders.model.EventEnvelope;
import com.example.orders.model.OutboxEvent;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
    /** O FOR UPDATE SKIP LOCKED trava as linhas lidas até o fim da transação e pula as que outro relay já travou. */
    public List<OutboxEvent> findPendingForUpdate(int batchSize) {
        return jdbc.sql("""
            SELECT id, aggregate_type, aggregate_id, aggregate_version, event_type,
                   CAST(payload AS text) AS payload, created_at, attempts
            FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """)
                .param("batchSize", batchSize)
                .query((rs, n) -> new OutboxEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getInt("aggregate_version"),
                        rs.getString("event_type"),
                        rs.getString("payload"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        rs.getInt("attempts")))
                .list();
    }

    public void markPublished(UUID id) {
        jdbc.sql("UPDATE outbox_events SET published_at = now() WHERE id = :id")
                .param("id", id)
                .update();
    }

    public void recordFailure(UUID id, String error) {
        jdbc.sql("UPDATE outbox_events SET attempts = attempts + 1, last_error = :error WHERE id = :id")
                .param("id", id)
                .param("error", error)
                .update();
    }

}