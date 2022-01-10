package no.nav.modiaeventdistribution.redis

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.Jedis

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
            channel = channel
        )
        assertTrue(redisConsumer.getHealthCheck().check.checkHealth().isHealthy)
    }
    
    @Test
    fun `mottar redis-melding på kanal`() = runBlocking {
        val message = "TestMessage"
        val redisConsumer = Redis.Consumer(
            hostAndPort = redisHostAndPort(),
            channel = channel
        )
        redisConsumer.start()
        delay(1000)
        val publisher = Jedis(redisHostAndPort())
        publisher.publish(channel, message)
        val messageList = redisConsumer.getFlow().take(1).toList()
        
        assertEquals(messageList[0], message)
        redisConsumer.stop()
    }
}