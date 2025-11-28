package com.ingsis.grupo10.snippet.dto.tests

data class ExecutionDto(
    val output: List<String>,
    val errors: List<ExecutionErrorDto>,
)
