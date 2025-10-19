package com.ingsis.grupo10.snippet.snippet.dto

data class SnippetCreateRequest(
    val name: String,
    val description: String?,
    val code: String,
    val languageName: String,
    val version: String,
)
