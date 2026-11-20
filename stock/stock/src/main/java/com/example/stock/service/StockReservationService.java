package com.example.stock.service;

import com.example.stock.model.ItemLine;
import com.example.stock.model.StatusEnum;
import com.example.stock.model.StockDecision;
import com.example.stock.model.StockTypeData;
import com.example.stock.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class StockReservationService {
    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);
    private final StockReservationRepository repository;

    public StockReservationService(StockReservationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public StockDecision handle(UUID eventId, UUID orderId, List<ItemLine> items) {
        boolean hasBeenSaved = repository.insertEventIfWasNotProcessed(eventId);
        if (!hasBeenSaved) {
            log.info("Evento {} já processado; ignorando (entrega duplicada)", eventId);
            return loadDecision(orderId, items);
        }

        reserve(orderId, items);
        return loadDecision(orderId, items);
    }

    private StockDecision loadDecision(UUID orderId, List<ItemLine> items) {
        List<StockTypeData.Item> summed = sumBySkuAndQuantity(items).entrySet().stream()
                .map(e -> new StockTypeData.Item(e.getKey(), e.getValue()))
                .toList();

        return repository.getStockDecisionByOrderId(orderId, summed);
    }

    // Soma quantidades por codigo SKU e ordena: travar as linhas sempre na mesma ordem evita deadlock.
    private Map<String, Integer> sumBySkuAndQuantity(List<ItemLine> items) {
        Map<String, Integer> needed = new TreeMap<>();
        items.forEach(i -> needed.merge(i.sku(), i.quantity(), Integer::sum));
        return needed;
    }

    private void reserve(UUID orderId, List<ItemLine> orderItems) {
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