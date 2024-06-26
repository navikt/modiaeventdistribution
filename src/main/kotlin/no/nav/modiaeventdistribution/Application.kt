package no.nav.modiaeventdistribution

import io.ktor.server.application.install
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.websocket.*
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.modiaeventdistribution.infrastructur.naisRoutes
import no.nav.modiaeventdistribution.redis.setupRedis

val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)
data class Event(
    val veilederIdent: String,
    val eventType: String
)

fun startApplication(config: Config) {
    val applicationState = ApplicationState()
    val redisConsumer = setupRedis()
    val websocketStorage = WebsocketStorage(redisConsumer.getFlow())

    val applicationServer = embeddedServer(Netty, config.port) {
        install(WebSockets, WebsocketStorage.options)
        install(MicrometerMetrics) {
            registry = metricsRegistry
            meterBinders = listOf(
                ClassLoaderMetrics(),
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                JvmThreadMetrics()
            )
        }

        routing {
            route(config.basePath ?: config.appName) {
                naisRoutes(
                    readinessCheck = { applicationState.initialized },
                    livenessCheck = { applicationState.running },
                    selftestChecks = listOf(
                        redisConsumer.getHealthCheck()
                    ),
                    collectorRegistry = metricsRegistry
                )

                webSocket(path = "/ws/{ident}", handler = websocketStorage.wsHandler)
            }
        }

        redisConsumer.start()
        applicationState.initialized = true
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            log.info("Shutdown hook called, shutting down gracefully")
            redisConsumer.stop()
            applicationState.initialized = false
            applicationServer.stop(5000, 5000)
        }
    )

    applicationServer.start(wait = true)
}
