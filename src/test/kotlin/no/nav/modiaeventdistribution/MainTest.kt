package no.nav.modiaeventdistribution

import no.nav.common.nais.NaisYamlUtils


fun main() {
    NaisYamlUtils
            .getTemplatedConfig(".nais/qa-template.yml", mapOf("namespace" to "q0"))
            .also(NaisYamlUtils::loadFromYaml)
    val config = ConfigLoader{
        withProperties(".vault.properties")
        withProperty("port", "8081")
//        withMap(mapOf(
//                "service_user_username" to "vault",
//                "service_user_password" to "vault",
//                "port" to "8081"
//        ))
    }.load()

    startApplication(config)
}
