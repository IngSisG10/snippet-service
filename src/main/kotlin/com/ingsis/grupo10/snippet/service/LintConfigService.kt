package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.models.LintConfig
import com.ingsis.grupo10.snippet.repository.LintConfigRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LintConfigService(
    private val lintConfigRepository: LintConfigRepository,
    private val objectMapper: ObjectMapper,
) {
    fun getConfig(userId: String): List<RuleConfigResponse> {
        val config =
            lintConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return parseConfigToResponse(config)
    }

    fun getConfigJson(userId: String): String {
        val config =
            lintConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        // Parse the stored config: {"rule-name": {"value": X, "isActive": Y}}
        val configMap =
            objectMapper.readValue(config.config, Map::class.java)
                as Map<String, Map<String, Any?>>

        // Transform to printscript format: {"rule-name": X} (only active rules)
        val simplifiedConfig =
            configMap
                .filter { (_, ruleData) -> ruleData["isActive"] as? Boolean ?: true }
                .mapValues { (_, ruleData) -> ruleData["value"] }

        return objectMapper.writeValueAsString(simplifiedConfig)
    }

    fun updateConfig(
        userId: String,
        request: List<RuleConfigRequest>,
    ): List<RuleConfigResponse> {
        val existingConfig = lintConfigRepository.findByUserId(userId)

        val configJson = buildConfigJson(request)

        val config =
            if (existingConfig != null) {
                lintConfigRepository.save(
                    existingConfig.copy(
                        config = configJson,
                    ),
                )
            } else {
                lintConfigRepository.save(
                    LintConfig(
                        id = UUID.randomUUID(),
                        userId = userId,
                        config = configJson,
                    ),
                )
            }

        return parseConfigToResponse(config)
    }

    // todo: add other rules of linting
    private fun createDefaultConfig(userId: String): LintConfig {
        val defaultConfig =
            mapOf(
                "identifier_format" to
                    mapOf(
                        "value" to "camel case", // "puede cambiarse a snake case, pascal case"
                        "isActive" to true,
                    ),
                "mandatory-variable-or-literal-in-println" to
                    mapOf(
                        "value" to true,
                        "isActive" to true,
                    ),
                "mandatory-variable-or-literal-in-readInput" to
                    mapOf(
                        "value" to true,
                        "isActive" to true,
                    ),
            )

        val json = objectMapper.writeValueAsString(defaultConfig)

        return lintConfigRepository.save(
            LintConfig(
                id = UUID.randomUUID(),
                userId = userId,
                config = json,
            ),
        )
    }

    private fun buildConfigJson(request: List<RuleConfigRequest>): String {
        val configMap =
            request.associate { rule ->
                rule.id to
                    mapOf(
                        "value" to rule.value,
                        "isActive" to rule.isActive,
                    )
            }

        return objectMapper.writeValueAsString(configMap)
    }

    private fun parseConfigToResponse(config: LintConfig): List<RuleConfigResponse> {
        val configMap =
            objectMapper.readValue(config.config, Map::class.java)
                as Map<String, Map<String, Any?>>

        return configMap.map { (key, ruleData) ->
            RuleConfigResponse(
                id = key,
                name = key.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                isActive = ruleData["isActive"] as? Boolean ?: true,
                value = ruleData["value"],
            )
        }
    }
}
