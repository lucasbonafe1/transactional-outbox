package com.example.orders.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

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

    @Bean
    NewTopic stockEventsTopic(@Value("${orders.stock-events.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic stockEventsDlt(@Value("${orders.stock-events.topic}") String topic) {
        return TopicBuilder.name(topic + ".DLT").partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}
