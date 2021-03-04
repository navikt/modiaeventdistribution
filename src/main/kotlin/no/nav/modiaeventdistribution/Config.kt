package no.nav.modiaeventdistribution

import dev.nohus.autokonfig.*
import dev.nohus.autokonfig.types.IntSetting
import dev.nohus.autokonfig.types.StringSetting
import no.nav.common.utils.NaisUtils
import java.io.IOException
import java.util.*
import java.util.Collections.singletonMap

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
    } catch (e: Throwable) {}
}

fun AutoKonfig.withProperties(file: String) = apply {
    val stream = Config::class.java.classLoader.getResourceAsStream(file)
        ?: throw RuntimeException("Resource file ($file) not found")

    val properties = try {
        Properties().apply { load(stream) }
    } catch (e: IOException) {
        throw RuntimeException("Could not read file ($file)")
    }

    withProperties(properties, "properties file ($file)")
}

fun AutoKonfig.withProperty(key: String, value: String) = apply {
    withMap(singletonMap(key, value))
}

class Config internal constructor() {
    val appName by StringSetting()
    val appEnvironment by StringSetting()
    val appVersion by StringSetting()
    val eventsApiUrl by StringSetting()
    val kafkaBrokersUrl by StringSetting()
    val serviceUserUsername by StringSetting()
    val serviceUserPassword by StringSetting()
    val port by IntSetting(8080)
}

class ConfigLoader(block: AutoKonfig.() -> Unit = {}) {
    init {
        AutoKonfig
            .clear()
            .withSystemProperties()
            .withEnvironmentVariables()
            .withVaultCredentials("service_user")
            .apply(block)
    }

    fun load() = Config()
}
