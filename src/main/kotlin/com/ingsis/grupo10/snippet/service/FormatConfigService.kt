package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.models.FormatConfig
import com.ingsis.grupo10.snippet.repository.FormatConfigRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FormatConfigService(
    private val formatConfigRepository: FormatConfigRepository,
    private val objectMapper: ObjectMapper,
) {
    fun getConfig(userId: String): List<RuleConfigResponse> {
        val config =
            formatConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return parseConfigToResponse(config)
    }

    fun updateConfig(
        userId: String,
        request: List<RuleConfigRequest>,
    ): List<RuleConfigResponse> {
        val existingConfig = formatConfigRepository.findByUserId(userId)

        val configJson = buildConfigJson(request) // "enforce-spacing-around-equals": {"value": true, "isActive": true}

        val config =
            if (existingConfig != null) {
                formatConfigRepository.save(
                    existingConfig.copy(
                        config = configJson,
                    ),
                )
            } else {
                formatConfigRepository.save(
                    FormatConfig(
                        id = UUID.randomUUID(),
                        userId = userId,
                        config = configJson,
                    ),
                )
            }

        return parseConfigToResponse(config)
    }

    fun getConfigJson(userId: String): String {
        val config =
            formatConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return config.config
    }

    // todo: que agarre las rules de printscript
    // todo: mockearlo con los nombres que deben ser
    private fun createDefaultConfig(userId: String): FormatConfig {
        // for now we need 1.0 version of default config
        val defaultConfig = // todo -> luego api de printscript-service que da el mapOf
            mapOf(
//                "if-brace-below-line" to mapOf("value" to false, "isActive" to true),
//                "if-brace-same-line" to mapOf("value" to false, "isActive" to true),
//                "indent-inside-if" to mapOf("value" to 2, "isActive" to true),
//                "mandatory-line-break-after-statement" to mapOf("value" to true, "isActive" to true),
                "line-breaks-after-println" to mapOf("value" to 1, "isActive" to true),
                "mandatory-single-space-separation" to mapOf("value" to true, "isActive" to true),
                "enforce-no-spacing-around-equals" to mapOf("value" to true, "isActive" to true),
                "enforce-spacing-after-colon-in-declaration" to mapOf("value" to true, "isActive" to true),
                "enforce-spacing-around-equals" to mapOf("value" to true, "isActive" to true),
                "mandatory-space-surrounding-operations" to mapOf("value" to true, "isActive" to true),
                "space_before_and_after_equal" to mapOf("value" to true, "isActive" to true),
                "mandatory-space-surrounding-operations" to mapOf("value" to true, "isActive" to true),
                "enforce-spacing-before-colon-in-declaration" to mapOf("value" to true, "isActive" to true),
                "enforce-spacing-around-equals" to mapOf("value" to true, "isActive" to true),
                "mandatory-space-surrounding-operations" to mapOf("value" to true, "isActive" to true),
            )

        val json = objectMapper.writeValueAsString(defaultConfig)

        return formatConfigRepository.save(
            FormatConfig(
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

    private fun parseConfigToResponse(config: FormatConfig): List<RuleConfigResponse> {
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
