package com.ingsis.grupo10.snippet.log.controller

import com.ingsis.grupo10.snippet.controller.LogController
import com.ingsis.grupo10.snippet.dto.log.LintStatus
import com.ingsis.grupo10.snippet.dto.log.LogDto
import com.ingsis.grupo10.snippet.dto.log.TestExecutionResult
import com.ingsis.grupo10.snippet.service.LogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.UUID

class LogControllerTest {
    private val logService: LogService = mock()
    private val logController = LogController(logService)

    private val snippetId = UUID.randomUUID()
    private val testId = UUID.randomUUID()

    @Test
    fun `should get snippet logs`() {
        val logs =
            listOf(
                LogDto(
                    id = UUID.randomUUID(),
                    tagName = "validation",
                    snippetId = snippetId,
                    testId = null,
                    date = LocalDateTime.now(),
                    data = mapOf("status" to "valid"),
                ),
            )

        `when`(logService.getSnippetLogs(snippetId, null)).thenReturn(logs)

        val response = logController.getSnippetLogs(snippetId, null)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())
        assertEquals(1, response.body?.size)
        assertEquals("validation", response.body?.get(0)?.tagName)
    }

    @Test
    fun `should get snippet logs filtered by tag`() {
        val logs =
            listOf(
                LogDto(
                    id = UUID.randomUUID(),
                    tagName = "validation",
                    snippetId = snippetId,
                    testId = null,
                    date = LocalDateTime.now(),
                    data = mapOf("status" to "valid"),
                ),
            )

        `when`(logService.getSnippetLogs(snippetId, "validation")).thenReturn(logs)

        val response = logController.getSnippetLogs(snippetId, "validation")

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `should get lint status`() {
        val lintStatus =
            LintStatus(
                snippetId = snippetId,
                status = "valid",
                lastLintDate = LocalDateTime.now(),
                errors = emptyList(),
            )

        `when`(logService.getLatestLintStatus(snippetId)).thenReturn(lintStatus)

        val response = logController.getLintStatus(snippetId)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())
        assertEquals("valid", response.body?.status)
    }

    @Test
    fun `should get formatted version when exists`() {
        val formattedCode = "let x: number = 5;"

        `when`(logService.getFormattedVersion(snippetId)).thenReturn(formattedCode)

        val response = logController.getFormattedVersion(snippetId)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())
        assertEquals(formattedCode, response.body)
    }

    @Test
    fun `should return 404 when formatted version does not exist`() {
        `when`(logService.getFormattedVersion(snippetId)).thenReturn(null)

        val response = logController.getFormattedVersion(snippetId)

        assertNotNull(response)
        assertEquals(404, response.statusCode.value())
    }

    @Test
    fun `should get test execution history`() {
        val history =
            listOf(
                TestExecutionResult(
                    logId = UUID.randomUUID(),
                    testId = testId,
                    status = "passed",
                    actualOutput = "5",
                    expectedOutput = "5",
                    durationMs = 100,
                    executedAt = LocalDateTime.now(),
                ),
            )

        `when`(logService.getTestExecutionHistory(testId)).thenReturn(history)

        val response = logController.getTestExecutionHistory(testId)

        assertNotNull(response)
        assertEquals(200, response.statusCode.value())
        assertEquals(1, response.body?.size)
        assertEquals("passed", response.body?.get(0)?.status)
    }
}
