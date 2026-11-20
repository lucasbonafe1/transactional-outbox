package com.example.orders.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class InboxRepository {
    private final JdbcClient jdbc;

    public InboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasProcessedEventsSaved(UUID eventId, String type) {
        return jdbc.sql("""
                INSERT INTO processed_events (event_id, event_type)
                VALUES (:id, :type)
                ON CONFLICT (event_id) DO NOTHING
                """)
                .param("id", eventId)
                .param("type", type)
                .update() > 0;
    }
}
