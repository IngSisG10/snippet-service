package com.ingsis.grupo10.snippet.dto.lintconfig

import java.util.UUID

data class LintConfigResponse(
    val id: UUID,
    val userId: UUID,
    val identifierFormat: String?,
    val printlnExpressionAllowed: Boolean?,
    val readInputExpressionAllowed: Boolean?,
)
