package no.nav.modiaeventdistribution

import no.nav.modiaeventdistribution.kafka.BootstrapServer
import no.nav.modiaeventdistribution.kafka.KafkaConsumer
import no.nav.modiaeventdistribution.kafka.KafkaConsumers
import java.util.*

fun setupKafkaConsumers(config: Config, websocketStorage: WebsocketStorage): KafkaConsumers {
    val bootstrapServer = BootstrapServer.parse(config.kafkaBrokersUrl)
    val kafkaConsumer = { topicName: String ->
        KafkaConsumer(
                config = config,
                topicName = topicName,
                bootstrapServers = bootstrapServer,
                groupId = UUID.randomUUID().toString(),
                username = config.serviceUserUsername,
                password = config.serviceUserPassword,
                handler = websocketStorage::kafkaHandler
        )
    }

    return KafkaConsumers(listOf(
            kafkaConsumer("aapen-modia-nyAktivBruker-v1"),
            kafkaConsumer("aapen-modia-nyAktivEnhet-v1")
    ))
}
