package no.nav.modiaeventdistribution

import io.ktor.application.install
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import no.nav.modiaeventdistribution.infrastructur.naisRoutes
import org.slf4j.LoggerFactory


val log = LoggerFactory.getLogger("modiaeventdistribution.Application")
fun main() {
    val config = ConfigLoader().load()
    startApplication(config)
}
