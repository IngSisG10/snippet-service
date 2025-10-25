package com.ingsis.grupo10.snippet.dto.log

import java.time.LocalDateTime
import java.util.UUID

data class LogDto(
    val id: UUID,
    val tagName: String,
    val snippetId: UUID?,
    val testId: UUID?,
    val date: LocalDateTime,
    val data: Map<String, String>,
)
