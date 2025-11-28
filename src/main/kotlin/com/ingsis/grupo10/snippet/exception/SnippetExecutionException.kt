package com.ingsis.grupo10.snippet.exception

import com.ingsis.grupo10.snippet.dto.validation.ExecutionError

class SnippetExecutionException(
    message: String,
    val errors: List<ExecutionError>,
) : RuntimeException(message)
