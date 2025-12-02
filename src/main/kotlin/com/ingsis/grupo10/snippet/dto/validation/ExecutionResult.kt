package com.ingsis.grupo10.snippet.dto.validation

sealed class ExecutionResult {
    data class Success(
        val output: List<String>,
    ) : ExecutionResult()

    data class Failed(
        val errors: List<ExecutionError>,
    ) : ExecutionResult()
}
