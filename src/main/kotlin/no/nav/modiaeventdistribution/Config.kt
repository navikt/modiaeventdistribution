package no.nav.modiaeventdistribution

import dev.nohus.autokonfig.*
import dev.nohus.autokonfig.types.IntSetting
import dev.nohus.autokonfig.types.StringSetting
import no.nav.common.utils.NaisUtils

fun AutoKonfig.withVaultCredentials(secretName: String) = apply {
    try {
        val credentials = NaisUtils.getCredentials(secretName)
        withMap(
                mapOf(
                        "${secretName}_username" to credentials.username,
                        "${secretName}_password" to credentials.password
                ),
                "vault secret ($secretName)"
        )
    } catch (e: Throwable){}
}

class Config internal constructor() {
    val appname by StringSetting(default = "modiaeventdistribution")
    val appEnvironmentName by StringSetting()
    val eventsApiUrl by StringSetting()
    val kafkaBrokersUrl by StringSetting()
    val serviceUserUsername by StringSetting()
    val serviceUserPassword by StringSetting()
    val port by IntSetting(8080)
}

class ConfigLoader(extraProps: Map<String, String> = emptyMap()) {
    init {
        AutoKonfig
                .clear()
                .withSystemProperties()
                .withEnvironmentVariables()
                .withVaultCredentials("service_user")
                .withMap(extraProps)
    }

    fun load() = Config()
}
