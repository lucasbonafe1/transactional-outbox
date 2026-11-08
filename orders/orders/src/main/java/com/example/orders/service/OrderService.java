package com.example.orders.service;

import com.example.orders.exception.IdempotencyKeyReuseException;
import com.example.orders.model.*;
import com.example.orders.model.dto.CreateOrderRequest;
import com.example.orders.model.dto.OrderCreatedData;
import com.example.orders.model.dto.OrderResponse;
import com.example.orders.model.dto.PlaceOrderResult;
import com.example.orders.repository.IdempotencyKeyRepository;
import com.example.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_KEY_LENGTH = 100;

    private final OrderWriterService writer;
    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeys;
    private final RequestHasher hasher;
    private final JsonMapper jsonMapper;

    public OrderService(OrderWriterService writer, OrderRepository orderRepository, IdempotencyKeyRepository idempotencyKeys, RequestHasher hasher, JsonMapper jsonMapper) {
        this.writer = writer;
        this.orderRepository = orderRepository;
        this.idempotencyKeys = idempotencyKeys;
        this.hasher = hasher;
        this.jsonMapper = jsonMapper;
    }

    public PlaceOrderResult placeOrder(String idempotencyKey, CreateOrderRequest request) {
        validateKey(idempotencyKey);
        String requestHash = hasher.hash(request);

        Optional<IdempotencyRecord> existing = idempotencyKeys.findByKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), idempotencyKey, requestHash);
        }

        try {
            return new PlaceOrderResult(writer.createOrder(idempotencyKey, requestHash, request), false);
        } catch (DuplicateKeyException e) {
            log.info("Requisição concorrente com a Idempotency-Key {}; devolvendo o pedido já criado", idempotencyKey);
            IdempotencyRecord winner = idempotencyKeys.findByKey(idempotencyKey).orElseThrow(() -> e);
            return replay(winner, idempotencyKey, requestHash);
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key não pode ser nula ou vazia");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Idempotency-Key não pode ter mais de " + MAX_KEY_LENGTH + " caracteres");
        }
    }

    private PlaceOrderResult replay(IdempotencyRecord record, String key, String requestHash) {
        if (!record.requestHash().equals(requestHash)) {
            log.warn("Requisição com Idempotency-Key {} tem hash diferente", key);
            throw new IdempotencyKeyReuseException(key);
        }
        OrderResponse stored = jsonMapper.readValue(record.responseBody(), OrderResponse.class);
        return new PlaceOrderResult(stored, true);
    }


    public Optional<OrderResponse> findById(UUID id) {
        return orderRepository.findById(id).map(OrderResponse::from);
    }
}
