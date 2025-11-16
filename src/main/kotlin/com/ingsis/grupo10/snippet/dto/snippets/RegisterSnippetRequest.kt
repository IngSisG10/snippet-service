package com.ingsis.grupo10.snippet.dto.snippets

import java.util.UUID

data class RegisterSnippetRequest(
    val snippetId: UUID,
    val ownerId: String,
)
