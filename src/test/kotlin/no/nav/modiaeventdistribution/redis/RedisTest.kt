package no.nav.modiaeventdistribution.redis

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.HostAndPort

object TestUtils {
    
    class RedisContainer : GenericContainer<RedisContainer>("redis:6-alpine") {
        init {
            withExposedPorts(6379)
        }
    }
    
    interface WithRedis {
        
        companion object {
            private val container = RedisContainer()
            
            @BeforeAll
            @JvmStatic
            fun startContainer() {
                container.start()
            }
            
            @AfterAll
            @JvmStatic
            fun stopContainer() {
                container.stop()
            }
        }
        
        fun redisHostAndPort() = HostAndPort(container.host, container.getMappedPort(6379))
        
    }
}

class RedisTest : TestUtils.WithRedis {
    private val channel: String = "Testchannel"
    
    @Test
    fun `redis selfcheck ok`() {
        val redisConsumer = Redis.Consumer(
            hostAndPort = redisHostAndPort(),
            handler = { key, value -> },
            channel = channel
        )
        assertTrue(redisConsumer.getHealthCheck().check.checkHealth().isHealthy)
    }
}