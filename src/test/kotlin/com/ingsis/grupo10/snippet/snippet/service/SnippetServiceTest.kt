package com.ingsis.grupo10.snippet.snippet.service

import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.log.LintStatus
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.service.LogService
import com.ingsis.grupo10.snippet.service.SnippetService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@SpringBootTest
class SnippetServiceTest {
    @MockitoBean
    private lateinit var snippetRepository: SnippetRepository

    @MockitoBean
    private lateinit var languageRepository: LanguageRepository

    @MockitoBean
    private lateinit var printScriptClient: PrintScriptClient

    @MockitoBean
    private lateinit var logService: LogService

    private lateinit var snippetService: SnippetService

    private lateinit var testLanguage: Language
    private lateinit var testSnippet: Snippet
    private lateinit var testRequest: SnippetCreateRequest

    @BeforeEach
    fun setUp() {
        snippetService = SnippetService(snippetRepository, languageRepository, printScriptClient, logService)

        testLanguage =
            Language(
                id = UUID.randomUUID(),
                name = "PrintScript",
            )

        testSnippet =
            Snippet(
                id = UUID.randomUUID(),
                name = "Test Snippet",
                code = "let x: number = 5;\nprintln(x);",
                language = testLanguage,
                description = "Test description",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        testRequest =
            SnippetCreateRequest(
                name = "Test Snippet",
                description = "Test description",
                code = "let x: number = 5;\nprintln(x);",
                languageName = "PrintScript",
                version = "1.1",
            )
    }

    @Test
    fun `should get snippet by id successfully`() {
        `when`(snippetRepository.findById(testSnippet.id)).thenReturn(Optional.of(testSnippet))

        val result = snippetService.getSnippetById(testSnippet.id)

        assertNotNull(result)
        assertEquals(testSnippet.id, result.id)
        assertEquals(testSnippet.name, result.name)
        assertEquals(testSnippet.description, result.description)
        assertEquals(testSnippet.language.name, result.language)
        assertEquals(testSnippet.version, result.version)
        verify(snippetRepository, times(1)).findById(testSnippet.id)
    }

    @Test
    fun `should throw exception when snippet not found by id`() {
        val nonExistentId = UUID.randomUUID()
        `when`(snippetRepository.findById(nonExistentId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                snippetService.getSnippetById(nonExistentId)
            }

        assertEquals("Snippet not found", exception.message)
        verify(snippetRepository, times(1)).findById(nonExistentId)
    }

    @Test
    fun `should get all snippets successfully`() {
        val snippets = listOf(testSnippet)
        val lintStatus =
            LintStatus(
                snippetId = testSnippet.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            )
        `when`(snippetRepository.findAll()).thenReturn(snippets)
        `when`(logService.getLatestLintStatus(testSnippet.id)).thenReturn(lintStatus)

        val result = snippetService.getAllSnippets()

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(testSnippet.id, result[0].id)
        assertEquals(testSnippet.name, result[0].name)
        assertEquals("pending", result[0].compliance)
        verify(snippetRepository, times(1)).findAll()
        verify(logService, times(1)).getLatestLintStatus(testSnippet.id)
    }

    @Test
    fun `should return empty list when no snippets exist`() {
        `when`(snippetRepository.findAll()).thenReturn(emptyList())

        val result = snippetService.getAllSnippets()

        assertNotNull(result)
        assertEquals(0, result.size)
        verify(snippetRepository, times(1)).findAll()
    }

    @Test
    fun `should create snippet successfully`() {
        `when`(printScriptClient.validateSnippet(testRequest.code, testRequest.version)).thenReturn(ValidationResult.Success)
        `when`(languageRepository.findByName(testRequest.languageName)).thenReturn(testLanguage)
        `when`(snippetRepository.save(any(Snippet::class.java))).thenAnswer { it.arguments[0] }

        val result = snippetService.createSnippet(testRequest)

        assertNotNull(result)
        assertEquals(testRequest.name, result.name)
        assertEquals(testRequest.description, result.description)
        assertEquals(testRequest.languageName, result.language)
        assertEquals(testRequest.version, result.version)
        assertNotNull(result.id)
        assertNotNull(result.ownerId)
        assertNotNull(result.createdAt)
        verify(languageRepository, times(1)).findByName(testRequest.languageName)
        verify(snippetRepository, times(1)).save(any(Snippet::class.java))
    }

    @Test
    fun `should throw exception when creating snippet with unsupported language`() {
        `when`(printScriptClient.validateSnippet(testRequest.code, testRequest.version)).thenReturn(ValidationResult.Success)
        `when`(languageRepository.findByName(testRequest.languageName)).thenReturn(null)

        val exception =
            assertThrows<IllegalArgumentException> {
                snippetService.createSnippet(testRequest)
            }

        assertEquals("Language not supported", exception.message)
        verify(languageRepository, times(1)).findByName(testRequest.languageName)
        verify(snippetRepository, never()).save(any(Snippet::class.java))
    }

    @Test
    fun `should delete snippet by id successfully`() {
        `when`(snippetRepository.existsById(testSnippet.id)).thenReturn(true)

        snippetService.deleteSnippetById(testSnippet.id)

        verify(snippetRepository, times(1)).existsById(testSnippet.id)
        verify(snippetRepository, times(1)).deleteById(testSnippet.id)
    }

    @Test
    fun `should throw exception when deleting non-existent snippet`() {
        val nonExistentId = UUID.randomUUID()
        `when`(snippetRepository.existsById(nonExistentId)).thenReturn(false)

        val exception =
            assertThrows<IllegalArgumentException> {
                snippetService.deleteSnippetById(nonExistentId)
            }

        assertEquals("Snippet not found", exception.message)
        verify(snippetRepository, times(1)).existsById(nonExistentId)
        verify(snippetRepository, never()).deleteById(any())
    }

    @Test
    fun `should update snippet successfully`() {
        val updateRequest =
            SnippetCreateRequest(
                name = "Updated Snippet",
                description = "Updated description",
                code = "let y: number = 10;\nprintln(y);",
                languageName = "PrintScript",
                version = "1.1",
            )

        `when`(snippetRepository.findById(testSnippet.id)).thenReturn(Optional.of(testSnippet))
        `when`(printScriptClient.validateSnippet(updateRequest.code, updateRequest.version)).thenReturn(ValidationResult.Success)
        `when`(languageRepository.findByName(updateRequest.languageName)).thenReturn(testLanguage)
        `when`(snippetRepository.save(any(Snippet::class.java))).thenAnswer { it.arguments[0] }

        val result = snippetService.updateSnippet(testSnippet.id, updateRequest)

        assertNotNull(result)
        assertEquals(testSnippet.id, result.id)
        assertEquals(updateRequest.name, result.name)
        assertEquals(updateRequest.description, result.description)
        assertEquals(updateRequest.languageName, result.language)
        assertEquals(updateRequest.version, result.version)
        verify(snippetRepository, times(1)).findById(testSnippet.id)
        verify(languageRepository, times(1)).findByName(updateRequest.languageName)
        verify(snippetRepository, times(1)).save(any(Snippet::class.java))
    }

    @Test
    fun `should throw exception when updating non-existent snippet`() {
        val nonExistentId = UUID.randomUUID()
        `when`(snippetRepository.findById(nonExistentId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                snippetService.updateSnippet(nonExistentId, testRequest)
            }

        assertEquals("Snippet not found", exception.message)
        verify(snippetRepository, times(1)).findById(nonExistentId)
        verify(languageRepository, never()).findByName(anyString())
        verify(snippetRepository, never()).save(any(Snippet::class.java))
    }

    @Test
    fun `should throw exception when updating snippet with unsupported language`() {
        `when`(snippetRepository.findById(testSnippet.id)).thenReturn(Optional.of(testSnippet))
        `when`(printScriptClient.validateSnippet(testRequest.code, testRequest.version)).thenReturn(ValidationResult.Success)
        `when`(languageRepository.findByName(testRequest.languageName)).thenReturn(null)

        val exception =
            assertThrows<IllegalArgumentException> {
                snippetService.updateSnippet(testSnippet.id, testRequest)
            }

        assertEquals("Language not supported", exception.message)
        verify(snippetRepository, times(1)).findById(testSnippet.id)
        verify(languageRepository, times(1)).findByName(testRequest.languageName)
        verify(snippetRepository, never()).save(any(Snippet::class.java))
    }

    @Test
    fun `should filter snippets by name`() {
        val snippet1 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Hello World",
                code = "println(\"Hello\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        val snippet2 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Goodbye World",
                code = "println(\"Bye\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val snippets = listOf(snippet1, snippet2)
        `when`(snippetRepository.findAll()).thenReturn(snippets)
        `when`(logService.getLatestLintStatus(snippet1.id)).thenReturn(
            LintStatus(
                snippetId = snippet1.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )
        `when`(logService.getLatestLintStatus(snippet2.id)).thenReturn(
            LintStatus(
                snippetId = snippet2.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )

        val result = snippetService.getAllSnippets(name = "Hello")

        assertEquals(1, result.size)
        assertEquals("Hello World", result[0].name)
    }

    @Test
    fun `should filter snippets by compliance status`() {
        val snippet1 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Valid Snippet",
                code = "println(\"Valid\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        val snippet2 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Invalid Snippet",
                code = "println(\"Invalid\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val snippets = listOf(snippet1, snippet2)
        `when`(snippetRepository.findAll()).thenReturn(snippets)
        `when`(logService.getLatestLintStatus(snippet1.id)).thenReturn(
            LintStatus(
                snippetId = snippet1.id,
                status = "valid",
                lastLintDate = LocalDateTime.now(),
                errors = emptyList(),
            ),
        )
        `when`(logService.getLatestLintStatus(snippet2.id)).thenReturn(
            LintStatus(
                snippetId = snippet2.id,
                status = "invalid",
                lastLintDate = LocalDateTime.now(),
                errors = emptyList(),
            ),
        )

        val result = snippetService.getAllSnippets(compliance = "valid")

        assertEquals(1, result.size)
        assertEquals("Valid Snippet", result[0].name)
        assertEquals("valid", result[0].compliance)
    }

    @Test
    fun `should sort snippets by name ascending`() {
        val snippet1 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Zebra",
                code = "println(\"Z\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        val snippet2 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Apple",
                code = "println(\"A\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val snippets = listOf(snippet1, snippet2)
        `when`(snippetRepository.findAll()).thenReturn(snippets)
        `when`(logService.getLatestLintStatus(snippet1.id)).thenReturn(
            LintStatus(
                snippetId = snippet1.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )
        `when`(logService.getLatestLintStatus(snippet2.id)).thenReturn(
            LintStatus(
                snippetId = snippet2.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )

        val result = snippetService.getAllSnippets(sortBy = "name", sortDirection = "ASC")

        assertEquals(2, result.size)
        assertEquals("Apple", result[0].name)
        assertEquals("Zebra", result[1].name)
    }

    @Test
    fun `should sort snippets by name descending`() {
        val snippet1 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Apple",
                code = "println(\"A\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        val snippet2 =
            Snippet(
                id = UUID.randomUUID(),
                name = "Zebra",
                code = "println(\"Z\");",
                language = testLanguage,
                description = "Test",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val snippets = listOf(snippet1, snippet2)
        `when`(snippetRepository.findAll()).thenReturn(snippets)
        `when`(logService.getLatestLintStatus(snippet1.id)).thenReturn(
            LintStatus(
                snippetId = snippet1.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )
        `when`(logService.getLatestLintStatus(snippet2.id)).thenReturn(
            LintStatus(
                snippetId = snippet2.id,
                status = "pending",
                lastLintDate = null,
                errors = emptyList(),
            ),
        )

        val result = snippetService.getAllSnippets(sortBy = "name", sortDirection = "DESC")

        assertEquals(2, result.size)
        assertEquals("Zebra", result[0].name)
        assertEquals("Apple", result[1].name)
    }
}
