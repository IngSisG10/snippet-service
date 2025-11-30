package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.producer.FormatRequestProducer
import com.ingsis.grupo10.snippet.producer.LintRequestProducer
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RuleConfigService(
    private val formatConfigService: FormatConfigService,
    private val lintConfigService: LintConfigService,
    private val authClient: AuthClient,
    private val formatRequestProducer: FormatRequestProducer,
    private val lintRequestProducer: LintRequestProducer,
) {
    fun generateFormatEvents(userId: String) {
        // Get owned snippets and queue for formatting
        val ownedSnippets = authClient.getUserOwnedSnippets(userId)

        val failedSnippets = mutableListOf<UUID>()

        ownedSnippets.forEach { snippetId ->
            try {
                formatRequestProducer.publishFormatRequest(userId, snippetId.toString())
            } catch (e: Exception) {
                // fixme -> en lugar de romper: "el proceso debe reanudar desde dónde falló o salteando el snippet que causó la falla si hubiera. "
                failedSnippets.add(snippetId)
                println("Failed to queue format request for snippet $snippetId: ${e.message}")
            }
        }
    }

    fun generateLintEvents(userId: String) {
        // Get owned snippets and queue for linting
        val ownedSnippets = authClient.getUserOwnedSnippets(userId)

        val failedSnippets = mutableListOf<UUID>()

        ownedSnippets.forEach { snippetId ->
            try {
                lintRequestProducer.publishLintRequest(userId, snippetId.toString())
            } catch (e: Exception) {
                // fixme -> en lugar de romper: "el proceso debe reanudar desde dónde falló o salteando el snippet que causó la falla si hubiera. "
                failedSnippets.add(snippetId)
                println("Failed to queue lint request for snippet $snippetId: ${e.message}")
            }
        }
    }

    fun getFormattingRules(userId: String): List<RuleConfigResponse> = formatConfigService.getConfig(userId)

    fun getLintingRules(userId: String): List<RuleConfigResponse> = lintConfigService.getConfig(userId)

    fun updateFormattingRules(
        rules: List<RuleConfigRequest>,
        userId: String,
    ): List<RuleConfigResponse> = formatConfigService.updateConfig(userId, rules)

    fun updateLintingRules(
        rules: List<RuleConfigRequest>,
        userId: String,
    ): List<RuleConfigResponse> = lintConfigService.updateConfig(userId, rules)
}
