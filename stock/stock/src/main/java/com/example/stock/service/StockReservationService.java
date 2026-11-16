package com.example.stock.service;

import com.example.stock.model.*;
import com.example.stock.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

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
    public StockDecision handle(EventEnvelope event) {
        if (!event.eventType().equals("OrderCreated")) return null;

        UUID orderId = event.aggregateId();
        OrderCreatedData data = jsonMapper.treeToValue(event.data(), OrderCreatedData.class);

        // Verifica na tabela Inbox e salva se o evento ainda não existe.
        boolean hasBeenSaved = repository.insertEventIfWasNotProcessed(event);
        if (!hasBeenSaved) {
            log.info("Evento {} já processado; ignorando (entrega duplicada)", event.eventId());
            return loadDecision(orderId, data.items());
        }

        reserve(orderId, data.items());
        return loadDecision(orderId, data.items());
    }

    private StockDecision loadDecision(UUID orderId, List<OrderCreatedData.Item> items) {
        List<StockTypeData.Item> summed = sumBySkuAndQuantity(items).entrySet().stream()
                .map(e -> new StockTypeData.Item(e.getKey(), e.getValue()))
                .toList();

        return repository.getStockDecisionByOrderId(orderId, summed);
    }

    // Soma quantidades por codigo SKU e ordena: travar as linhas sempre na mesma ordem evita deadlock.
    private Map<String, Integer> sumBySkuAndQuantity(List<OrderCreatedData.Item> items) {
        Map<String, Integer> needed = new TreeMap<>();
        items.forEach(i -> needed.merge(i.sku(), i.quantity(), Integer::sum));
        return needed;
    }

    private void reserve(UUID orderId, List<OrderCreatedData.Item> orderItems) {
        Map<String, Integer> productsBySkuAndQuantity = sumBySkuAndQuantity(orderItems);

        Map<String, Integer> itemsWithStock = repository.findAvailableStockForSkus(productsBySkuAndQuantity.keySet());

        List<String> skusWithNoStock = getListOfSkusWithInsufficientStock(productsBySkuAndQuantity, itemsWithStock);

        if (!skusWithNoStock.isEmpty()) {
            String reason = "Estoque insuficiente: " + String.join(", ", skusWithNoStock);

            repository.saveReservation(orderId, StatusEnum.REJECTED.toString(), reason);
            log.info("Pedido {} REJEITADO: estoque insuficiente para {}", orderId, skusWithNoStock);
            return;
        }

        productsBySkuAndQuantity.forEach(repository::updateStock);

        repository.saveReservation(orderId, StatusEnum.RESERVED.toString(), null);
        log.info("Pedido {} RESERVADO: {}", orderId, productsBySkuAndQuantity);
    }

    private static List<String> getListOfSkusWithInsufficientStock(Map<String, Integer> productsBySkuAndQuantity, Map<String, Integer> itemsWithStock) {
        return productsBySkuAndQuantity.entrySet().stream()
                .filter(e -> itemsWithStock.getOrDefault(e.getKey(), 0) < e.getValue())
                .map(Map.Entry::getKey)
                .toList();
    }
}