package com.ingsis.grupo10.snippet.snippet.dto

import com.ingsis.grupo10.snippet.models.Language

data class SnippetCreateRequest(
    val name: String,
    val description: String,
    val code: String,
    val language: Language,
    val version: String,
)
