package com.ingsis.grupo10.snippet.dto.lintconfig

data class LintConfigRequest(
    val identifierFormat: String? = null,
    val printlnExpressionAllowed: Boolean? = null,
    val readInputExpressionAllowed: Boolean? = null,
)
