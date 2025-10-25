package com.ingsis.grupo10.snippet.dto.validation

data class ValidationError(
    val message: String,
    val line: Int?,
    val column: Int?,
    val rule: String,
)
