package no.nav.modiaeventdistribution

import dev.nohus.autokonfig.AutoKonfig
import dev.nohus.autokonfig.withMap
import no.nav.common.nais.NaisYamlUtils

// .vault.properties skal inneholder noe alà (hentes fra vault om nødvendig);
// service_user_username=<srv username>
// service_user_password=<srv password>
fun AutoKonfig.withVaultProps(brukVaultFil: Boolean) = apply {
    if (brukVaultFil) {
        withProperties(".vault.properties")
    } else {
        withMap(mapOf(
                "service_user_username" to "N/A",
                "service_user_password" to "N/A"
        ))
    }
}

fun main() {
    NaisYamlUtils
            .getTemplatedConfig(".nais/qa-template.yml", mapOf("namespace" to "q0"))
            .also(NaisYamlUtils::loadFromYaml)
    val config = ConfigLoader{
        withVaultProps(brukVaultFil = false)
        withProperty("port", "8081")
    }.load()

    startApplication(config)
}
