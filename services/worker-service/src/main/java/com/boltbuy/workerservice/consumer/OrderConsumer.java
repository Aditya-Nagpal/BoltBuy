package com.boltbuy.workerservice.consumer;

import com.boltbuy.workerservice.model.Order;
import com.boltbuy.workerservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "flash-sale-orders", groupId = "flash-sale-group")
    public void consume(String message) {
        log.info("Processing order from Kafka: {}", message);
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(message);

            if ("fail-test".equals(jsonNode.get("userId").asText())) {
                throw new RuntimeException("CRITICAL: Simulated Database Crash!");
            }
            
            // Build the entity using the Integer/Long productId you updated
            Order order = Order.builder()
                    .userId(jsonNode.get("userId").asText())
                    .productId(jsonNode.get("productId").asLong()) // Use asLong() for your new data type
                    .build();

            orderRepository.save(order);
            log.info("Order for User {} saved to Postgres", order.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist order: {}", e.getMessage());

            if (jsonNode != null && jsonNode.has("idempotencyKey")) {
                String compensationMessage = message;
                log.warn("Triggering compensation for key: {}", jsonNode.get("idempotencyKey").asText());
                kafkaTemplate.send("order-compensation-events", compensationMessage);
            }
        }
    }
}