package com.example.orders.service;

import com.example.orders.model.dto.CreateOrderRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RequestHasher {

    private final JsonMapper jsonMapper;

    public RequestHasher(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String hash(CreateOrderRequest request) {
        try {
            byte[] json = jsonMapper.writeValueAsBytes(request);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}