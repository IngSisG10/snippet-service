package com.ingsis.grupo10.snippet.log.service

import com.ingsis.grupo10.snippet.dto.validation.LintErrorDTO
import com.ingsis.grupo10.snippet.dto.validation.LintResultDTO
import com.ingsis.grupo10.snippet.dto.validation.ValidationError
import com.ingsis.grupo10.snippet.models.Data
import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Log
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.models.Tag
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.DataRepository
import com.ingsis.grupo10.snippet.repository.LogRepository
import com.ingsis.grupo10.snippet.repository.TagRepository
import com.ingsis.grupo10.snippet.service.LogService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.any
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Test as JUnitTest

@SpringBootTest
class LogServiceTest {
    @MockitoBean
    private lateinit var logRepository: LogRepository

    @MockitoBean
    private lateinit var tagRepository: TagRepository

    @MockitoBean
    private lateinit var dataRepository: DataRepository

    private lateinit var logService: LogService

    private lateinit var testSnippet: Snippet
    private lateinit var testTest: Test
    private lateinit var validationTag: Tag
    private lateinit var lintTag: Tag

    @BeforeEach
    fun setUp() {
        logService = LogService(logRepository, tagRepository, dataRepository)

        val language = Language(UUID.randomUUID(), "PrintScript")
        testSnippet =
            Snippet(
                id = UUID.randomUUID(),
                name = "Test Snippet",
                code = "let x: number = 5;",
                language = language,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        testTest =
            Test(
                id = UUID.randomUUID(),
                snippet = testSnippet,
                name = "Test Case",
                inputs = "5",
                expectedOutputs = "5",
            )

        validationTag = Tag(UUID.randomUUID(), "validation")
        lintTag = Tag(UUID.randomUUID(), "lint")
    }

    @JUnitTest
    fun `should log validation with no errors`() {
        `when`(tagRepository.findByName("validation")).thenReturn(validationTag)
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logValidation(testSnippet, emptyList())

        assertNotNull(result)
        assertEquals(validationTag, result.tag)
        assertEquals(testSnippet, result.snippet)
        verify(logRepository, times(1)).save(any(Log::class.java))
        verify(dataRepository, times(1)).save(any(Data::class.java))
    }

    @JUnitTest
    fun `should log validation with errors`() {
        val errors =
            listOf(
                ValidationError("Syntax error", 1, 5, "unexpected_token"),
                ValidationError("Type error", 2, 10, "type_mismatch"),
            )

        `when`(tagRepository.findByName("validation")).thenReturn(validationTag)
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logValidation(testSnippet, errors)

        assertNotNull(result)
        verify(dataRepository, times(9)).save(any(Data::class.java)) // status + 2 errors * 4 fields
    }

    @JUnitTest
    fun `should log linting result`() {
        val lintResult =
            LintResultDTO(
                errors =
                    listOf(
                        LintErrorDTO("Invalid identifier format", "identifier_format", 1),
                    ),
            )

        `when`(tagRepository.findByName("lint")).thenReturn(lintTag)
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logLinting(testSnippet, lintResult)

        assertNotNull(result)
        assertEquals(lintTag, result.tag)
        verify(dataRepository, times(4)).save(any(Data::class.java)) // status + error data
    }

    @JUnitTest
    fun `should log formatting`() {
        val formattedCode = "let x : number = 5;"
        val rules = """{"space_before_colon":true}"""

        `when`(tagRepository.findByName("format")).thenReturn(Tag(UUID.randomUUID(), "format"))
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logFormatting(testSnippet, formattedCode, rules)

        assertNotNull(result)
        verify(dataRepository, times(3)).save(any(Data::class.java))
    }

    @JUnitTest
    fun `should get latest lint status when log exists`() {
        val log =
            Log(
                id = UUID.randomUUID(),
                tag = lintTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val dataEntries =
            listOf(
                Data(UUID.randomUUID(), log, "status", "valid"),
            )

        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "lint"))
            .thenReturn(log)
        `when`(dataRepository.findByLogId(log.id)).thenReturn(dataEntries)

        val result = logService.getLatestLintStatus(testSnippet.id)

        assertNotNull(result)
        assertEquals("valid", result.status)
        assertEquals(testSnippet.id, result.snippetId)
    }

