package com.ingsis.grupo10.snippet.dto.rules

data class RuleConfigResponse(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val value: Any? = null, // string | number | null | undefined;
)
