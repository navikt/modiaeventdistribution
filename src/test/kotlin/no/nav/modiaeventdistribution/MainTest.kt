package no.nav.modiaeventdistribution

import no.nav.common.nais.NaisYamlUtils


fun main() {
    NaisYamlUtils.loadFromYaml(".nais/nais-q0.yml")
    val config = ConfigLoader(mapOf(
            "service_user_username" to "vault",
            "service_user_password" to "vault",
            "port" to "8081"
    )).load()

    startApplication(config)
}
