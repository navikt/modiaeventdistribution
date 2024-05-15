package no.nav.modiaeventdistribution

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.withMap
import no.nav.common.nais.NaisYamlUtils


fun main() {
    NaisYamlUtils
        .getTemplatedConfig(".nais/qa-template.yml", mapOf("namespace" to "q0"))
        .also(NaisYamlUtils::loadFromYaml)
    val config = ConfigLoader {
        withProperty("port", "8081")
    }.load()

    startApplication(config)
}
