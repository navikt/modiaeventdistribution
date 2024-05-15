package no.nav.modiaeventdistribution

import dev.nohus.autokonfig.*
import dev.nohus.autokonfig.types.IntSetting
import dev.nohus.autokonfig.types.OptionalStringSetting
import dev.nohus.autokonfig.types.StringSetting
import java.util.Collections.singletonMap


fun AutoKonfig.withProperty(key: String, value: String) = apply {
    withMap(singletonMap(key, value))
}

class Config internal constructor() {
    val appName by StringSetting()
    val appEnvironment by StringSetting()
    val appVersion by StringSetting()
    val basePath by OptionalStringSetting()
    val port by IntSetting(8080)
}

class ConfigLoader(block: AutoKonfig.() -> Unit = {}) {
    init {
        AutoKonfig
            .clear()
            .withSystemProperties()
            .withEnvironmentVariables()
            .apply(block)
    }

    fun load() = Config()
}
