package com.boltbuy.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getPath().toString();
        String method = exchange.getRequest().getMethod().name();

        // Logging the incoming request
        log.info("🚀 Incoming Request: {} {}", method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = (exchange.getResponse().getStatusCode() != null) 
                             ? exchange.getResponse().getStatusCode().value() 
                             : 500;

            log.info("✅ Response Sent: {} {} | Status: {} | Time: {}ms", 
                     method, path, statusCode, duration);
        }));
    }

    @Override
    public int getOrder() {
        // High priority so it wraps all other filters
        return -1;
    }
}