    @JUnitTest
    fun `should return pending status when no lint log exists`() {
        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "lint"))
            .thenReturn(null)

        val result = logService.getLatestLintStatus(testSnippet.id)

        assertNotNull(result)
        assertEquals("pending", result.status)
        assertNull(result.lastLintDate)
    }

    @JUnitTest
    fun `should get formatted version`() {
        val formattedCode = "formatted code"
        val log =
            Log(
                id = UUID.randomUUID(),
                tag = Tag(UUID.randomUUID(), "format"),
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val dataEntries =
            listOf(
                Data(UUID.randomUUID(), log, "formatted_code", formattedCode),
            )

        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "format"))
            .thenReturn(log)
        `when`(dataRepository.findByLogId(log.id)).thenReturn(dataEntries)

        val result = logService.getFormattedVersion(testSnippet.id)

        assertEquals(formattedCode, result)
    }

    @JUnitTest
    fun `should return null when no formatted version exists`() {
        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "format"))
            .thenReturn(null)

        val result = logService.getFormattedVersion(testSnippet.id)

        assertNull(result)
    }

    @JUnitTest
    fun `should log test execution`() {
        `when`(tagRepository.findByName("test_execution")).thenReturn(Tag(UUID.randomUUID(), "test_execution"))
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logTestExecution(testTest, "5", "5", true, 100L)

        assertNotNull(result)
        verify(dataRepository, times(4)).save(any(Data::class.java))
    }

    @JUnitTest
    fun `should get test execution history`() {
        val log =
            Log(
                id = UUID.randomUUID(),
                tag = Tag(UUID.randomUUID(), "test_execution"),
                snippet = testSnippet,
                test = testTest,
                date = LocalDateTime.now(),
            )

        val dataEntries =
            listOf(
                Data(UUID.randomUUID(), log, "status", "passed"),
                Data(UUID.randomUUID(), log, "actual_output", "5"),
                Data(UUID.randomUUID(), log, "expected_output", "5"),
            )

        `when`(logRepository.findByTestId(testTest.id)).thenReturn(listOf(log))
        `when`(dataRepository.findByLogId(log.id)).thenReturn(dataEntries)

        val result = logService.getTestExecutionHistory(testTest.id)

        assertEquals(1, result.size)
        assertEquals("passed", result[0].status)
    }

    @JUnitTest
    fun `should log snippet execution successfully`() {
        val snippetExecutionTag = Tag(UUID.randomUUID(), "snippet_execution")

        `when`(tagRepository.findByName("snippet_execution")).thenReturn(snippetExecutionTag)
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logSnippetExecution(testSnippet, "output text", "input text", "success")

        assertNotNull(result)
        assertEquals(snippetExecutionTag, result.tag)
        assertEquals(testSnippet, result.snippet)
        assertNull(result.test)
        verify(logRepository, times(1)).save(any(Log::class.java))
        verify(dataRepository, times(3)).save(any(Data::class.java)) // output, inputs, status
    }

    @JUnitTest
    fun `should get snippet logs without filter`() {
        val validationLog =
            Log(
                id = UUID.randomUUID(),
                tag = validationTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val lintLog =
            Log(
                id = UUID.randomUUID(),
                tag = lintTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        `when`(logRepository.findBySnippetIdOrderByDateDesc(testSnippet.id)).thenReturn(listOf(validationLog, lintLog))
        `when`(dataRepository.findByLogId(validationLog.id)).thenReturn(
            listOf(Data(UUID.randomUUID(), validationLog, "status", "valid")),
        )
        `when`(dataRepository.findByLogId(lintLog.id)).thenReturn(
            listOf(Data(UUID.randomUUID(), lintLog, "status", "valid")),
        )

        val result = logService.getSnippetLogs(testSnippet.id)

        assertEquals(2, result.size)
        assertEquals("validation", result[0].tagName)
        assertEquals("lint", result[1].tagName)
    }

    @JUnitTest
    fun `should get snippet logs filtered by tag`() {
        val lintLog =
            Log(
                id = UUID.randomUUID(),
                tag = lintTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        `when`(logRepository.findBySnippetIdAndTagName(testSnippet.id, "lint")).thenReturn(listOf(lintLog))
        `when`(dataRepository.findByLogId(lintLog.id)).thenReturn(
            listOf(Data(UUID.randomUUID(), lintLog, "status", "valid")),
        )

        val result = logService.getSnippetLogs(testSnippet.id, "lint")

        assertEquals(1, result.size)
        assertEquals("lint", result[0].tagName)
        assertEquals(testSnippet.id, result[0].snippetId)
    }

    @JUnitTest
    fun `should parse multiple lint errors from data`() {
        val log =
            Log(
                id = UUID.randomUUID(),
                tag = lintTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        val dataEntries =
            listOf(
                Data(UUID.randomUUID(), log, "status", "invalid"),
                Data(UUID.randomUUID(), log, "error_0_message", "First error"),
                Data(UUID.randomUUID(), log, "error_0_rule", "rule1"),
                Data(UUID.randomUUID(), log, "error_0_line", "1"),
                Data(UUID.randomUUID(), log, "error_0_column", "5"),
                Data(UUID.randomUUID(), log, "error_1_message", "Second error"),
                Data(UUID.randomUUID(), log, "error_1_rule", "rule2"),
                Data(UUID.randomUUID(), log, "error_1_line", "2"),
                Data(UUID.randomUUID(), log, "error_1_column", "10"),
            )

        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "lint"))
            .thenReturn(log)
        `when`(dataRepository.findByLogId(log.id)).thenReturn(dataEntries)

        val result = logService.getLatestLintStatus(testSnippet.id)

        assertNotNull(result)
        assertEquals("invalid", result.status)
        assertEquals(2, result.errors.size)
        assertEquals("First error", result.errors[0].message)
        assertEquals("Second error", result.errors[1].message)
    }

    @JUnitTest
    fun `should handle test execution with duration`() {
        `when`(tagRepository.findByName("test_execution")).thenReturn(Tag(UUID.randomUUID(), "test_execution"))
        `when`(logRepository.save(any(Log::class.java))).thenAnswer { it.arguments[0] }
        `when`(dataRepository.save(any(Data::class.java))).thenAnswer { it.arguments[0] }

        val result = logService.logTestExecution(testTest, "actual", "expected", false, 250L)

        assertNotNull(result)
        verify(dataRepository, times(4)).save(any(Data::class.java)) // status, actual, expected, duration
    }

    @JUnitTest
    fun `should throw IllegalStateException when validation tag is not found`() {
        `when`(tagRepository.findByName("validation")).thenReturn(null)

        val exception =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                logService.logValidation(testSnippet, emptyList())
            }

        assertEquals("Tag 'validation' not found. Please seed the database.", exception.message)
    }

    @JUnitTest
    fun `should throw IllegalStateException when lint tag is not found`() {
        `when`(tagRepository.findByName("lint")).thenReturn(null)

        val exception =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                logService.logLinting(testSnippet, LintResultDTO(emptyList()))
            }

        assertEquals("Tag 'lint' not found. Please seed the database.", exception.message)
    }

    @JUnitTest
    fun `should throw IllegalStateException when format tag is not found`() {
        `when`(tagRepository.findByName("format")).thenReturn(null)

        val exception =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                logService.logFormatting(testSnippet, "formatted code", "{}")
            }

        assertEquals("Tag 'format' not found. Please seed the database.", exception.message)
    }

    @JUnitTest
    fun `should throw IllegalStateException when test_execution tag is not found`() {
        `when`(tagRepository.findByName("test_execution")).thenReturn(null)

        val exception =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                logService.logTestExecution(testTest, "actual", "expected", true)
            }

        assertEquals("Tag 'test_execution' not found. Please seed the database.", exception.message)
    }

    @JUnitTest
    fun `should throw IllegalStateException when snippet_execution tag is not found`() {
        `when`(tagRepository.findByName("snippet_execution")).thenReturn(null)

        val exception =
            org.junit.jupiter.api.assertThrows<IllegalStateException> {
                logService.logSnippetExecution(testSnippet, "output", "inputs", "success")
            }

        assertEquals("Tag 'snippet_execution' not found. Please seed the database.", exception.message)
    }

    @JUnitTest
    fun `getLatestLintStatus should return pending when dataMap has no status`() {
        val log =
            Log(
                id = UUID.randomUUID(),
                tag = lintTag,
                snippet = testSnippet,
                test = null,
                date = LocalDateTime.now(),
            )

        `when`(logRepository.findFirstBySnippetIdAndTagNameOrderByDateDesc(testSnippet.id, "lint"))
            .thenReturn(log)
        `when`(dataRepository.findByLogId(log.id)).thenReturn(emptyList())

        val result = logService.getLatestLintStatus(testSnippet.id)

        assertEquals("pending", result.status)
    }

    @JUnitTest
    fun `getTestExecutionHistory should return empty list when no logs are found`() {
        `when`(logRepository.findByTestId(testTest.id)).thenReturn(emptyList())

        val result = logService.getTestExecutionHistory(testTest.id)

        assertEquals(0, result.size)
    }
}
