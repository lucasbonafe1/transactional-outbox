package com.example.stock.repository;

import com.example.stock.model.EventEnvelope;
import com.example.stock.model.StatusEnum;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class StockReservationRepository {
    private final JdbcClient jdbc;

    public StockReservationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertEventIfWasNotProcessed(EventEnvelope event) {
        return jdbc.sql("""
                    INSERT INTO processed_events (event_id, event_type)
                    VALUES (:id, :type)
                    ON CONFLICT (event_id) DO NOTHING
                """)
                .param("id", event.eventId())
                .param("type", event.eventType())
                .update() > 0;
    }

    /**
     * Retorna o estoque disponível para os SKUs informados, travando as linhas lidas até o fim da transação.
     *
     * @param skus Conjunto de códigos SKU a serem consultados.
     * @return Map em que a chave é o código SKU e o valor é a quantidade disponível.
     */
    public Map<String, Integer> findAvailableStockForSkus(Iterable<String> skus) {
        return jdbc.sql("""
                    SELECT sku, available FROM stock_items
                    WHERE sku IN (:skus) ORDER BY sku FOR UPDATE
                 """)
                .param("skus", skus)
                .query((rs, n) -> Map.entry(rs.getString("sku"), rs.getInt("available")))
                .list().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
    * Atualiza o estoque disponível de um SKU, subtraindo a quantidade reservada de acordo com o pedido criado.
    *
    * @param sku O código SKU do produto a ser atualizado.
    * @param qty A quantidade a ser subtraída do estoque disponível.
    */
    public void updateStock(String sku, int qty) {
        jdbc.sql("""
                    UPDATE stock_items SET available = available - :qty WHERE sku = :sku
                 """)
                .param("qty", qty).param("sku", sku)
                .update();
    }

    public void saveReservation(UUID orderId, String status, String reason) {
        jdbc.sql("""
                   INSERT INTO stock_reservations (order_id, status, reason) VALUES (:id, :status, :reason)
                 """)
                .param("id", orderId)
                .param("status", status)
                .param("reason", reason)
                .update();
    }
}
