package com.ingsis.grupo10.snippet.dto.snippets

import java.util.UUID

data class SnippetPermissionInfo(
    val snippetId: UUID,
    val ownerId: String,
    val ownerEmail: String?,
    val permission: String,
)
