package com.ingsis.grupo10.snippet.dto

import java.util.UUID

data class TestResponseDto(
    val id: UUID,
    val snippetId: UUID,
    val name: String,
    val inputs: String,
    val expectedOutputs: String,
)
