package com.example.orders.repository;

import com.example.orders.model.IdempotencyRecord;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class IdempotencyKeyRepository {

    private final JdbcClient jdbc;

    public IdempotencyKeyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<IdempotencyRecord> findByKey(String key) {
        return jdbc.sql("""
                SELECT idempotency_key, request_hash, order_id, CAST(response_body AS text) AS response_body
                FROM idempotency_keys WHERE idempotency_key = :key
                """)
                .param("key", key)
                .query((rs, n) -> new IdempotencyRecord(
                        rs.getString("idempotency_key"),
                        rs.getString("request_hash"),
                        rs.getObject("order_id", UUID.class),
                        rs.getString("response_body")))
                .optional();
    }

    /** A chave primária resolve a concorrência: a segunda requisição espera a primeira
     *  terminar e, se ela confirmar, recebe DuplicateKeyException. */
    public void insert(IdempotencyRecord record) {
        jdbc.sql("""
                INSERT INTO idempotency_keys (idempotency_key, request_hash, order_id, response_body)
                VALUES (:key, :requestHash, :orderId, CAST(:responseBody AS jsonb))
                """)
                .param("key", record.key())
                .param("requestHash", record.requestHash())
                .param("orderId", record.orderId())
                .param("responseBody", record.responseBody())
                .update();
    }
}