package no.nav.modiaeventdistribution.redis

import no.nav.common.utils.EnvironmentUtils
import no.nav.modiaeventdistribution.WebsocketStorage
import redis.clients.jedis.HostAndPort

fun setupRedis(websocketStorage: WebsocketStorage): Redis.Consumer {
    return Redis.Consumer(
        hostAndPort = HostAndPort(EnvironmentUtils.getRequiredProperty("REDIS_HOST"), 6379),
        handler = websocketStorage::redisHandler
    )
}