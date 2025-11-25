package com.ingsis.grupo10.snippet.dto.tests

data class RunTestRequest(
    val name: String,
    val input: List<String>?, // readLn
    val output: List<String>?, // printLn
)
