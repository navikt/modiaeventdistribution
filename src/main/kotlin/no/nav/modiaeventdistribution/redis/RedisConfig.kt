package no.nav.modiaeventdistribution.redis

import no.nav.common.utils.EnvironmentUtils

fun setupRedis(): Redis.Consumer {
    return Redis.Consumer(
        uri = EnvironmentUtils.getRequiredProperty("REDIS_URI"),
        user = EnvironmentUtils.getRequiredProperty("REDIS_USER"),
        password = EnvironmentUtils.getRequiredProperty("REDIS_PASSWORD")
    )
}