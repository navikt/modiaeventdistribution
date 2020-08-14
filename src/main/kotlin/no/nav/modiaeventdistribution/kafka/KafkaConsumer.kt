package no.nav.modiaeventdistribution.kafka

import kotlinx.coroutines.runBlocking
import no.nav.common.health.HealthCheckResult
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.modiaeventdistribution.infrastructur.HealthCheckAware
import no.nav.modiaeventdistribution.log
import no.nav.modiaeventdistribution.Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.security.plain.PlainLoginModule
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*

data class BootstrapServer internal constructor(
        val host: String,
        val port: Int
) {
    companion object {
        fun parse(configString: String): List<BootstrapServer> {
            return configString.split(",")
                    .map {
                        val (host, port) = it.split(":")
                        BootstrapServer(host, port.toInt())
                    }
        }
    }
}

data class KafkaConsumers(val consumers: List<KafkaConsumer>) {
    fun start() = consumers.forEach(KafkaConsumer::start)
    fun stop() = consumers.forEach(KafkaConsumer::stop)
}

internal val ERROR_GRACE_PERIODE = Duration.ofMinutes(5).toMillis()
internal val POLL_TIMEOUT = Duration.ofSeconds(5)

class KafkaConsumer(
        private val config: Config,
        private val topicName: String,
        private val groupId: String,
        private val bootstrapServers: List<BootstrapServer>,
        private val username: String,
        private val password: String,
        private val handler: suspend (key: String?, value: String?) -> Unit
) : HealthCheckAware {
    private val healthCheckData: SelfTestCheck
    private val consumer: org.apache.kafka.clients.consumer.KafkaConsumer<String, String>
    private val topicNameWithEnv: String

    @Volatile
    private var running = false

    @Volatile
    private var lastErrorTimestamp: Long = 0

    @Volatile
    private var lastError: Throwable? = null

    init {
        val bootstrapServers = bootstrapServers.joinToString(",") { (host, port) -> "$host:$port" }
        this.topicNameWithEnv = "${topicName}-${config.appEnvironment}"
        this.healthCheckData = SelfTestCheck("kafka-consumer-${this.topicNameWithEnv}",false) {
            this.checkHealth()
        }


        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        props["sasl.mechanism"] = "PLAIN"
        props[SaslConfigs.SASL_JAAS_CONFIG] = String.format("%s required username=\"%s\" password=\"%s\";",
                PlainLoginModule::class.java.name,
                username,
                password
        )
        this.consumer = org.apache.kafka.clients.consumer.KafkaConsumer(props)
    }

    fun start() {
        this.running = true
        log.info("starting kafka consumer for topic {}", topicName)
        this.consumer.subscribe(setOf(topicName))

        val thread = Thread(Runnable { this.run() })
        thread.name = "consumer-$topicName"
        thread.isDaemon = true
        thread.start()
    }

    fun stop() {
        this.running = false
    }

    fun run() {
        while (this.running) {
            try {
                val records: ConsumerRecords<String?, String?> = this.consumer.poll(POLL_TIMEOUT)
                runBlocking {
                    log.info("Received kafka-messages: ${records.count()}")
                    for (record in records) {
                        handler(record.key(), record.value())
                    }
                }
                this.consumer.commitAsync()
            } catch (e: Throwable) {
                log.error(e.message, e)
                this.lastErrorTimestamp = System.currentTimeMillis()
                this.lastError = e
            }
        }
    }

    override fun getHealthCheck(): SelfTestCheck = this.healthCheckData

    private fun checkHealth(): HealthCheckResult {
        if (!this.running) {
            return HealthCheckResult.unhealthy("consumer is not running")
        }
        if (this.lastErrorTimestamp + ERROR_GRACE_PERIODE > System.currentTimeMillis()) {
            return HealthCheckResult.unhealthy(lastError as Throwable)
        }
        return HealthCheckResult.healthy()
    }
}
