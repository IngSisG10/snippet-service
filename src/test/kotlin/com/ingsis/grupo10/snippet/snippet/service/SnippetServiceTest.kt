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
import com.ingsis.grupo10.snippet.util.AssetUtils.parseCodeUrl
import com.ingsis.grupo10.snippet.util.UserContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
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
    private val testUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val testUserIdString = testUserId.toString()
    private val differentUserId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        snippetRepository = mockk()
        languageRepository = mockk()
        printScriptClient = mockk()
        logService = mockk(relaxed = true)
        lintConfigService = mockk()
        formatConfigService = mockk()
        assetClient = mockk()

     /*   val code = "let x: number = 5;"
        val bucket = "snippets"
        val codeUrl = "/$bucket/any-snippet-id"

        every { assetClient.createAsset(bucket, any(), code) } returns "/$bucket/${id}"
        every { assetClient.getAsset(bucket, any()) } returns code*/

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

        // Mock UserContext
        mockkObject(UserContext)
        every { UserContext.getCurrentUserId() } returns testUserId
        every { UserContext.toUuidOrThrow(testUserIdString, any()) } returns testUserId
        every { UserContext.toUuidOrThrow("invalid-uuid", any()) } throws IllegalArgumentException("Invalid userId format")
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UserContext)
    }

    // ========== getSnippetById Tests ==========

    @Test
    fun `getSnippetById should return snippet when found`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)

        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"

        val result = snippetService.getSnippetById(snippetId)

        // codeUrl -> (container, key)
        val (snippetContainer, snippetKey) = parseCodeUrl(codeUrl = snippet.codeUrl)
        val expectedCode = assetClient.getAsset(snippetContainer, snippetKey)

        val (resultContainer, resultKey) = parseCodeUrl(codeUrl = result.codeUrl)
        val resultCode = assetClient.getAsset(resultContainer, resultKey)

        assertEquals(snippet.id, result.id)
        assertEquals(snippet.name, result.name)
        assertEquals(expectedCode, resultCode)
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
                createTestSnippet(UUID.randomUUID(), testUserId),
                createTestSnippet(UUID.randomUUID(), testUserId),
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
                createTestSnippet(UUID.randomUUID(), testUserId, name = "Test Snippet"),
                createTestSnippet(UUID.randomUUID(), testUserId, name = "Another Snippet"),
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
                createTestSnippet(UUID.randomUUID(), testUserId),
                createTestSnippet(UUID.randomUUID(), testUserId, language = Language(UUID.randomUUID(), "javascript")),
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
                createTestSnippet(UUID.randomUUID(), testUserId, name = "Zebra"),
                createTestSnippet(UUID.randomUUID(), testUserId, name = "Apple"),
            )
        every { snippetRepository.findAll() } returns snippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getAllSnippets(sortBy = "name", sortDirection = "ASC")

        assertEquals("Apple", result[0].name)
        assertEquals("Zebra", result[1].name)
    }

    // ========== createSnippet Tests ==========

    @Test
    fun `createSnippet should create snippet with provided userId`() {
        val request = createTestRequest()
        val snippetId = UUID.randomUUID()
        val createdSnippet = createTestSnippet(snippetId, testUserId)

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { snippetRepository.save(any()) } returns createdSnippet

        every { assetClient.createAsset("snippets", any(), any()) } returns CreatedResult.Success("/snippets/fake-key")

        val result = snippetService.createSnippet(request, testUserIdString)

        assertEquals(createdSnippet.id, result.id)
        verify { snippetRepository.save(any()) }
        verify { logService.logValidation(any(), any()) }
    }

    @Test
    fun `createSnippet should create snippet with current user when userId is null`() {
        val request = createTestRequest()
        val snippetId = UUID.randomUUID()
        val createdSnippet = createTestSnippet(snippetId, testUserId)

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { snippetRepository.save(any()) } returns createdSnippet

        every { assetClient.createAsset("snippets", any(), any()) } returns CreatedResult.Success("/snippets/fake-key")

        val result = snippetService.createSnippet(request)

        assertEquals(createdSnippet.id, result.id)
        verify { snippetRepository.save(any()) }
    }

    @Test
    fun `createSnippet should throw exception when validation fails`() {
        val request = createTestRequest()
        val validationError = ValidationError("Syntax error", 1, 5, "syntax")

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Failed(listOf(validationError))

        assertThrows(SnippetValidationException::class.java) {
            snippetService.createSnippet(request)
        }
    }

    @Test
    fun `createSnippet should throw exception when language not supported`() {
        val request = createTestRequest()

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.createSnippet(request)
        }
    }

    @Test
    fun `createSnippet should throw exception when userId format is invalid`() {
        val request = createTestRequest()

        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { UserContext.toUuidOrThrow("invalid-uuid", any()) } throws IllegalArgumentException("Invalid userId format")

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.createSnippet(request, "invalid-uuid")
        }
    }

    // ========== updateSnippet Tests ==========

    @Test
    fun `updateSnippet should update snippet when user owns it`() {
        val snippetId = UUID.randomUUID()
        val existingSnippet = createTestSnippet(snippetId, testUserId)
        val request = createTestRequest(name = "Updated Snippet")
        val updatedSnippet = createTestSnippet(snippetId, testUserId, name = "Updated Snippet")

        every { snippetRepository.findById(snippetId) } returns Optional.of(existingSnippet)
        every { printScriptClient.validateSnippet(any(), any()) } returns ValidationResult.Success
        every { languageRepository.findByName("printscript") } returns testLanguage
        every { snippetRepository.save(any()) } returns updatedSnippet

        every { assetClient.createAsset("snippets", any(), any()) } returns CreatedResult.Success("/snippets/fake-key")

        val result = snippetService.updateSnippet(snippetId, request, testUserIdString)

        assertEquals("Updated Snippet", result.name)
        verify { snippetRepository.save(any()) }
    }

    @Test
    fun `updateSnippet should update snippet when userId is null`() {
        val snippetId = UUID.randomUUID()
        val existingSnippet = createTestSnippet(snippetId, testUserId)
        val request = createTestRequest(name = "Updated Snippet")
        val updatedSnippet = createTestSnippet(snippetId, testUserId, name = "Updated Snippet")

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
    fun `updateSnippet should throw exception when user does not own snippet`() {
        val snippetId = UUID.randomUUID()
        val existingSnippet = createTestSnippet(snippetId, differentUserId)
        val request = createTestRequest()

        every { snippetRepository.findById(snippetId) } returns Optional.of(existingSnippet)
        every { UserContext.toUuidOrThrow(testUserIdString, any()) } returns testUserId

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.updateSnippet(snippetId, request, testUserIdString)
        }
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
    fun `deleteSnippetById should delete snippet when user owns it`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { snippetRepository.deleteById(snippetId) } returns Unit
        every { UserContext.toUuidOrThrow(testUserIdString, any()) } returns testUserId

        snippetService.deleteSnippetById(snippetId, testUserIdString)

        verify { snippetRepository.deleteById(snippetId) }
    }

    @Test
    fun `deleteSnippetById should delete snippet when userId is null`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { snippetRepository.deleteById(snippetId) } returns Unit

        snippetService.deleteSnippetById(snippetId)

        verify { snippetRepository.deleteById(snippetId) }
    }

    @Test
    fun `deleteSnippetById should throw exception when user does not own snippet`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, differentUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { UserContext.toUuidOrThrow(testUserIdString, any()) } returns testUserId

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.deleteSnippetById(snippetId, testUserIdString)
        }
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
    fun `lintSnippet should lint snippet with provided userId`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { lintConfigService.getConfigJson(testUserId) } returns "{}"
        every { printScriptClient.lintSnippet(any(), any(), any()) } returns mockk()

        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"

        val result = snippetService.lintSnippet(snippetId, testUserId)

        assertEquals(snippet.id, result.id)
        verify { lintConfigService.getConfigJson(testUserId) }
        verify { printScriptClient.lintSnippet(any(), any(), any()) }
        verify { logService.logLinting(any(), any()) }
    }

    @Test
    fun `lintSnippet should lint snippet with current user when userId is null`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { lintConfigService.getConfigJson(testUserId) } returns "{}"
        every { printScriptClient.lintSnippet(any(), any(), any()) } returns mockk()

        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"

        val result = snippetService.lintSnippet(snippetId)

        assertEquals(snippet.id, result.id)
        verify { lintConfigService.getConfigJson(testUserId) }
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
    fun `formatSnippet should format snippet with provided userId`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { formatConfigService.getConfigJson(testUserId) } returns "{}"
        every { printScriptClient.formatSnippet(any(), any(), any()) } returns
            mockk {
                every { formattedCode } returns "formatted code"
            }

        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"

        val result = snippetService.formatSnippet(snippetId, testUserId)

        assertEquals(snippet.id, result.id)
        verify { formatConfigService.getConfigJson(testUserId) }
        verify { printScriptClient.formatSnippet(any(), any(), any()) }
        verify { logService.logFormatting(any(), any(), any()) }
    }

    @Test
    fun `formatSnippet should format snippet with current user when userId is null`() {
        val snippetId = UUID.randomUUID()
        val snippet = createTestSnippet(snippetId, testUserId)

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { formatConfigService.getConfigJson(testUserId) } returns "{}"
        every { printScriptClient.formatSnippet(any(), any(), any()) } returns
            mockk {
                every { formattedCode } returns "formatted code"
            }

        every { assetClient.getAsset(any(), any()) } returns "let x: number = 5;"

        val result = snippetService.formatSnippet(snippetId)

        assertEquals(snippet.id, result.id)
        verify { formatConfigService.getConfigJson(testUserId) }
    }

    @Test
    fun `formatSnippet should throw exception when snippet not found`() {
        val snippetId = UUID.randomUUID()

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.formatSnippet(snippetId)
        }
    }

    // ========== getSnippetsByUser Tests ==========

    @Test
    fun `getSnippetsByUser should return snippets owned by user`() {
        val userSnippets =
            listOf(
                createTestSnippet(UUID.randomUUID(), testUserId),
                createTestSnippet(UUID.randomUUID(), testUserId),
            )

        every { snippetRepository.findByOwnerId(testUserId) } returns userSnippets
        every { logService.getLatestLintStatus(any()) } returns mockk { every { status } returns "valid" }

        val result = snippetService.getSnippetsByUser(testUserIdString)

        assertEquals(2, result.size)
        assertEquals("valid", result[0].compliance)
        assertEquals("valid", result[1].compliance)
        verify { snippetRepository.findByOwnerId(testUserId) }
    }

    @Test
    fun `getSnippetsByUser should return empty list when user has no snippets`() {
        every { snippetRepository.findByOwnerId(testUserId) } returns emptyList()

        val result = snippetService.getSnippetsByUser(testUserIdString)

        assertEquals(0, result.size)
        verify { snippetRepository.findByOwnerId(testUserId) }
    }

    @Test
    fun `getSnippetsByUser should throw exception when userId format is invalid`() {
        every { UserContext.toUuidOrThrow("invalid-uuid", any()) } throws IllegalArgumentException("Invalid userId format")

        assertThrows(IllegalArgumentException::class.java) {
            snippetService.getSnippetsByUser("invalid-uuid")
        }
    }

    // ========== Helper Methods ==========

    private fun createTestSnippet(
        id: UUID = UUID.randomUUID(),
        ownerId: UUID = testUserId,
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
            ownerId = ownerId,
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
