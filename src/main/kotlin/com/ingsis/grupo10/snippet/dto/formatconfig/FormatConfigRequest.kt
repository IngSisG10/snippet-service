package com.ingsis.grupo10.snippet.dto.formatconfig

data class FormatConfigRequest(
    val spaceBeforeColon: Boolean? = null,
    val spaceAfterColon: Boolean? = null,
    val spaceAroundEquals: Boolean? = null,
    val newlineBeforePrintln: Int? = null,
    val indentInsideBlock: Int? = null,
)
