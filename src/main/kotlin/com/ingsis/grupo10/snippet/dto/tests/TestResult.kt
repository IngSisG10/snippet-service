package com.ingsis.grupo10.snippet.dto.tests

data class TestResult(
    val passed: Boolean,
    val expected: List<String>,
    val actual: List<String>,
)
