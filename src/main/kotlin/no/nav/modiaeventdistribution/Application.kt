package no.nav.modiaeventdistribution

import io.ktor.application.install
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.modiaeventdistribution.infrastructur.naisRoutes


val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)
data class Event(
        val id: Long,
        val veilederIdent: String,
        val eventType: String
)

fun startApplication(config: Config) {
    val applicationState = ApplicationState()
    val websocketStorage = WebsocketStorage()
    val kafkaConsumers = setupKafkaConsumers(config, websocketStorage)

    val applicationServer = embeddedServer(Netty, config.port) {
        install(WebSockets, WebsocketStorage.options)
        install(MicrometerMetrics) {
            registry = metricsRegistry
            meterBinders = listOf(
                    ClassLoaderMetrics(),
                    JvmMemoryMetrics(),
                    JvmGcMetrics(),
                    ProcessorMetrics(),
                    JvmThreadMetrics(),
                    FileDescriptorMetrics()
            )
        }

        routing {
            route(config.appname) {
                naisRoutes(
                        readinessCheck = { applicationState.initialized },
                        livenessCheck = { applicationState.running },
                        selftestChecks = listOf(
                                *kafkaConsumers.consumers.map{ it.getHealthCheck() }.toTypedArray()
                        )
                )

                webSocket("/ws/{ident}", websocketStorage.wsHandler)
            }
        }

        kafkaConsumers.start()
        applicationState.initialized = true
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        kafkaConsumers.stop()
        applicationState.initialized = false
        applicationServer.stop(5000, 5000)
    })

    applicationServer.start(wait = true)
}