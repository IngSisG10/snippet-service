package com.ingsis.grupo10.snippet.client

sealed class CreatedResult {
    data class Success(
        val url: String,
    ) : CreatedResult()

    data class Failure(
        val status: Int?,
        val message: String? = null,
    ) : CreatedResult()
}
