package com.ingsis.grupo10.snippet.config.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.models.FormatConfig
import com.ingsis.grupo10.snippet.repository.FormatConfigRepository
import com.ingsis.grupo10.snippet.service.FormatConfigService
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
class FormatConfigServiceTest {
    @MockitoBean
    private lateinit var formatConfigRepository: FormatConfigRepository

    private lateinit var formatConfigService: FormatConfigService
    private lateinit var objectMapper: ObjectMapper

    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        formatConfigService = FormatConfigService(formatConfigRepository, objectMapper)
    }

    @Test
    fun `should get config for user`() {
        val config =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"space_before_colon":false,"space_after_colon":true,"indent_inside_block":4}""",
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = formatConfigService.getConfig(testUserId)

        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(false, result.spaceBeforeColon)
        assertEquals(true, result.spaceAfterColon)
        assertEquals(4, result.indentInsideBlock)
        verify(formatConfigRepository, times(1)).findByUserId(testUserId)
    }

    @Test
    fun `should create default config when user has no config`() {
        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(null)
        `when`(formatConfigRepository.save(any(FormatConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = formatConfigService.getConfig(testUserId)

        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(false, result.spaceBeforeColon)
        assertEquals(true, result.spaceAfterColon)
        assertEquals(4, result.indentInsideBlock)
        verify(formatConfigRepository, times(1)).findByUserId(testUserId)
        verify(formatConfigRepository, times(1)).save(any(FormatConfig::class.java))
    }

    @Test
    fun `should update existing config`() {
        val existingConfig =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"space_before_colon":false}""",
            )

        val request =
            FormatConfigRequest(
                spaceBeforeColon = true,
                spaceAfterColon = false,
                spaceAroundEquals = true,
                newlineBeforePrintln = 2,
                indentInsideBlock = 2,
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(existingConfig)
        `when`(formatConfigRepository.save(any(FormatConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = formatConfigService.updateConfig(testUserId, request)

        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(true, result.spaceBeforeColon)
        assertEquals(false, result.spaceAfterColon)
        assertEquals(true, result.spaceAroundEquals)
        assertEquals(2, result.newlineBeforePrintln)
        assertEquals(2, result.indentInsideBlock)
        verify(formatConfigRepository, times(1)).save(any(FormatConfig::class.java))
    }

    @Test
    fun `should create new config when updating non-existent config`() {
        val request =
            FormatConfigRequest(
                spaceBeforeColon = true,
                indentInsideBlock = 8,
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(null)
        `when`(formatConfigRepository.save(any(FormatConfig::class.java))).thenAnswer { it.arguments[0] }

        val result = formatConfigService.updateConfig(testUserId, request)

        assertNotNull(result)
        assertEquals(testUserId, result.userId)
        assertEquals(true, result.spaceBeforeColon)
        assertEquals(8, result.indentInsideBlock)
        verify(formatConfigRepository, times(1)).save(any(FormatConfig::class.java))
    }

    @Test
    fun `should get config as JSON string`() {
        val config =
            FormatConfig(
                id = UUID.randomUUID(),
                userId = testUserId,
                config = """{"indent_inside_block":4}""",
            )

        `when`(formatConfigRepository.findByUserId(testUserId)).thenReturn(config)

        val result = formatConfigService.getConfigJson(testUserId)

        assertNotNull(result)
        assertEquals("""{"indent_inside_block":4}""", result)
    }
}
