package com.ingsis.grupo10.snippet.dto.snippets

import java.util.UUID

data class SnippetPermissionInformation(
    val snippetId: UUID,
    val ownerId: String,
    val ownerEmail: String?,
    val ownerName: String?,
    val permission: String,
)
