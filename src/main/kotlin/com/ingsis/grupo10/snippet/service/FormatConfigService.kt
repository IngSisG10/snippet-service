package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigResponse
import com.ingsis.grupo10.snippet.models.FormatConfig
import com.ingsis.grupo10.snippet.repository.FormatConfigRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FormatConfigService(
    private val formatConfigRepository: FormatConfigRepository,
    private val objectMapper: ObjectMapper,
) {
    fun getConfig(userId: String): FormatConfigResponse {
        val config =
            formatConfigRepository.findByUserId(userId)
                ?: createDefaultConfig(userId)

        return parseConfigToResponse(config)
    }

    fun updateConfig(
        userId: String,
        request: FormatConfigRequest,
    ): FormatConfigResponse {
        val existingConfig = formatConfigRepository.findByUserId(userId)

        val configJson = buildConfigJson(request)

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

    private fun createDefaultConfig(userId: String): FormatConfig {
        val defaultJson =
            """
            {
                "space_before_colon": false,
                "space_after_colon": true,
                "space_around_equals": true,
                "newline_before_println": 1,
                "indent_inside_block": 4
            }
            """.trimIndent()

        return formatConfigRepository.save(
            FormatConfig(
                id = UUID.randomUUID(),
                userId = userId,
                config = defaultJson,
            ),
        )
    }

    private fun buildConfigJson(request: FormatConfigRequest): String {
        val configMap = mutableMapOf<String, Any>()

        request.spaceBeforeColon?.let { configMap["space_before_colon"] = it }
        request.spaceAfterColon?.let { configMap["space_after_colon"] = it }
        request.spaceAroundEquals?.let { configMap["space_around_equals"] = it }
        request.newlineBeforePrintln?.let { configMap["newline_before_println"] = it }
        request.indentInsideBlock?.let { configMap["indent_inside_block"] = it }

        return objectMapper.writeValueAsString(configMap)
    }

    private fun parseConfigToResponse(config: FormatConfig): FormatConfigResponse {
        val configMap = objectMapper.readValue(config.config, Map::class.java)

        return FormatConfigResponse(
            id = config.id,
            userId = config.userId,
            spaceBeforeColon = configMap["space_before_colon"] as? Boolean,
            spaceAfterColon = configMap["space_after_colon"] as? Boolean,
            spaceAroundEquals = configMap["space_around_equals"] as? Boolean,
            newlineBeforePrintln = (configMap["newline_before_println"] as? Number)?.toInt(),
            indentInsideBlock = (configMap["indent_inside_block"] as? Number)?.toInt(),
        )
    }
}
