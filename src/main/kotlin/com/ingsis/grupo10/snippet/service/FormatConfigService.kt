//package com.ingsis.grupo10.snippet.service
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.ingsis.grupo10.snippet.client.PrintScriptClient
//import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
//import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
//import com.ingsis.grupo10.snippet.models.FormatConfig
//import com.ingsis.grupo10.snippet.repository.FormatConfigRepository
//import org.springframework.stereotype.Service
//import java.util.UUID
//
//@Service
//class FormatConfigService(
//    private val formatConfigRepository: FormatConfigRepository,
//    private val printScriptClient: PrintScriptClient,
//    private val objectMapper: ObjectMapper,
//) {
//    fun getConfig(userId: String): List<RuleConfigResponse> {
//        val config =
//            formatConfigRepository.findByUserId(userId)
//                ?: createDefaultConfig(userId)
//        return parseConfigToResponse(config)
//    }
//
//    fun updateConfig(
//        userId: String,
//        request: List<RuleConfigRequest>,
//    ): List<RuleConfigResponse> {
//        val existingConfig = formatConfigRepository.findByUserId(userId)
//
//        val configJson = buildConfigJson(request) // "enforce-spacing-around-equals": {"value": true, "isActive": true}
//
//        val config =
//            if (existingConfig != null) {
//                formatConfigRepository.save(
//                    existingConfig.copy(
//                        config = configJson,
//                    ),
//                )
//            } else {
//                formatConfigRepository.save(
//                    FormatConfig(
//                        id = UUID.randomUUID(),
//                        userId = userId,
//                        config = configJson,
//                    ),
//                )
//            }
//
//        return parseConfigToResponse(config)
//    }
//
//    fun getConfigJson(userId: String): String {
//        val config =
//            formatConfigRepository.findByUserId(userId)
//                ?: createDefaultConfig(userId)
//
//        // Parse the stored config: {"rule-name": {"value": X, "isActive": Y}}
//        val configMap =
//            objectMapper.readValue(config.config, Map::class.java)
//                as Map<String, Map<String, Any?>>
//
//        // Transform to printscript format: {"rule-name": X} (only active rules)
//        val simplifiedConfig =
//            configMap
//                .filter { (_, ruleData) -> ruleData["isActive"] as? Boolean ?: true }
//                .mapValues { (_, ruleData) -> ruleData["value"] }
//
//        return objectMapper.writeValueAsString(simplifiedConfig)
//    }
//
//    private fun createDefaultConfig(userId: String): FormatConfig {        // Always fetch the latest rules from PrintScript
//        val rules = printScriptClient.getFormatConfigRules("1.1")
//
//        // Create a base config map with all rules from PrintScript with default values
//        val configMap =
//            rules.associate { rule ->
//                val defaultValue =
//                    rule.data.firstOrNull()?.default ?: "true"
//
//                val value: Any =
//                    when (rule.data.firstOrNull()?.type) {
//                        "Boolean" -> defaultValue.toBoolean()
//                        "Number" -> defaultValue.toIntOrNull() ?: defaultValue
//                        else -> defaultValue
//                        rule.name to
//                                mapOf(
//                                    "value" to value,
//                                    "isActive" to true,
//                                    )
//                    }
//
//                val json = objectMapper.writeValueAsString(configMap)
//
//                return formatConfigRepository.save(
//                    FormatConfig(
//                        id = UUID.randomUUID(),
//                        userId = userId,
//                        config = json,
//                    ),
//                )
//    }
//
//    private fun buildConfigJson(request: List<RuleConfigRequest>): String {
//        val configMap =
//            request.associate { rule ->
//                rule.id to
//                    mapOf(
//                        "value" to rule.value,
//                        "isActive" to rule.isActive,
//                    )
//            }
//
//        return objectMapper.writeValueAsString(configMap)
//    }
//
//    private fun parseConfigToResponse(config: FormatConfig): List<RuleConfigResponse> {
//        val configMap =
//            objectMapper.readValue(config.config, Map::class.java)
//                as Map<String, Map<String, Any?>>
//
//        return configMap.map { (key, ruleData) ->
//            RuleConfigResponse(
//                id = key,
//                name =
//                    key
//                        .replace("-", " ")
//                        .replace("_", " ")
//                        .split(" ")
//                        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) },
//                isActive = ruleData["isActive"] as? Boolean ?: true,
//                value = ruleData["value"],
//            )
//        }
//    }
//}
