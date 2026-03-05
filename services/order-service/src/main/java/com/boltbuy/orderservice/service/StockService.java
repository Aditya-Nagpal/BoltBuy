package com.boltbuy.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> script;

    @PostConstruct
    public void init() {
        script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/check_and_deduct.lua")));
    }

    public boolean deductStock(Long productId, int quantity) {
        String key = "product:" + productId + ":stock";
        
        // Execute the Lua script atomically
        Long result = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(quantity));
        
        return result != null && result == 1L;
    }
}