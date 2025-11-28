package com.ingsis.grupo10.snippet.consumer

import com.ingsis.grupo10.snippet.events.LintRequest
import com.ingsis.grupo10.snippet.service.SnippetService
import org.austral.ingsis.redis.RedisStreamConsumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
@Profile("!test") // No ejecutar en tests
class LintRequestConsumer
    @Autowired
    constructor(
        redis: RedisTemplate<String, String>,
        @Value("\${stream.lint.key}") streamKey: String,
        @Value("\${groups.lint}") groupId: String,
        private val snippetService: SnippetService,
    ) : RedisStreamConsumer<LintRequest>(streamKey, groupId, redis) {
        override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, LintRequest>> =
            StreamReceiver.StreamReceiverOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(10)) // Poll cada 10 segundos
                .targetType(LintRequest::class.java) // Tipo para deserialización
                .build()

        override fun onMessage(record: ObjectRecord<String, LintRequest>) {
            val request = record.value
            println("========================================")
            println("Processing LINT request")
            println("Stream: ${record.stream}, Record ID: ${record.id}")
            println("Snippet ID: ${request.snippetId}")
            println("========================================")

            try {
                val snippetId = UUID.fromString(request.snippetId)
                snippetService.lintSnippet(snippetId)
                println("Lint request processed successfully for snippet: ${request.snippetId}")
            } catch (e: Exception) {
                println("Error processing lint request: ${e.message}")
                e.printStackTrace()
            }
        }
    }
