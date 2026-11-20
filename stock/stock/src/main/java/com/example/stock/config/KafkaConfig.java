package com.example.stock.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    /** Tópico das mensagens do `orders` que o estoque não conseguiu processar. */
    @Bean
    NewTopic ordersDlt(@Value("${orders.dead.letter.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    /** Tópico onde o estoque publica StockReserved e StockRejected. */
    @Bean
    NewTopic stockEventsTopic(@Value("${stock.events.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> avroTemplate,
                                     @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
                                     @Value("${orders.dead.letter.topic}") String dltTopic) {

        // Mensagem que nem desserializa chega como bytes crus e precisa de um produtor de bytes.
        // Fica como objeto local (e não como @Bean) para não desligar o KafkaTemplate do Spring Boot.
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap),
                new StringSerializer(), new ByteArraySerializer()));

        // A ordem importa: a primeira chave compatível com o tipo do valor é a usada.
        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);   // falha de desserialização
        templates.put(Object.class, avroTemplate);    // objeto Avro válido cujo processamento falhou

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                templates, (record, ex) -> new TopicPartition(dltTopic, record.partition()));

        // 1 tentativa + 3 retentativas, com 1 s entre elas. DeserializationException
        // já é "não repetível" por padrão e vai direto para a DLT.
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}