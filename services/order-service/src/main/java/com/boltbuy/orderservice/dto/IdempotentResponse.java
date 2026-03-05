package com.boltbuy.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotentResponse implements Serializable {
    private int statusCode;
    private Object body;
    private String status; // "PENDING" or "COMPLETED"
}