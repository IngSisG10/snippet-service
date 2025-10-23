package com.ingsis.grupo10.snippet.dto.validation

data class LintErrorDTO(
    val message: String,
    val type: String,
    val segment: Int?,
)
