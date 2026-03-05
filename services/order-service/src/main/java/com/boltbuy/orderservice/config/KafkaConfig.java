package com.boltbuy.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("flash-sale-orders")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compensationTopic() {
        return TopicBuilder.name("order-compensation-events")
                .partitions(3)
                .compact() // Optional: keeps only the latest state for a key
                .build();
    }
}