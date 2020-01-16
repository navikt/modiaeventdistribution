package no.nav.sbl.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.metrics.aspects.TimerAspect;
import no.nav.modiaeventdistribution.kafka.KafkaConsumer;
import no.nav.modiaeventdistribution.kafka.KafkaConsumerConfig;
import no.nav.modiaeventdistribution.kafka.KafkaConsumers;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.sbl.websockets.WebSocketProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;

@Configuration
@EnableAspectJAutoProxy
@EnableScheduling
public class ApplicationConfig implements ApiApplication {

    public static final String EVENTS_API_URL_PROPERTY_NAME = "EVENTS_API_URL";
    public static final String KAFKA_BROKERS_URL_PROPERTY_NAME = "KAFKA_BROKERS_URL";

    private final KafkaConsumers kafkaConsumers;

    public ApplicationConfig() {
        kafkaConsumers =  new KafkaConsumers(asList(
                new KafkaConsumer(eventConsumerBuilder().topicName("aapen-modia-nyAktivBruker-v1").build()),
                new KafkaConsumer(eventConsumerBuilder().topicName("aapen-modia-nyAktivEnhet-v1").build())
        ));
    }

    private KafkaConsumerConfig.KafkaConsumerConfigBuilder eventConsumerBuilder() {
        List<KafkaConsumerConfig.BootstrapServer> bootstrapServers = KafkaConsumerConfig.BootstrapServer.parse(EnvironmentUtils.getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY_NAME));
        return KafkaConsumerConfig.builder()
                .bootstrapServers(bootstrapServers)
                .groupId(UUID.randomUUID().toString())
                .username(EnvironmentUtils.getRequiredProperty(EnvironmentUtils.resolveSrvUserPropertyName()))
                .password(EnvironmentUtils.getRequiredProperty(EnvironmentUtils.resolverSrvPasswordPropertyName()))
                .handler(WebSocketProvider::sendEventToWebsocketSubscribers);
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator.customizeJettyBuilder(jettyBuilder -> jettyBuilder.websocketEndpoint(WebSocketProvider.class));
        apiAppConfigurator.selfTests(kafkaConsumers.getKafkaConsumers());
    }

    @Override
    public void startup(ServletContext servletContext) {
        kafkaConsumers.start();
    }

    @Override
    public void shutdown(ServletContext servletContext) {
        kafkaConsumers.stop();
    }

}
