package com.ingsis.grupo10.snippet.dto.rules

data class RuleDto(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val value: Any?,
)
