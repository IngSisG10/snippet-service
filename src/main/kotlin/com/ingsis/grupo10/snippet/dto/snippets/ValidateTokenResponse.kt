package com.ingsis.grupo10.snippet.dto.snippets

data class ValidateTokenResponse(

    val userId: String,
    val email: String?,
    val name: String?,
)
