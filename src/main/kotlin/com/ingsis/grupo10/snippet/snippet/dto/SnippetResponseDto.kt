package com.ingsis.grupo10.snippet.snippet.dto

import java.time.LocalDateTime
import java.util.UUID

data class SnippetResponseDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val language: String,
    val version: String,
    val ownerId: UUID,
    val createdAt: LocalDateTime,
)
