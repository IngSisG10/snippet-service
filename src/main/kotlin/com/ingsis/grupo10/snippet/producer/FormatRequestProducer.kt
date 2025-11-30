package com.ingsis.grupo10.snippet.producer

import com.ingsis.grupo10.snippet.events.FormatRequest
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

interface FormatRequestProducer {
    fun publishFormatRequest(
        userId: String,
        snippetId: String,
    )
}

@Component
class RedisFormatRequestProducer
    @Autowired
    constructor(
        @Value("\${stream.format.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
    ) : RedisStreamProducer(streamKey, redis),
        FormatRequestProducer {
        override fun publishFormatRequest(
            userId: String,
            snippetId: String,
        ) {
            println("Publishing format request for snippet: $snippetId")
            val request = FormatRequest(userId, snippetId)
            emit(request)
        }
    }
