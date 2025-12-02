package com.ingsis.grupo10.snippet.producer

import com.ingsis.grupo10.snippet.events.TestExecutionRequest
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

interface TestExecutionProducer {
    fun publishTestExecutionRequest(
        userId: String,
        snippetId: String,
    )
}

@Component
class RedisTestExecutionProducer
    @Autowired
    constructor(
        @Value("\${stream.test.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
    ) : RedisStreamProducer(streamKey, redis),
        TestExecutionProducer {
        override fun publishTestExecutionRequest(
            userId: String,
            snippetId: String,
        ) {
            println("Publishing test execution request for snippet: $snippetId")
            val request = TestExecutionRequest(userId, snippetId)
            emit(request)
        }
    }
