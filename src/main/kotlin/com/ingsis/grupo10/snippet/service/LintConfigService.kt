package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.models.LintConfig
import com.ingsis.grupo10.snippet.repository.LintConfigRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LintConfigService(
    private val lintConfigRepository: LintConfigRepository,
    private val printScriptClient: PrintScriptClient,
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

    private fun createDefaultConfig(userId: String): LintConfig {
        val rules = printScriptClient.getLintConfigRules("1.1")

        val configMap =
            rules.associate { rule ->
                val defaultValue =
                    rule.data.firstOrNull()?.default ?: "true"

                val value: Any =
                    when (rule.data.firstOrNull()?.type) {
                        "Boolean" -> defaultValue.toBoolean()
                        "Number" -> defaultValue.toIntOrNull() ?: defaultValue
                        else -> defaultValue
                    }

                rule.name to
                    mapOf(
                        "value" to value,
                        "isActive" to true,
                    )
            }

        val json = objectMapper.writeValueAsString(configMap)

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
                name =
                    key
                        .replace("-", " ")
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
                isActive = ruleData["isActive"] as? Boolean ?: true,
                value = ruleData["value"],
            )
        }
    }
}
