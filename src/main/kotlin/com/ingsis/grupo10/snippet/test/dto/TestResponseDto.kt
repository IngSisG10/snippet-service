package com.ingsis.grupo10.snippet.test.dto

import java.util.UUID

data class TestResponseDto(
    val id: UUID,
    val snippetId: UUID,
    val name: String,
    val inputs: String,
    val expectedOutputs: String,
)
