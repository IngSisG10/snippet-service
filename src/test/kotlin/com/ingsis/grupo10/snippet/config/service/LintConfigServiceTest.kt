package com.ingsis.grupo10.snippet.config.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.models.LintConfig
import com.ingsis.grupo10.snippet.repository.LintConfigRepository
import com.ingsis.grupo10.snippet.service.LintConfigService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.UUID

class LintConfigServiceTest {
    private val lintConfigRepository: LintConfigRepository = mock()

    private lateinit var lintConfigService: LintConfigService
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = "test-user-id"

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        lintConfigService = LintConfigService(lintConfigRepository, objectMapper)
    }

    @Test
    fun `should get config for user`() {
        val config =
            LintConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"identifier_format":{"value":"camel case","isActive":true}}""",
            )

        `when`(lintConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = lintConfigService.getConfig(testUserId)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("identifier_format", result[0].id)
        assertEquals("camel case", result[0].value)
    }

    @Test
    fun `should create default config when user has no config`() {
        `when`(lintConfigRepository.findByUserId(testUserId)).thenReturn(null)
        `when`(lintConfigRepository.save(org.mockito.kotlin.any())).thenAnswer { it.arguments[0] }

        val result = lintConfigService.getConfig(testUserId)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should update existing config`() {
        val existingConfig =
            LintConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"identifier_format":{"value":"camel case","isActive":true}}""",
            )

        val request =
            listOf(
                RuleConfigRequest(
                    id = "identifier_format",
                    name = "Identifier Format",
                    isActive = true,
                    value = "snake case",
                ),
            )

        `when`(lintConfigRepository.findByUserId(testUserId)).thenReturn(existingConfig)
        `when`(lintConfigRepository.save(org.mockito.kotlin.any())).thenAnswer { it.arguments[0] }

        val result = lintConfigService.updateConfig(testUserId, request)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        assertEquals("snake case", result[0].value)
    }

    @Test
    fun `should create new config when updating non-existent config`() {
        val request =
            listOf(
                RuleConfigRequest(
                    id = "identifier_format",
                    name = "Identifier Format",
                    isActive = true,
                    value = "snake case",
                ),
            )

        `when`(lintConfigRepository.findByUserId(testUserId)).thenReturn(null)
        `when`(lintConfigRepository.save(org.mockito.kotlin.any())).thenAnswer { it.arguments[0] }

        val result = lintConfigService.updateConfig(testUserId, request)

        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `should get config as JSON string with only active rules`() {
        val config =
            LintConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"identifier_format":{"value":"camel case","isActive":true},"other_rule":{"value":"test","isActive":false}}""",
            )

        `when`(lintConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = lintConfigService.getConfigJson(testUserId)

        assertNotNull(result)
        // Should contain active rules
        assertTrue(result.contains("identifier_format"))
        // Should NOT contain inactive rules
        assertTrue(!result.contains("other_rule"))
    }
}
