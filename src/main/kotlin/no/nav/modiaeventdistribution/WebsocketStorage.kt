package no.nav.modiaeventdistribution

import io.ktor.features.BadRequestException
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.WebSocketServerSession
import io.ktor.websocket.WebSockets
import io.micrometer.core.instrument.Gauge
import no.nav.modiaeventdistribution.infrastructur.fromJson
import java.time.Duration

class WebsocketStorage {
    companion object {
        val options: WebSockets.WebSocketOptions.() -> Unit = {
            pingPeriod = Duration.ofMinutes(3)
        }
    }

    init {
        Gauge
                .builder("websocket_clients", this::getAntallTilkoblinger)
                .register(metricsRegistry)
    }

    val sessions = mutableMapOf<String, MutableList<WebSocketServerSession>>()
    val wsHandler: suspend DefaultWebSocketServerSession.() -> Unit = {
        val ident = (call.parameters["ident"] ?: throw BadRequestException("No ident found"))
        try {
            sessions.putIfAbsent(ident, mutableListOf())!!.add(this)
        } catch (e: Throwable) {
            log.error("Websocket error", e)
        } finally {
            sessions[ident]?.remove(this)
        }
    }

    fun getAntallTilkoblinger(): Int = sessions
            .values
            .map { it.size }
            .sum()

    suspend fun kafkaHandler(key: String?, value: String?) {
        if (value == null) {
            log.error("Empty kafka-message")
            return
        }

        val event = value.fromJson<Event>()
        val (id, veilederIdent, eventType) = event
        log.info("Sending $eventType to $veilederIdent")

        sessions[veilederIdent]?.forEach {
            it.send(Frame.Text(eventType))
        }
    }
}