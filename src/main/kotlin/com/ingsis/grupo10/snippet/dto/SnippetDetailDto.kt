package com.ingsis.grupo10.snippet.dto

import java.time.LocalDateTime
import java.util.UUID

data class SnippetDetailDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val language: String,
    val version: String,
    val createdAt: LocalDateTime,
    val codeUrl: String,
)
