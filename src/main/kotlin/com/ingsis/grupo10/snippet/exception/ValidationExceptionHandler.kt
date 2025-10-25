package com.ingsis.grupo10.snippet.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ValidationExceptionHandler {
    @ExceptionHandler(SnippetValidationException::class)
    fun handleValidationException(ex: SnippetValidationException): ResponseEntity<ErrorResponse> {
        val response =
            ErrorResponse(
                message = ex.message ?: "Validation failed",
                errors =
                    ex.errors.map { error ->
                        ErrorDetail(
                            rule = error.rule,
                            message = error.message,
                            location =
                                if (error.line != null && error.column != null) {
                                    "Línea ${error.line}, Columna ${error.column}"
                                } else {
                                    null
                                },
                        )
                    },
            )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }
}

data class ErrorResponse(
    val message: String,
    val errors: List<ErrorDetail>,
)

data class ErrorDetail(
    val rule: String,
    val message: String,
    val location: String?,
)
