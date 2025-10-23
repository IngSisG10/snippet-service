package com.ingsis.grupo10.snippet.dto.validation

sealed class ValidationResult {
    object Success : ValidationResult()

    data class Failed(
        val errors: List<ValidationError>,
    ) : ValidationResult()
}
