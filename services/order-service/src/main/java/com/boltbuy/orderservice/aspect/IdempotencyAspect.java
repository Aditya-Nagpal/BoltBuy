package com.boltbuy.orderservice.aspect;

import com.boltbuy.orderservice.annotation.Idempotent;
import com.boltbuy.orderservice.dto.IdempotentResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (key == null || key.isBlank()) return joinPoint.proceed();

        String redisKey = "idempotency:" + key;

        // 1. Check if key exists
        Object cachedValue = redisTemplate.opsForValue().get(redisKey);
        if (cachedValue != null) {
            IdempotentResponse cachedResp = objectMapper.convertValue(cachedValue, IdempotentResponse.class);
            
            if ("PENDING".equals(cachedResp.getStatus())) {
                // throw new RuntimeException("Request is already being processed.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body("This request is already being processed. Please wait.");
            }
            
            log.info("Returning cached response for key: {}", key);
            return ResponseEntity.status(cachedResp.getStatusCode()).body(cachedResp.getBody());
        }

        // 2. Lock the request (Set PENDING)
        IdempotentResponse pending = new IdempotentResponse(0, null, "PENDING");
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, pending, idempotent.expire(), TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isNew)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Duplicate request.");
        }

        try {
            Object result = joinPoint.proceed();
            
            // 3. Cache the actual response
            if (result instanceof ResponseEntity<?> resp) {
                IdempotentResponse successResp = new IdempotentResponse(
                    resp.getStatusCode().value(), 
                    resp.getBody(), 
                    "COMPLETED"
                );
                redisTemplate.opsForValue().set(redisKey, successResp, idempotent.expire(), TimeUnit.SECONDS);
            }
            
            return result;
        } catch (Exception e) {
            // 4. Release lock on failure so user can retry
            redisTemplate.delete(redisKey);
            throw e;
        }
    }
}