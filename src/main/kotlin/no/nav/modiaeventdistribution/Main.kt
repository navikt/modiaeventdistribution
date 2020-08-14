package no.nav.modiaeventdistribution

import org.slf4j.LoggerFactory


val log = LoggerFactory.getLogger("modiaeventdistribution.Application")
fun main() {
    val config = ConfigLoader().load()
    startApplication(config)
}
