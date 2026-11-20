package com.example.orders.controller;

import com.example.orders.model.dto.CreateOrderRequest;
import com.example.orders.model.dto.OrderResponse;
import com.example.orders.model.dto.PlaceOrderResult;
import com.example.orders.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Uma repetição com a mesma Idempotency-Key recebe exatamente a mesma resposta (201 e o mesmo corpo),
     * mais o cabeçalho Idempotent-Replayed: true, útil para depurar.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        PlaceOrderResult result = orderService.placeOrder(idempotencyKey, request);

        return ResponseEntity
                .created(URI.create("/orders/" + result.order().id()))
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(result.order());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.of(orderService.findById(id));
    }
}