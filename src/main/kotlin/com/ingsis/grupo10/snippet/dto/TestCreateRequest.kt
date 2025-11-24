package com.ingsis.grupo10.snippet.dto

data class TestCreateRequest(
    val name: String,
    val input: List<String> = emptyList(),
    val output: List<String> = emptyList(),
)
