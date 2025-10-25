package com.ingsis.grupo10.snippet.dto.log

import com.ingsis.grupo10.snippet.dto.validation.ValidationError
import java.time.LocalDateTime
import java.util.UUID

data class LintStatus(
    val snippetId: UUID,
    val status: String, // "valid", "invalid", "pending"
    val lastLintDate: LocalDateTime?,
    val errors: List<ValidationError>,
)
