package com.dishari.in.config;

import com.dishari.in.infrastructure.messaging.event.CreateBulkUrlEvent;
import com.dishari.in.infrastructure.messaging.event.QrGenerationEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;


import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ── Topic definition ─────────────────────────────────────────
    @Bean
    public NewTopic qrGenerationTopic() {
        return TopicBuilder
                .name("qr-generation")
                .partitions(3)      // 3 partitions — parallel QR generation
                .replicas(1)        // increase in prod
                .build();
    }

    @Bean
    public NewTopic bulkUrlTopic() {
        return TopicBuilder.name("bulk-url-creation")
                .partitions(5) // More partitions for bulk as it's a heavier load
                .replicas(1)
                .build();
    }

    // ── Producer ─────────────────────────────────────────────────
    @Bean
    public ProducerFactory<String, QrGenerationEvent> qrProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        // Reliability — wait for all replicas to ack
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        // Retry on transient failures
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, CreateBulkUrlEvent> bulkProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }


    @Bean(name = "defaultRetryTopicKafkaTemplate")
    public KafkaTemplate<String, QrGenerationEvent> qrKafkaTemplate() {
        return new KafkaTemplate<>(qrProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, CreateBulkUrlEvent> bulkKafkaTemplate() {
        return new KafkaTemplate<>(bulkProducerFactory());
    }

    // ── Consumer ─────────────────────────────────────────────────
    @Bean
    public ConsumerFactory<String, QrGenerationEvent> qrConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "qr-generation-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JacksonJsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Only deserialize our trusted package
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES,
                "com.dishari.in.infrastructure.messaging.event");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE,
                QrGenerationEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, CreateBulkUrlEvent> bulkConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "bulk-url-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Trust the package for the Bulk Event
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.dishari.in.*");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, "com.dishari.in.infrastructure.messaging.event.CreateBulkUrlEvent");

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QrGenerationEvent>
    qrKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, QrGenerationEvent>
                factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(qrConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CreateBulkUrlEvent> bulkKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CreateBulkUrlEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(bulkConsumerFactory());
        factory.setConcurrency(5);
        return factory;
    }
}