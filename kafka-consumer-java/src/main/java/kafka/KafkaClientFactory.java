package kafka;

import configuration.Config;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.clients.consumer.StickyAssignor;
import org.apache.kafka.clients.producer.KafkaProducer;

public class KafkaClientFactory {

    private Properties getAuthProperties() {
        var props = new Properties();
        props.put("bootstrap.servers", Config.KAFKA_BROKER);

        if (!Config.AUTHENTICATED_KAFKA) {
            return props;
        }

        props.put("security.protocol", Config.SECURITY_PROTOCOL);

        if (Config.TRUSTSTORE_PASSWORD != null) {
            props.put("ssl.truststore.location", Config.TRUSTSTORE_LOCATION);
            props.put("ssl.truststore.password", Config.TRUSTSTORE_PASSWORD);
        }

        if (Config.SECURITY_PROTOCOL.equals("SSL")) {
            props.put("ssl.keystore.type", "PKCS12");
            props.put("ssl.keystore.location", Config.KEYSTORE_LOCATION);
            props.put("ssl.keystore.password", Config.KEYSTORE_PASSWORD);
            props.put("ssl.key.password", Config.KEY_PASSWORD);
        }

        if (Config.SECURITY_PROTOCOL.equals("SASL_SSL")) {
            props.put("sasl.mechanism", "PLAIN");
            props.put("ssl.endpoint.identification.algorithm", "");
            props.put(
                "sasl.jaas.config",
                String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    Config.SASL_USERNAME,
                    Config.SASL_PASSWORD
                )
            );
        }

        return props;
    }

    public <K, V> org.apache.kafka.clients.consumer.Consumer<K, V> createConsumer() {
        var props = getAuthProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, Config.GROUP_ID);
        props.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer"
        );
        props.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer"
        );
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Config.MAX_POLL_RECORDS);
        props.put(
            ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
            String.format("%s,%s", StickyAssignor.class.getName(), RangeAssignor.class.getName())
        );
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, Config.SESSION_TIMEOUT);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, Config.SESSION_TIMEOUT / 3);
        return new KafkaConsumer<>(props);
    }

    public <K, V> KafkaProducer<K, V> createProducer() {
        var props = getAuthProperties();
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }
}
