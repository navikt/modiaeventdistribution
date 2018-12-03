package no.nav.modiaeventdistribution.kafka;

import java.util.List;

public class KafkaConsumers {

    private final List<KafkaConsumer> kafkaConsumers;

    public KafkaConsumers(List<KafkaConsumer> kafkaConsumers) {
        this.kafkaConsumers = kafkaConsumers;
    }

    public void start() {
        kafkaConsumers.forEach(KafkaConsumer::start);
    }

    public void stop() {
        kafkaConsumers.forEach(KafkaConsumer::stop);
    }

    public List<KafkaConsumer> getKafkaConsumers() {
        return kafkaConsumers;
    }
}
