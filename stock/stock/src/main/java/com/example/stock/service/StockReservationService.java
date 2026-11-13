package com.example.stock.service;

import com.example.stock.model.EventEnvelope;
import com.example.stock.model.OrderCreatedData;
import com.example.stock.model.StatusEnum;
import com.example.stock.repository.StockReservationRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.web.servlet.function.RequestPredicates.param;

@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final JsonMapper jsonMapper;

    private final StockReservationRepository repository;

    public StockReservationService(StockReservationRepository repository, JsonMapper jsonMapper) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public void handle(EventEnvelope event) {
        if (!event.eventType().equals("OrderCreated")) {
            return;
        }

        // Verifica na tabela Inbox se o evento já foi processado.
        boolean wasProcessed = repository.eventWasProcessed(event);
        if (wasProcessed) {
            log.info("Evento {} já processado; ignorando (entrega duplicada)", event.eventId());
            return;
        }

        // map para class OrderCreatedData
        OrderCreatedData data = jsonMapper.treeToValue(event.data(), OrderCreatedData.class);
        reserve(event.aggregateId(), data.items());
    }

    private void reserve(UUID orderId, List<OrderCreatedData.Item> orderItems) {
        // Soma quantidades por codigo SKU e ordena: travar as linhas sempre na mesma ordem evita deadlock.
        Map<String, Integer> productsBySkuAndQuantity = new TreeMap<>();
        orderItems.forEach(
                i -> productsBySkuAndQuantity.merge(i.sku(), i.quantity(), Integer::sum)
        );

        Map<String, Integer> available = repository.findAvailableStockForSkus(productsBySkuAndQuantity.keySet());

        List<String> skusWithNoStock = getListOfSkusWithInsufficientStock(productsBySkuAndQuantity, available);

        if (!skusWithNoStock.isEmpty()) {
            String reason = "Estoque insuficiente: " + String.join(", ", skusWithNoStock);

            repository.saveReservation(orderId, StatusEnum.REJECTED, reason);
            log.info("Pedido {} REJEITADO: estoque insuficiente para {}", orderId, skusWithNoStock);
            return;
        }

        productsBySkuAndQuantity.forEach(repository::updateStock);

        repository.saveReservation(orderId, StatusEnum.RESERVED, null);
        log.info("Pedido {} RESERVADO: {}", orderId, productsBySkuAndQuantity);
    }

    private static List<String> getListOfSkusWithInsufficientStock(Map<String, Integer> productsBySkuAndQuantity, Map<String, Integer> available) {
        return productsBySkuAndQuantity.entrySet().stream()
                .filter(e -> available.getOrDefault(e.getKey(), 0) < e.getValue())
                .map(Map.Entry::getKey)
                .toList();
    }
}