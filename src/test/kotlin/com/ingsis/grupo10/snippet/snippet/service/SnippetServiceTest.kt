package com.ingsis.grupo10.snippet.snippet.service

import com.ingsis.grupo10.snippet.client.AssetClient
import com.ingsis.grupo10.snippet.client.CreatedResult
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.validation.ValidationError
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.exception.SnippetValidationException
import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.service.FormatConfigService
import com.ingsis.grupo10.snippet.service.LintConfigService
import com.ingsis.grupo10.snippet.service.LogService
import com.ingsis.grupo10.snippet.service.SnippetService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class SnippetServiceTest {
    private lateinit var snippetRepository: SnippetRepository
    private lateinit var languageRepository: LanguageRepository
    private lateinit var printScriptClient: PrintScriptClient
    private lateinit var logService: LogService
    private lateinit var lintConfigService: LintConfigService
    private lateinit var formatConfigService: FormatConfigService
    private lateinit var snippetService: SnippetService
    private lateinit var assetClient: AssetClient

    private val testLanguage = Language(id = UUID.randomUUID(), name = "printscript")

    @BeforeEach
    fun setUp() {
        snippetRepository = mockk()
        languageRepository = mockk()
        printScriptClient = mockk()
        logService = mockk(relaxed = true)
        lintConfigService = mockk()
        formatConfigService = mockk()
        assetClient = mockk()

        snippetService =
            SnippetService(
                snippetRepository,
                languageRepository,
                printScriptClient,
                assetClient,
                logService,
                lintConfigService,
                formatConfigService,
            )
    }

    // ========== getSnippetById Tests ==========

    @Test
    fun `getSnippetById should return snippet when found`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)

        val result = snippetService.getSnippetById(snippetId)

        assertEquals(snippet.id, result.id)
        assertEquals(snippet.name, result.name)
        assertEquals(snippet.language.name, result.language)
    }

    @Test
    fun `getSnippetById should throw exception when not found`() {
        val snippetId = UUID.randomUUID()
        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.getSnippetById(snippetId)
        }
    }

    // ========== getAllSnippets Tests ==========

    @Test
    fun `getAllSnippets should return all snippets with compliance status`() {
        val snippets =
            listOf(
                createTestSnippet(UUID.randomUUID()),
                createTestSnippet(UUID.randomUUID()),
            )
        every { snippetRepository.findAll() } returns snippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getAllSnippets()

        assertEquals(2, result.size)
        assertEquals("valid", result[0].compliance)
        assertEquals("valid", result[1].compliance)
    }

    @Test
    fun `getAllSnippets should filter by name`() {
        val snippets =
            listOf(
                createTestSnippet(UUID.randomUUID(), name = "Test Snippet"),
                createTestSnippet(UUID.randomUUID(), name = "Another Snippet"),
            )
        every { snippetRepository.findAll() } returns snippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getAllSnippets(name = "Test")

        assertEquals(1, result.size)
        assertEquals("Test Snippet", result[0].name)
    }

    @Test
    fun `getAllSnippets should filter by language`() {
        val snippets =
            listOf(
                createTestSnippet(UUID.randomUUID()),
                createTestSnippet(UUID.randomUUID(), language = Language(UUID.randomUUID(), "javascript")),
            )
        every { snippetRepository.findAll() } returns snippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getAllSnippets(language = "printscript")

        assertEquals(1, result.size)
        assertEquals("printscript", result[0].language)
    }

    @Test
    fun `getAllSnippets should sort by name ascending`() {
        val snippets =
            listOf(
                createTestSnippet(UUID.randomUUID(), name = "Zebra"),
                createTestSnippet(UUID.randomUUID(), name = "Apple"),
            )
        every { snippetRepository.findAll() } returns snippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getAllSnippets(sortBy = "name", sortDirection = "ASC")

        assertEquals("Apple", result[0].name)
        assertEquals("Zebra", result[1].name)
    }

    // ========== createSnippet Tests ==========

    @Test
    fun `createSnippet should create snippet successfully`() {
        val request = createTestRequest()
        val snippetId = UUID.randomUUID()
        val createdSnippet = createTestSnippet(snippetId)

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { snippetRepository.save(any()) } returns createdSnippet
        every { assetClient.createAsset("snippets", any(), any()) } returns CreatedResult.Success("/snippets/fake-key")

        val result = snippetService.createSnippet(request, snippetId)

        assertEquals(snippetId.toString(), result.id)
        verify { snippetRepository.save(any()) }
        verify { logService.logValidation(any(), any()) }
    }

    @Test
    fun `createSnippet should throw exception when validation fails`() {
        val request = createTestRequest()
        val snippetId = UUID.randomUUID()
        val validationError = ValidationError("Syntax error", 1, 5, "syntax")

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Failed(listOf(validationError))

        assertThrows(SnippetValidationException::class.java) {
            snippetService.createSnippet(request, snippetId)
        }
    }

    @Test
    fun `createSnippet should throw exception when language not supported`() {
        val request = createTestRequest()
        val snippetId = UUID.randomUUID()

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.createSnippet(request, snippetId)
        }
    }

    // ========== updateSnippet Tests ==========

    @Test
    fun `updateSnippet should update snippet successfully`() {
        val snippetId = UUID.randomUUID()
        val existingSnippet = createTestSnippet(snippetId)
        val request = createTestRequest(name = "Updated Snippet")
        val updatedSnippet = createTestSnippet(snippetId, name = "Updated Snippet")

        every { snippetRepository.findById(snippetId) } returns Optional.of(existingSnippet)
        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { snippetRepository.save(any()) } returns updatedSnippet
        every { assetClient.createAsset("snippets", any(), any()) } returns CreatedResult.Success("/snippets/fake-key")

        val result = snippetService.updateSnippet(snippetId, request)

        assertEquals("Updated Snippet", result.name)
        verify { snippetRepository.save(any()) }
    }

    @Test
    fun `updateSnippet should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()
        val request = createTestRequest()

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.updateSnippet(snippetId, request)
        }
    }

    // ========== deleteSnippetById Tests ==========

    @Test
    fun `deleteSnippetById should delete snippet successfully`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { snippetRepository.deleteById(snippetId) } returns Unit

        snippetService.deleteSnippetById(snippetId)

        verify { snippetRepository.deleteById(snippetId) }
    }

    @Test
    fun `deleteSnippetById should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.deleteSnippetById(snippetId)
        }
    }

    // ========== lintSnippet Tests ==========

    @Test
    fun `lintSnippet should lint snippet successfully`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"
        every { printScriptClient.lintSnippet(any(), any(), any()) } returns
            mockk {
                every { errors } returns emptyList()
            }

        val result = snippetService.lintSnippet(snippetId)

        assertEquals(snippet.id, result.id)
        verify { printScriptClient.lintSnippet(any(), any(), eq("{}")) }
        verify { logService.logLinting(any(), any()) }
    }

    @Test
    fun `lintSnippet should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.lintSnippet(snippetId)
        }
    }

    // ========== formatSnippet Tests ==========

    @Test
    fun `formatSnippet should format snippet and update asset`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId)
        val formatted = "let x: number = 5;"

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { assetClient.getAsset(any(), any()) } returns "let x:number=5;"
        every { printScriptClient.formatSnippet(any(), any(), any()) } returns
            mockk {
                every { formattedCode } returns formatted
            }
        every { assetClient.createAsset(any(), any(), any()) } returns CreatedResult.Success("/snippets/key")

        val result = snippetService.formatSnippet(snippetId)

        assertEquals(snippet.id, result.id)
        verify { printScriptClient.formatSnippet(any(), any(), eq("""{"enforce-spacing-around-equals": true}""")) }
        verify { assetClient.createAsset("snippets", snippetId.toString(), formatted) }
        verify { logService.logFormatting(any(), eq(formatted), any()) }
    }

    @Test
    fun `formatSnippet should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.formatSnippet(snippetId)
        }
    }

    // ========== Helper Methods ==========

    private fun createTestSnippet(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Snippet",
        language: Language = testLanguage,
    ): Snippet =
        Snippet(
            id = id,
            name = name,
            codeUrl = "snippets/$id",
            language = language,
            description = "Test description",
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun createTestRequest(
        name: String = "Test Snippet",
        languageName: String = "printscript",
    ): SnippetCreateRequest =
        SnippetCreateRequest(
            name = name,
            description = "Test description",
            code = "let x: number = 5;",
            languageName = languageName,
            version = "1.0",
        )
}
