package no.nav.modiaeventdistribution.infrastructur

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.common.health.selftest.SelfTestCheck
import no.nav.common.health.selftest.SelfTestUtils
import no.nav.common.health.selftest.SelftestHtmlGenerator

fun Route.naisRoutes(
    readinessCheck: () -> Boolean,
    livenessCheck: () -> Boolean = { true },
    selftestChecks: List<SelfTestCheck> = emptyList(),
    collectorRegistry: PrometheusMeterRegistry
) {
    route("internal") {
        get("/isAlive") {
            if (livenessCheck()) {
                call.respondText("Alive")
            } else {
                call.respondText("Not alive", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/isReady") {
            if (readinessCheck()) {
                call.respondText("Ready")
            } else {
                call.respondText("Not ready", status = HttpStatusCode.InternalServerError)
            }
        }

        get("/metrics") {
            call.respondText(collectorRegistry.scrape())
        }

        get("/selftest") {
            withContext(Dispatchers.IO) {
                val selftest = SelfTestUtils.checkAllParallel(selftestChecks)
                val selftestMarkup = SelftestHtmlGenerator.generate(selftest)

                call.respondText(selftestMarkup, ContentType.Text.Html)
            }
        }
    }
}
