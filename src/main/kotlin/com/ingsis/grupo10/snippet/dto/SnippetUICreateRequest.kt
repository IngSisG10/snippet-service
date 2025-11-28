package com.ingsis.grupo10.snippet.dto

data class SnippetUICreateRequest(
    val content: String,
    val extension: String,
    val language: String,
    val name: String,
)
