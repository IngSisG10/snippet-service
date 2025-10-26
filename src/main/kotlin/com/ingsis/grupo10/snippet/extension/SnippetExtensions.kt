package com.ingsis.grupo10.snippet.extension

import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Snippet
import java.time.LocalDateTime
import java.util.UUID

fun SnippetCreateRequest.toSnippet(
    language: Language,
    ownerId: UUID,
    codeUrl: String,
): Snippet =
    Snippet(
        id = UUID.randomUUID(),
        name = this.name,
        codeUrl = codeUrl,
        language = language,
        description = this.description,
        version = this.version,
        ownerId = ownerId,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

fun Snippet.toSummaryDto(compliance: String? = null): SnippetSummaryDto =
    SnippetSummaryDto(
        id = this.id,
        name = this.name,
        language = this.language.name,
        version = this.version,
        createdAt = this.createdAt,
        compliance = compliance,
    )

fun Snippet.toDetailDto(): SnippetDetailDto =
    SnippetDetailDto(
        id = this.id,
        name = this.name,
        description = this.description,
        language = this.language.name,
        version = this.version,
        ownerId = this.ownerId,
        createdAt = this.createdAt,
        codeUrl = this.codeUrl,
    )
