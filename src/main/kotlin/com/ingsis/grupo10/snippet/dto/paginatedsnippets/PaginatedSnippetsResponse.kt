package com.ingsis.grupo10.snippet.dto.paginatedsnippets

import java.util.UUID

data class PaginatedSnippetsResponse(
    val page: Int,
    val pageSize: Int,
    val count: Long,
    val snippets: List<SnippetResponse>,
)

// todo: Vamos a tener que incluir el extension en el SnippetDetailDto
// Este es temporal
data class SnippetResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val language: String,
    val extension: String = "ps", // Hardcoded for now. Fixme!
    val version: String,
    val createdAt: String,
)
