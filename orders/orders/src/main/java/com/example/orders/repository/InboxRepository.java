package com.example.orders.repository;

import com.example.orders.model.dto.inbox.InboundEvent;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InboxRepository {
    private final JdbcClient jdbc;

    public InboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasProcessedEventsSaved(InboundEvent event, String type) {
        return jdbc.sql("""
                INSERT INTO processed_events (event_id, event_type)
                VALUES (:id, :type)
                ON CONFLICT (event_id) DO NOTHING
                """)
                .param("id", event.eventId())
                .param("type", type)
                .update() > 0;
    }
}
