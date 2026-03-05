package com.boltbuy.workerservice.dto;

// Common DTO in both services
public record OrderEvent(
    String userId, 
    Long productId, 
    String idempotencyKey // Add this field
) {}
