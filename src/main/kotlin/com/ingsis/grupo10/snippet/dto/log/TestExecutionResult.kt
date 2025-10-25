package com.ingsis.grupo10.snippet.dto.log

import java.time.LocalDateTime
import java.util.UUID

data class TestExecutionResult(
    val logId: UUID,
    val testId: UUID,
    val status: String, // "passed" or "failed"
    val actualOutput: String,
    val expectedOutput: String,
    val durationMs: Long?,
    val executedAt: LocalDateTime,
)
