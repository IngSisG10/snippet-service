package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigResponse
import com.ingsis.grupo10.snippet.models.LintConfig
import com.ingsis.grupo10.snippet.repository.LintConfigRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LintConfigService(
    private val lintConfigRepository: LintConfigRepository,
    private val objectMapper: ObjectMapper,
) {
    fun getConfig(userId: UUID): LintConfigResponse {
        val config =
            lintConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return parseConfigToResponse(config)
    }

    fun updateConfig(
        userId: UUID,
        request: LintConfigRequest,
    ): LintConfigResponse {
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

    fun getConfigJson(userId: UUID): String {
        val config =
            lintConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return config.config
    }

    private fun createDefaultConfig(userId: UUID): LintConfig {
        val defaultJson =
            """
            {
                "identifier_format": "camel case"
            }
            """.trimIndent()

        return lintConfigRepository.save(
            LintConfig(
                id = UUID.randomUUID(),
                userId = userId,
                config = defaultJson,
            ),
        )
    }

    private fun buildConfigJson(request: LintConfigRequest): String {
        val configMap = mutableMapOf<String, Any>()

        request.identifierFormat?.let { configMap["identifier_format"] = it }
        request.printlnExpressionAllowed?.let { configMap["println_expression_allowed"] = it }
        request.readInputExpressionAllowed?.let { configMap["read_input_expression_allowed"] = it }

        return objectMapper.writeValueAsString(configMap)
    }

    private fun parseConfigToResponse(config: LintConfig): LintConfigResponse {
        val configMap = objectMapper.readValue(config.config, Map::class.java)

        return LintConfigResponse(
            id = config.id,
            userId = config.userId,
            identifierFormat = configMap["identifier_format"] as? String,
            printlnExpressionAllowed = configMap["println_expression_allowed"] as? Boolean,
            readInputExpressionAllowed = configMap["read_input_expression_allowed"] as? Boolean,
        )
    }
}
