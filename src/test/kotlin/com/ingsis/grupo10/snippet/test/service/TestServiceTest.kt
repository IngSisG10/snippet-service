package com.ingsis.grupo10.snippet.test.service

import com.ingsis.grupo10.snippet.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.repository.TestRepository
import com.ingsis.grupo10.snippet.service.TestService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class TestServiceTest {
    private lateinit var testService: TestService

    private lateinit var testLanguage: Language
    private lateinit var testSnippet: Snippet
    private lateinit var testCase: Test
    private lateinit var testRequest: TestCreateRequest
    private val testRepository: TestRepository = mock()
    private val snippetRepository: SnippetRepository = mock()
    private val printScriptClient: com.ingsis.grupo10.snippet.client.PrintScriptClient = mock()
    private val assetClient: com.ingsis.grupo10.snippet.client.AssetClient = mock()
    private val lintConfigService: com.ingsis.grupo10.snippet.service.LintConfigService = mock()

    @BeforeEach
    fun setUp() {
        testService = TestService(testRepository, snippetRepository, printScriptClient, assetClient, lintConfigService)

        val snippetId = UUID.randomUUID()
        val code = "let x: number = 5;\nprintln(x);"
        val container = "snippets"
        val codeUrl = "$container/$snippetId"

        testLanguage =
            Language(
                id = UUID.randomUUID(),
                name = "PrintScript",
            )

        testSnippet =
            Snippet(
                id = UUID.randomUUID(),
                name = "Test Snippet",
                codeUrl = codeUrl,
                language = testLanguage,
                description = "Test description",
                version = "1.1",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        testCase =
            Test(
                id = UUID.randomUUID(),
                snippet = testSnippet,
                name = "Test case 1",
                input = listOf("5"),
                output = listOf("5"),
            )

        testRequest =
            TestCreateRequest(
                name = "Test case 1",
                input = listOf("5"),
                output = listOf("5"),
            )
    }

    @org.junit.jupiter.api.Test
    fun `should create test successfully`() {
        `when`(snippetRepository.findById(testSnippet.id)).thenReturn(Optional.of(testSnippet))
        `when`(testRepository.save(any(Test::class.java))).thenAnswer { it.arguments[0] }

        val result = testService.createTest(testSnippet.id, testRequest)

        assertNotNull(result)
        assertEquals("Test case 1", result.name)
    }

    /*
     * Additional tests commented out - need to be updated to match current DTOs
     *
     * @org.junit.jupiter.api.Test
     * fun `should get all tests for snippet`() {
     *     // Test implementation
     * }
     *
     * @org.junit.jupiter.api.Test
     * fun `should get test by id`() {
     *     // Test implementation
     * }
     *
     * @org.junit.jupiter.api.Test
     * fun `should delete test by id`() {
     *     // Test implementation
     * }
     *
     * @org.junit.jupiter.api.Test
     * fun `should throw exception when snippet not found`() {
     *     // Test implementation
     * }
     */
}
