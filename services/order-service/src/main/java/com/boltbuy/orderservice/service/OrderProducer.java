package com.boltbuy.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "flash-sale-orders";

    public void sendOrderEvent(String userId, Long productId, String idempotencyKey) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("productId", productId);
            event.put("idempotencyKey", idempotencyKey);
            
            String message = objectMapper.writeValueAsString(event);
            log.info("Sending order event to Kafka: {}", message);

            kafkaTemplate.send(TOPIC, userId, message);
        } catch (Exception e) {
            log.error("Failed to send order event to Kafka", e);
        }
    }
}