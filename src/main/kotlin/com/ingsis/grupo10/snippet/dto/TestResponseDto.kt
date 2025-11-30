package com.ingsis.grupo10.snippet.dto

import java.util.UUID

data class TestResponseDto(
    val id: UUID,
    val name: String,
    val input: List<String> = emptyList(),
    val output: List<String> = emptyList(),
)
