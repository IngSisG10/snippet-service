package com.ingsis.grupo10.snippet.snippet.dto

data class SnippetResponseDto(
    val id: String,
    val name: String,
    val description: String,
    val language: String,
    val version: String,
    val ownerId: String,
    val createdAt: String,
)
