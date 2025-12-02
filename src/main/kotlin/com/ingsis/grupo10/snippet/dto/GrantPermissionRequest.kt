package com.ingsis.grupo10.snippet.dto

import java.util.UUID

data class GrantPermissionRequest(
    val snippetId: UUID,
    val targetUserEmail: String,
)
