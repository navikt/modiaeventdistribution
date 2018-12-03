package no.nav.modiaeventdistribution.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.validation.ValidationUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.function.BiConsumer;

import static java.util.stream.Collectors.joining;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;

@Slf4j
public class KafkaConsumer implements Helsesjekk {

    private static final long ERROR_GRACE_PERIOD = Duration.ofMinutes(5).toMillis();
    public static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);
    private final HelsesjekkMetadata helsesjekkMetadata;
    private final org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer;
    private final String topicName;
    private final BiConsumer<String, String> handler;

    private volatile boolean running;
    private volatile long lastErrorTimestamp;
    private volatile Throwable lastError;

    public KafkaConsumer(KafkaConsumerConfig kafkaConsumerConfig) {
        ValidationUtils.validate(kafkaConsumerConfig);

        this.topicName = kafkaConsumerConfig.topicName + "-" + requireEnvironmentName();
        this.handler = kafkaConsumerConfig.handler;

        String bootstrapServers = kafkaConsumerConfig.bootstrapServers.stream().map(s -> String.format("%s:%s", s.host, s.port)).collect(joining(","));
        String groupId = kafkaConsumerConfig.groupId;

        this.helsesjekkMetadata = new HelsesjekkMetadata(
                "kafka-consumer-" + topicName,
                bootstrapServers,
                String.format("consuming kafka topic: %s with groupId %s", topicName, groupId),
                false
        );

        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(GROUP_ID_CONFIG, groupId);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put(SASL_JAAS_CONFIG, String.format("%s required username=\"%s\" password=\"%s\";",
                PlainLoginModule.class.getName(),
                kafkaConsumerConfig.username,
                kafkaConsumerConfig.password
        ));

        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
    }

    public void start() {
        running = true;

        log.info("starting kafka consumer for topic {}", topicName);
        consumer.subscribe(Collections.singleton(topicName));

        Thread thread = new Thread(this::run);
        thread.setName("consumer-" + topicName);
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        while (running) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<String, String> record : records) {
                    handler.accept(record.value(), record.key());
                }
                consumer.commitAsync();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                lastErrorTimestamp = System.currentTimeMillis();
                lastError = e;
            }
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void helsesjekk() throws Throwable {
        if (!running) {
            throw new IllegalStateException("consumer is not running");
        }

        if (lastErrorTimestamp + ERROR_GRACE_PERIOD > System.currentTimeMillis()){
            throw lastError;
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return helsesjekkMetadata;
    }

}
