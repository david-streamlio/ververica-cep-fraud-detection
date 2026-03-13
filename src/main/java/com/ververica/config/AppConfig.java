package com.ververica.config;

import io.confluent.kafka.serializers.KafkaJsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class AppConfig {
    public static final String BOOTSTRAP_URL = requireEnv("KAFKA_BOOTSTRAP_URL");

    public static final String TRANSACTIONS_TOPIC = "transactions";
    public static final String ALERTS_TOPIC = "alerts";

    public static final String CONSUMER_ID = "fn.consumer";
    public static final int TOTAL_CUSTOMERS = 10_000;

    public static Properties buildProducerProps() {
        var properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_URL);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getCanonicalName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSerializer.class.getCanonicalName());
        properties.put(ProducerConfig.ACKS_CONFIG, "1");

        return buildSecurityProps(properties);
    }

    public static Properties buildSecurityProps(Properties properties) {
        String username = requireEnv("KAFKA_USERNAME");
        String password = requireEnv("KAFKA_PASSWORD");

        properties.put("security.protocol", "SASL_SSL");
        properties.put("sasl.mechanism", "PLAIN");
        properties.put("sasl.jaas.config",
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"" + username + "\" " +
            "password=\"" + password + "\";");

        return properties;
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "Required environment variable '" + name + "' is not set. " +
                "See the Configuration section in Readme.md."
            );
        }
        return value;
    }
}