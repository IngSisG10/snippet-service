package com.ingsis.grupo10.snippet.consumer

import com.ingsis.grupo10.snippet.events.TestExecutionRequest
import com.ingsis.grupo10.snippet.service.TestService
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
class TestExecutionConsumer
    @Autowired
    constructor(
        redis: RedisTemplate<String, String>,
        @Value("\${stream.test.key}") streamKey: String,
        @Value("\${groups.test}") groupId: String,
        private val testService: TestService,
    ) : RedisStreamConsumer<TestExecutionRequest>(streamKey, groupId, redis) {
        override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, TestExecutionRequest>> =
            StreamReceiver.StreamReceiverOptions
                .builder()
                .pollTimeout(Duration.ofSeconds(10)) // Poll cada 10 segundos
                .targetType(TestExecutionRequest::class.java) // Tipo para deserialización
                .build()

        override fun onMessage(record: ObjectRecord<String, TestExecutionRequest>) {
            val request = record.value
            println("========================================")
            println("Processing TEST EXECUTION request")
            println("Stream: ${record.stream}, Record ID: ${record.id}")
            println("Snippet ID: ${request.snippetId}")
            println("========================================")

            try {
                val snippetId = UUID.fromString(request.snippetId)

                val results = testService.runAllTestsForSnippet(snippetId)

                println("Test execution completed for snippet: ${request.snippetId}")
                println("Results: ${results.size} tests executed")
                results.forEachIndexed { index, result ->
                    println("  Test ${index + 1}: ${result.status}")
                }
            } catch (e: Exception) {
                println("Error processing test execution request: ${e.message}")
                e.printStackTrace()
            }
        }
    }
