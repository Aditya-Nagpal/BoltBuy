package com.boltbuy.orderservice.consumer;

import com.boltbuy.orderservice.annotation.Idempotent;
import com.boltbuy.orderservice.dto.IdempotentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderCompensationConsumer {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private Counter rollbackCounter;

    public OrderCompensationConsumer(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        this.rollbackCounter = Counter.builder("boltbuy_orders_total")
                .description("Total number of saga rollbacks")
                .tag("status", "rolled_back")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "order-compensation-events", groupId = "order-compensation-group")
    public void compensate(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String key = jsonNode.get("idempotencyKey").asText();
            Long productId = jsonNode.get("productId").asLong();
            
            String stateKey = "idempotency:" + key;

            // Check if we already rolled this back to prevent "Phantom Inventory"
            Object cachedValue = redisTemplate.opsForValue().get(stateKey);
            
            if (cachedValue != null) {

                IdempotentResponse state = objectMapper.convertValue(cachedValue, IdempotentResponse.class);
                
                if(!"ROLLED_BACK".equals(state.getStatus())) {
                    // Rollback the stock
                    String stockKey = "product:" + productId + ":stock";
                    redisTemplate.opsForValue().increment(stockKey, 1);

                    // Update the state to prevent future rollbacks for the same key
                    state.setStatus("ROLLED_BACK");

                    long ttl = Idempotent.class.getMethod("expire").getDefaultValue() != null
                                ? (long) Idempotent.class.getMethod("expire").getDefaultValue() : 3600L;
                    redisTemplate.opsForValue().set(stateKey, state, ttl, TimeUnit.SECONDS);
                    rollbackCounter.increment();
                    log.warn("🔄 SAGA SUCCESS: Stock restored and state updated for {}", stateKey);
                } else {
                    log.warn("⚠️ SAGA SKIPPED: Compensation already performed for {}", stateKey);
                }
            }
        } catch (Exception e) {
            log.error("Saga compensation failed!", e);
        }
    }
}