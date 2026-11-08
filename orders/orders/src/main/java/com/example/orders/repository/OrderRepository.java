package com.example.orders.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.orders.model.Order;
import com.example.orders.model.enums.Status;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Grava o pedido e os itens. Não abre transação: quem chama (OrderWriter) é que abre. */
    public void insert(Order order) {
        jdbc.sql("""
                INSERT INTO orders (id, customer_id, status, total, version, created_at)
                VALUES (:id, :customerId, :status, :total, :version, :createdAt)
                """)
                .param("id", order.id())
                .param("customerId", order.customerId())
                .param("status", order.status().name())
                .param("total", order.total())
                .param("version", order.version())
                .param("createdAt", order.createdAt())
                .update();

        int line = 1;
        for (Order.OrderItem item : order.items()) {
            jdbc.sql("""
                    INSERT INTO order_items (order_id, line_number, sku, quantity, unit_price)
                    VALUES (:orderId, :line, :sku, :quantity, :unitPrice)
                    """)
                    .param("orderId", order.id())
                    .param("line", line++)
                    .param("sku", item.sku())
                    .param("quantity", item.quantity())
                    .param("unitPrice", item.unitPrice())
                    .update();
        }
    }

    public Optional<Order> findById(UUID id) {
        List<Order.OrderItem> items = jdbc.sql("""
                SELECT sku, quantity, unit_price FROM order_items
                WHERE order_id = :id ORDER BY line_number
                """)
                .param("id", id)
                .query((rs, n) -> new Order.OrderItem(
                        rs.getString("sku"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price")))
                .list();

        return jdbc.sql("""
                SELECT id, customer_id, status, total, version, created_at
                FROM orders WHERE id = :id
                """)
                .param("id", id)
                .query((rs, n) -> new Order(
                        rs.getObject("id", UUID.class),
                        rs.getObject("customer_id", UUID.class),
                        Status.valueOf(rs.getString("status")),
                        rs.getBigDecimal("total"),
                        rs.getInt("version"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        items))
                .optional();
    }
}
