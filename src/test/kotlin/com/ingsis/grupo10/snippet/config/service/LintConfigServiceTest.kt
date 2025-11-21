package com.ingsis.grupo10.snippet.config.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.models.LintConfig
import com.ingsis.grupo10.snippet.repository.LintConfigRepository
import com.ingsis.grupo10.snippet.service.LintConfigService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
class LintConfigServiceTest {
    @MockitoBean
    private lateinit var lintConfigRepository: LintConfigRepository

    private lateinit var lintConfigService: LintConfigService
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = UUID.randomUUID()

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
                userId = testUserId.toString(),
                config = """{"identifier_format":"camel case"}""",
            )

        `when`(lintConfigRepository.findByUserId(testUserId.toString())).thenReturn(config)

        val result = lintConfigService.getConfig(testUserId.toString())

        assertNotNull(result)
        assertEquals(testUserId.toString(), result.userId)
        assertEquals("camel case", result.identifierFormat)
        verify(lintConfigRepository, times(1)).findByUserId(testUserId.toString())
    }

    @Test
    fun `should create default config when user has no config`() {
        `when`(lintConfigRepository.findByUserId(testUserId.toString())).thenReturn(null)
        `when`(lintConfigRepository.save(any(LintConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = lintConfigService.getConfig(testUserId.toString())

        assertNotNull(result)
        assertEquals(testUserId.toString(), result.userId)
        assertEquals("camel case", result.identifierFormat)
        verify(lintConfigRepository, times(1)).findByUserId(testUserId.toString())
        verify(lintConfigRepository, times(1)).save(any(LintConfig::class.java))
    }

    @Test
    fun `should update existing config`() {
        val existingConfig =
            LintConfig(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                config = """{"identifier_format":"camel case"}""",
            )

        val request =
            LintConfigRequest(
                identifierFormat = "snake case",
                printlnExpressionAllowed = true,
                readInputExpressionAllowed = false,
            )

        `when`(lintConfigRepository.findByUserId(testUserId.toString())).thenReturn(existingConfig)
        `when`(lintConfigRepository.save(any(LintConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = lintConfigService.updateConfig(testUserId.toString(), request)

        assertNotNull(result)
        assertEquals(testUserId.toString(), result.userId)
        assertEquals("snake case", result.identifierFormat)
        assertEquals(true, result.printlnExpressionAllowed)
        assertEquals(false, result.readInputExpressionAllowed)
        verify(lintConfigRepository, times(1)).save(any(LintConfig::class.java))
    }

    @Test
    fun `should create new config when updating non-existent config`() {
        val request =
            LintConfigRequest(
                identifierFormat = "snake case",
            )

        `when`(lintConfigRepository.findByUserId(testUserId.toString())).thenReturn(null)
        `when`(lintConfigRepository.save(any(LintConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = lintConfigService.updateConfig(testUserId.toString(), request)

        assertNotNull(result)
        assertEquals(testUserId.toString(), result.userId)
        assertEquals("snake case", result.identifierFormat)
        verify(lintConfigRepository, times(1)).save(any(LintConfig::class.java))
    }

    @Test
    fun `should get config as JSON string`() {
        val config =
            LintConfig(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                config = """{"identifier_format":"camel case"}""",
            )

        `when`(lintConfigRepository.findByUserId(testUserId.toString())).thenReturn(config)

        val result = lintConfigService.getConfigJson(testUserId.toString())

        assertNotNull(result)
        assertEquals("""{"identifier_format":"camel case"}""", result)
    }
}
