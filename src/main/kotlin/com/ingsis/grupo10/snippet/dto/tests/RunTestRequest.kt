package com.ingsis.grupo10.snippet.dto.tests

data class RunTestRequest(
    val content: String,
    val input: List<String>?, // readLn
    val output: List<String>?, // printLn
    val version: String,
)
