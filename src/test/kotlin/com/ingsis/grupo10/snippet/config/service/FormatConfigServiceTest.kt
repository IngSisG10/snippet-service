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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class FormatConfigServiceTest {
    private val formatConfigRepository: FormatConfigRepository = mock()

    private lateinit var formatConfigService: FormatConfigService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var printScriptClient: PrintScriptClient

    private val testUserId = "test-user-id"

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        formatConfigService = FormatConfigService(formatConfigRepository, printScriptClient, objectMapper)
    }

    @Test
    fun `should get config for user`() {
        val configJson =
            """{"enforce-spacing-around-equals":{"value":true,"isActive":true},""" +
                """"line-breaks-after-println":{"value":1,"isActive":true}}"""
        val config =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = configJson,
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = formatConfigService.getConfig(testUserId)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("enforce-spacing-around-equals", result[0].id)
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

    @Test
    fun `should get config as JSON`() {
        val configJson =
            """{"enforce-spacing-around-equals":{"value":true,"isActive":true},""" +
                """"line-breaks-after-println":{"value":1,"isActive":false}}"""
        val config =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = configJson,
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = formatConfigService.getConfigJson(testUserId)

        assertNotNull(result)
        // Should only include active rules
        assertTrue(result.contains("enforce-spacing-around-equals"))
        // Should NOT include inactive rules
        assertTrue(!result.contains("line-breaks-after-println") || result.contains("\"line-breaks-after-println\":null"))
    }
}
