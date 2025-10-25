package com.ingsis.grupo10.snippet.dto

import java.time.LocalDateTime
import java.util.UUID

data class SnippetSummaryDto(
    val id: UUID,
    val name: String,
    val language: String,
    val version: String,
    val createdAt: LocalDateTime,
    val compliance: String?, // "pending", "valid", or "invalid"
)
