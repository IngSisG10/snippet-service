package com.ingsis.grupo10.snippet.dto

data class TestCreateRequest(
    val name: String,
    val inputs: String,
    val expectedOutputs: String,
)
