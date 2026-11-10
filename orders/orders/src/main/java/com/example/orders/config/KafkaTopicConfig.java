package com.example.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    /**
     * Cria o tópico ao subir a aplicação, se ainda não existir.
     * 3 partições: pedidos diferentes se espalham; eventos do mesmo pedido
     * (mesma chave = orderId) caem sempre na mesma partição, em ordem.
     */
    @Bean
    NewTopic ordersTopic(@Value("${outbox.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
