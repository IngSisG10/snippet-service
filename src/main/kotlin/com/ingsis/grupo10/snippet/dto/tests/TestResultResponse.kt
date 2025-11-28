package com.ingsis.grupo10.snippet.dto.tests

data class TestResultResponse(
    val status: String, // success | fail
    val output: List<String>?,
)
