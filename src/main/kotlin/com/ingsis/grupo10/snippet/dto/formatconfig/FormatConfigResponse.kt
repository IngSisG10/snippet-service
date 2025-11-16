package com.ingsis.grupo10.snippet.dto.formatconfig

import java.util.UUID

data class FormatConfigResponse(
    val id: UUID,
    val userId: String,
    val spaceBeforeColon: Boolean?,
    val spaceAfterColon: Boolean?,
    val spaceAroundEquals: Boolean?,
    val newlineBeforePrintln: Int?,
    val indentInsideBlock: Int?,
)
