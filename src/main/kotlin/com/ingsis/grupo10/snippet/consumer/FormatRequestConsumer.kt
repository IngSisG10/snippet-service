package com.ingsis.grupo10.snippet.consumer

import com.ingsis.grupo10.snippet.events.FormatRequest
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
class FormatRequestConsumer
    @Autowired
    constructor(
        redis: RedisTemplate<String, String>,
        @Value("\${stream.format.key}") streamKey: String,
        @Value("\${groups.format}") groupId: String,
        private val snippetService: SnippetService,
    ) : RedisStreamConsumer<FormatRequest>(streamKey, groupId, redis) {
        override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, FormatRequest>> =
            StreamReceiver.StreamReceiverOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(10)) // Poll cada 10 segundos
                .targetType(FormatRequest::class.java) // Tipo para deserialización
                .build()

        override fun onMessage(record: ObjectRecord<String, FormatRequest>) {
            val request = record.value
            println("========================================")
            println("Processing FORMAT request")
            println("Stream: ${record.stream}, Record ID: ${record.id}")
            println("Snippet ID: ${request.snippetId}")
            println("========================================")

            try {
                val snippetId = UUID.fromString(request.snippetId)
                val userId = request.userId

                // Llamar al servicio existente que hace el formateo
                snippetService.formatSnippet(userId, snippetId)

                println("Format request processed successfully for snippet: ${request.snippetId}")
            } catch (e: IllegalArgumentException) {
                println("Skipping format request - snippet not found: ${request.snippetId}")
                println("This might happen if the snippet was deleted after the format request was queued")
            } catch (e: Exception) {
                println("Error processing format request: ${e.message}")
                e.printStackTrace()
            }
        }
    }
