package com.ingsis.grupo10.snippet.dto

import java.time.LocalDateTime
import java.util.UUID

data class SnippetUIDetailDto(
    val id: UUID,
    val name: String,
    val content: String,
    val language: String,
    val extension: String,
    val compliance: LocalDateTime,
    val author: String,
)
