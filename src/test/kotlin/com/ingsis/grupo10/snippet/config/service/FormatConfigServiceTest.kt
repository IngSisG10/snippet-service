package com.ingsis.grupo10.snippet.config.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.models.FormatConfig
import com.ingsis.grupo10.snippet.repository.FormatConfigRepository
import com.ingsis.grupo10.snippet.service.FormatConfigService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.ingsis.grupo10.snippet.dto.rules.DataItem
import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class FormatConfigServiceTest {
    private val formatConfigRepository: FormatConfigRepository = mock()
    private val printScriptClient: PrintScriptClient = mock()

    private lateinit var formatConfigService: FormatConfigService
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = "test-user-id"

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        val dataItem = DataItem("true", "true", "boolean")
        val rule = RuleDto("enforce-spacing-around-equals", listOf(dataItem))
        `when`(printScriptClient.getFormatConfigRules(anyString())).thenReturn(listOf(rule))
        formatConfigService = FormatConfigService(formatConfigRepository, printScriptClient, objectMapper)
    }

    @Test
    fun `should update config for user`() {
        val existingConfig =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"enforce-spacing-around-equals":{"value":true,"isActive":true}}""",
            )

        val request =
            listOf(
                RuleConfigRequest(
                    id = "enforce-spacing-around-equals",
                    name = "Enforce Spacing Around Equals",
                    isActive = false,
                    value = true,
                ),
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(existingConfig)
        `when`(formatConfigRepository.save(org.mockito.kotlin.any())).thenReturn(existingConfig)

        val result = formatConfigService.updateConfig(testUserId, request)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
