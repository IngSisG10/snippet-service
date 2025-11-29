package com.ingsis.grupo10.snippet.dto

import java.util.UUID

data class SnippetUIShareRequest(
    val snippetId: UUID,
    val targetUserEmail: String,
)
