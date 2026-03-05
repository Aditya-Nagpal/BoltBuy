package com.boltbuy.orderservice.controller;

import com.boltbuy.orderservice.annotation.Idempotent;
import com.boltbuy.orderservice.dto.OrderRequest;
import com.boltbuy.orderservice.service.OrderProducer;
import com.boltbuy.orderservice.service.StockService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final StockService stockService;
    private final OrderProducer orderProducer;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    private Counter successCounter;
    private Counter soldOutCounter;

    public OrderController(StockService stockService, OrderProducer orderProducer, StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.stockService = stockService;
        this.orderProducer = orderProducer;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;

        this.successCounter = Counter.builder("boltbuy_orders_total")
                .description("Total successful flash sale orders")
                .tag("status", "success")
                .register(this.meterRegistry);

        this.soldOutCounter = Counter.builder("boltbuy_orders_total")
                .description("Total attempts on sold out products")
                .tag("status", "sold_out")
                .register(this.meterRegistry);
    }

    @PostMapping("/buy")
    @Idempotent(message = "Slow down! Your order is already being processed.")
    public ResponseEntity<String> placeOrder(
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody OrderRequest request
    ) {
        log.info("Received order request for user: {} and product: {}, idempotencyKey: {}", request.getUserId(), request.getProductId(), idempotencyKey);

        // 1. Atomically deduct stock from Redis
        boolean isStockDeducted = stockService.deductStock(request.getProductId(), 1);
        if (isStockDeducted) {
            successCounter.increment();

            // 2. Send to Kafka for worker-service to persist in DB
            orderProducer.sendOrderEvent(request.getUserId(), request.getProductId(), idempotencyKey);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Order request accepted. Processing your order...");
        } else {
            soldOutCounter.increment();

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Sorry, the item is sold out!");
        }
    }

    @GetMapping("/stock/{productId}")
    public ResponseEntity<Integer> getStock(@PathVariable Long productId) {
        String stockStr = redisTemplate.opsForValue().get("product:" + productId + ":stock");
        int stock = (stockStr != null) ? Integer.parseInt(stockStr) : 0;
        return ResponseEntity.ok(stock);
    }
}