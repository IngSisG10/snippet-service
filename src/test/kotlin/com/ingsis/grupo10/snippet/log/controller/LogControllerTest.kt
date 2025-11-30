package com.ingsis.grupo10.snippet.log.controller

import com.ingsis.grupo10.snippet.controller.LogController
import com.ingsis.grupo10.snippet.dto.log.LintStatus
import com.ingsis.grupo10.snippet.dto.log.LogDto
import com.ingsis.grupo10.snippet.dto.log.TestExecutionResult
import com.ingsis.grupo10.snippet.service.LogService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(LogController::class)
@AutoConfigureMockMvc(addFilters = false)
class LogControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var logService: LogService

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

        mockMvc
            .perform(get("/logs/snippets/$snippetId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].tagName").value("validation"))
            .andExpect(jsonPath("$[0].snippetId").value(snippetId.toString()))
    }

    @Test
    fun `should get snippet logs filtered by tag`() {
        val logs =
            listOf(
                LogDto(
                    id = UUID.randomUUID(),
                    tagName = "lint",
                    snippetId = snippetId,
                    testId = null,
                    date = LocalDateTime.now(),
                    data = mapOf("status" to "valid"),
                ),
            )

        `when`(logService.getSnippetLogs(snippetId, "lint")).thenReturn(logs)

        mockMvc
            .perform(get("/logs/snippets/$snippetId?tag=lint"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].tagName").value("lint"))
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

        mockMvc
            .perform(get("/logs/snippets/$snippetId/lint-status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("valid"))
            .andExpect(jsonPath("$.snippetId").value(snippetId.toString()))
    }

    @Test
    fun `should get formatted version when exists`() {
        val formattedCode = "let x : number = 5;"

        `when`(logService.getFormattedVersion(snippetId)).thenReturn(formattedCode)

        mockMvc
            .perform(get("/logs/snippets/$snippetId/formatted"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 404 when formatted version does not exist`() {
        `when`(logService.getFormattedVersion(snippetId)).thenReturn(null)

        mockMvc
            .perform(get("/logs/snippets/$snippetId/formatted"))
            .andExpect(status().isNotFound)
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
                    durationMs = 100L,
                    executedAt = LocalDateTime.now(),
                ),
            )

        `when`(logService.getTestExecutionHistory(testId)).thenReturn(history)

        mockMvc
            .perform(get("/logs/tests/$testId/executions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("passed"))
            .andExpect(jsonPath("$[0].testId").value(testId.toString()))
    }
}
