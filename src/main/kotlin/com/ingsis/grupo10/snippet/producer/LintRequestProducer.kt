package com.ingsis.grupo10.snippet.producer

import com.ingsis.grupo10.snippet.events.LintRequest
import org.austral.ingsis.redis.RedisStreamProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

interface LintRequestProducer {
    fun publishLintRequest(snippetId: String)
}

@Component
class RedisLintRequestProducer
    @Autowired
    constructor(
        @Value("\${stream.lint.key}") streamKey: String,
        redis: RedisTemplate<String, String>,
    ) : RedisStreamProducer(streamKey, redis),
        LintRequestProducer {
        override fun publishLintRequest(snippetId: String) {
            println("Publishing lint request for snippet: $snippetId")
            val request = LintRequest(snippetId)
            emit(request)
        }
    }
