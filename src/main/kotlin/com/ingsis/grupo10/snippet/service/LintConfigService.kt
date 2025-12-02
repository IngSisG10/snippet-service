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
        val config = mergeWithPrintScriptRules(userId)
        return parseConfigToResponse(config)
    }

    fun getConfigJson(userId: String): String {
        val config = mergeWithPrintScriptRules(userId)

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

    private fun mergeWithPrintScriptRules(userId: String): LintConfig {
        // Always fetch the latest rules from PrintScript
        val rules = printScriptClient.getLintConfigRules("1.1")

        // Create a base config map with all rules from PrintScript with default values
        val baseConfigMap: MutableMap<String, Map<String, Any?>> =
            rules
                .associate { rule ->
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
                }.toMutableMap()

        // Try to find existing user config
        val existingConfig = lintConfigRepository.findByUserId(userId)

        // If user has custom config, merge it with the base config
        val finalConfigMap =
            if (existingConfig != null) {
                val userConfigMap =
                    objectMapper.readValue(existingConfig.config, Map::class.java)
                        as Map<String, Map<String, Any?>>

                // Override with user's custom values where they exist
                userConfigMap.forEach { (ruleName, userData) ->
                    if (baseConfigMap.containsKey(ruleName)) {
                        baseConfigMap[ruleName] = userData
                    }
                }

                baseConfigMap
            } else {
                baseConfigMap
            }

        val json = objectMapper.writeValueAsString(finalConfigMap)

        // Save or update the config
        return if (existingConfig != null) {
            lintConfigRepository.save(
                existingConfig.copy(config = json),
            )
        } else {
            lintConfigRepository.save(
                LintConfig(
                    id = UUID.randomUUID(),
                    userId = userId,
                    config = json,
                ),
            )
        }
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
