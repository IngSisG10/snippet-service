package com.ingsis.grupo10.snippet.exception

import com.ingsis.grupo10.snippet.dto.validation.ValidationError

class SnippetValidationException(
    message: String,
    val errors: List<ValidationError>,
) : RuntimeException(message)
