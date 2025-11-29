package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import com.ingsis.grupo10.snippet.producer.FormatRequestProducer
import com.ingsis.grupo10.snippet.producer.LintRequestProducer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RuleConfigService(
    private val formatConfigService: FormatConfigService,
    private val lintConfigService: LintConfigService,
    private val objectMapper: ObjectMapper,
    private val authClient: AuthClient,
    private val formatRequestProducer: FormatRequestProducer,
    private val lintRequestProducer: LintRequestProducer,
) {
    // todo: RuleConfigService, en sus metodos, va a estar compuesto por la logica que tenga
    // cada uno de los servicios de configuracion (format y lint)

    fun generateFormatEvents(userId: String) {
        // Get owned snippets and queue for formatting
        val ownedSnippets = authClient.getUserOwnedSnippets(userId)

        val failedSnippets = mutableListOf<UUID>()

        ownedSnippets.forEach { snippetId ->
            try {
                formatRequestProducer.publishFormatRequest(snippetId.toString())
            } catch (e: Exception) {
                // fixme -> en lugar de romper: "el proceso debe reanudar desde dónde falló o salteando el snippet que causó la falla si hubiera. "
                failedSnippets.add(snippetId)
                println("Failed to queue format request for snippet $snippetId: ${e.message}")
            }
        }
    }

    fun getFormattingRules(userId: String): List<RuleConfigResponse> = formatConfigService.getConfig(userId)

    // fixme
    fun getLintingRules(userId: String): List<RuleDto> {
        val json = lintConfigService.getConfigJson(userId)
        val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
        return mapToRuleList(map)
    }

    fun updateFormattingRules(
        rules: List<RuleConfigRequest>,
        userId: String,
    ): List<RuleConfigResponse> = formatConfigService.updateConfig(userId, rules)

    // fixme
    fun updateLintingRules(
        rules: Map<String, Any>,
        userId: String,
    ) {
        val request = rulesToLintConfigRequest(rules)
        lintConfigService.updateConfig(userId, request)
    }

    // Helpers
    // todo: tiralo en helpers
    private fun mapToRuleList(config: Map<String, Any?>): List<RuleDto> =
        config.map { (key, value) ->
            RuleDto(
                id = key,
                name = formatKeyToHumanName(key),
                isActive = true, // fixme: Why? -> siempre activas por ahora
                value = value,
            )
        }

    private fun formatKeyToHumanName(key: String): String = key.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    // fixme
    private fun rulesToFormatConfigRequest(rules: List<RuleDto>): FormatConfigRequest {
        var spaceBeforeColon: Boolean? = null
        var spaceAfterColon: Boolean? = null
        var spaceAroundEquals: Boolean? = null
        var newlineBeforePrintln: Int? = null
        var indentInsideBlock: Int? = null

        rules.forEach { rule ->
            when (rule.id) {
                "space_before_colon" -> spaceBeforeColon = rule.value as? Boolean
                "space_after_colon" -> spaceAfterColon = rule.value as? Boolean
                "space_around_equals" -> spaceAroundEquals = rule.value as? Boolean
                "newline_before_println" -> newlineBeforePrintln = (rule.value as? Number)?.toInt()
                "indent_inside_block" -> indentInsideBlock = (rule.value as? Number)?.toInt()
            }
        }

        return FormatConfigRequest(
            spaceBeforeColon = spaceBeforeColon,
            spaceAfterColon = spaceAfterColon,
            spaceAroundEquals = spaceAroundEquals,
            newlineBeforePrintln = newlineBeforePrintln,
            indentInsideBlock = indentInsideBlock,
        )
    }

    // fixme
    private fun rulesToLintConfigRequest(rules: Map<String, Any>): LintConfigRequest =
        LintConfigRequest(
            identifierFormat = rules["identifierFormat"] as? String,
            printlnExpressionAllowed = rules["printlnExpressionAllowed"] as? Boolean,
            readInputExpressionAllowed = rules["readInputExpressionAllowed"] as? Boolean,
        )
}
