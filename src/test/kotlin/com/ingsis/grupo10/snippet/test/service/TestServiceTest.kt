package com.ingsis.grupo10.snippet.test.service

import com.ingsis.grupo10.snippet.client.AssetClient
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
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@SpringBootTest
class TestServiceTest {
    @MockitoBean
    private lateinit var testRepository: TestRepository

    @MockitoBean
    private lateinit var snippetRepository: SnippetRepository

    private lateinit var testService: TestService

    private lateinit var testLanguage: Language
    private lateinit var testSnippet: Snippet
    private lateinit var testCase: Test
    private lateinit var testRequest: TestCreateRequest
    lateinit var assetClient: AssetClient // fixme -> tiene sentido esto?

    @BeforeEach
    fun setUp() {
        testService = TestService(testRepository, snippetRepository)

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
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        whenever(assetClient.createAsset("snippets", snippetId.toString(), "let x: number = 5;"))
            .thenReturn(codeUrl)

        whenever(assetClient.getAsset("snippets", testSnippet.id.toString()))
            .thenReturn(code)

        testCase =
            Test(
                id = UUID.randomUUID(),
                snippet = testSnippet,
                name = "Test case 1",
                inputs = "5",
                expectedOutputs = "5",
            )

        testRequest =
            TestCreateRequest(
                name = "Test case 1",
                inputs = "5",
                expectedOutputs = "5",
            )
    }

    @org.junit.jupiter.api.Test
    fun `should create test successfully`() {
        `when`(snippetRepository.findById(testSnippet.id)).thenReturn(Optional.of(testSnippet))
        `when`(testRepository.save(any(Test::class.java))).thenAnswer { it.arguments[0] }

        val result = testService.createTest(testSnippet.id, testRequest)

        assertNotNull(result)
        assertEquals(testRequest.name, result.name)
        assertEquals(testRequest.inputs, result.inputs)
        assertEquals(testRequest.expectedOutputs, result.expectedOutputs)
        assertEquals(testSnippet.id, result.snippetId)
        assertNotNull(result.id)
        verify(snippetRepository, times(1)).findById(testSnippet.id)
        verify(testRepository, times(1)).save(any(Test::class.java))
    }

    @org.junit.jupiter.api.Test
    fun `should throw exception when creating test for non-existent snippet`() {
        val nonExistentSnippetId = UUID.randomUUID()
        `when`(snippetRepository.findById(nonExistentSnippetId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                testService.createTest(nonExistentSnippetId, testRequest)
            }

        assertEquals("Snippet not found", exception.message)
        verify(snippetRepository, times(1)).findById(nonExistentSnippetId)
        verify(testRepository, never()).save(any(Test::class.java))
    }

    @org.junit.jupiter.api.Test
    fun `should get all tests by snippet id successfully`() {
        val tests = listOf(testCase)
        `when`(snippetRepository.existsById(testSnippet.id)).thenReturn(true)
        `when`(testRepository.findBySnippetId(testSnippet.id)).thenReturn(tests)

        val result = testService.getTestsBySnippet(testSnippet.id)

        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(testCase.id, result[0].id)
        assertEquals(testCase.name, result[0].name)
        assertEquals(testCase.inputs, result[0].inputs)
        assertEquals(testCase.expectedOutputs, result[0].expectedOutputs)
        assertEquals(testSnippet.id, result[0].snippetId)
        verify(snippetRepository, times(1)).existsById(testSnippet.id)
        verify(testRepository, times(1)).findBySnippetId(testSnippet.id)
    }

    @org.junit.jupiter.api.Test
    fun `should return empty list when snippet has no tests`() {
        `when`(snippetRepository.existsById(testSnippet.id)).thenReturn(true)
        `when`(testRepository.findBySnippetId(testSnippet.id)).thenReturn(emptyList())

        val result = testService.getTestsBySnippet(testSnippet.id)

        assertNotNull(result)
        assertEquals(0, result.size)
        verify(snippetRepository, times(1)).existsById(testSnippet.id)
        verify(testRepository, times(1)).findBySnippetId(testSnippet.id)
    }

    @org.junit.jupiter.api.Test
    fun `should throw exception when getting tests for non-existent snippet`() {
        val nonExistentSnippetId = UUID.randomUUID()
        `when`(snippetRepository.existsById(nonExistentSnippetId)).thenReturn(false)

        val exception =
            assertThrows<IllegalArgumentException> {
                testService.getTestsBySnippet(nonExistentSnippetId)
            }

        assertEquals("Snippet not found", exception.message)
        verify(snippetRepository, times(1)).existsById(nonExistentSnippetId)
    }

    @org.junit.jupiter.api.Test
    fun `should get test by id successfully`() {
        `when`(testRepository.findById(testCase.id)).thenReturn(Optional.of(testCase))

        val result = testService.getTestById(testCase.id)

        assertNotNull(result)
        assertEquals(testCase.id, result.id)
        assertEquals(testCase.name, result.name)
        assertEquals(testCase.inputs, result.inputs)
        assertEquals(testCase.expectedOutputs, result.expectedOutputs)
        assertEquals(testSnippet.id, result.snippetId)
        verify(testRepository, times(1)).findById(testCase.id)
    }

    @org.junit.jupiter.api.Test
    fun `should throw exception when test not found by id`() {
        val nonExistentId = UUID.randomUUID()
        `when`(testRepository.findById(nonExistentId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                testService.getTestById(nonExistentId)
            }

        assertEquals("Test not found", exception.message)
        verify(testRepository, times(1)).findById(nonExistentId)
    }

    @org.junit.jupiter.api.Test
    fun `should update test successfully`() {
        val updateRequest =
            TestCreateRequest(
                name = "Updated test case",
                inputs = "10",
                expectedOutputs = "10",
            )

        `when`(testRepository.findById(testCase.id)).thenReturn(Optional.of(testCase))
        `when`(testRepository.save(any(Test::class.java))).thenAnswer { it.arguments[0] }

        val result = testService.updateTest(testCase.id, updateRequest)

        assertNotNull(result)
        assertEquals(testCase.id, result.id)
        assertEquals(updateRequest.name, result.name)
        assertEquals(updateRequest.inputs, result.inputs)
        assertEquals(updateRequest.expectedOutputs, result.expectedOutputs)
        assertEquals(testSnippet.id, result.snippetId)
        verify(testRepository, times(1)).findById(testCase.id)
        verify(testRepository, times(1)).save(any(Test::class.java))
    }

    @org.junit.jupiter.api.Test
    fun `should throw exception when updating non-existent test`() {
        val nonExistentId = UUID.randomUUID()
        `when`(testRepository.findById(nonExistentId)).thenReturn(Optional.empty())

        val exception =
            assertThrows<IllegalArgumentException> {
                testService.updateTest(nonExistentId, testRequest)
            }

        assertEquals("Test not found", exception.message)
        verify(testRepository, times(1)).findById(nonExistentId)
        verify(testRepository, never()).save(any(Test::class.java))
    }

    @org.junit.jupiter.api.Test
    fun `should delete test successfully`() {
        `when`(testRepository.existsById(testCase.id)).thenReturn(true)

        testService.deleteTest(testCase.id)

        verify(testRepository, times(1)).existsById(testCase.id)
        verify(testRepository, times(1)).deleteById(testCase.id)
    }

    @org.junit.jupiter.api.Test
    fun `should throw exception when deleting non-existent test`() {
        val nonExistentId = UUID.randomUUID()
        `when`(testRepository.existsById(nonExistentId)).thenReturn(false)

        val exception =
            assertThrows<IllegalArgumentException> {
                testService.deleteTest(nonExistentId)
            }

        assertEquals("Test not found", exception.message)
        verify(testRepository, times(1)).existsById(nonExistentId)
        verify(testRepository, never()).deleteById(any())
    }

    @org.junit.jupiter.api.Test
    fun `should handle multiple tests for same snippet`() {
        val testCase2 =
            Test(
                id = UUID.randomUUID(),
                snippet = testSnippet,
                name = "Test case 2",
                inputs = "10",
                expectedOutputs = "10",
            )

        val tests = listOf(testCase, testCase2)
        `when`(snippetRepository.existsById(testSnippet.id)).thenReturn(true)
        `when`(testRepository.findBySnippetId(testSnippet.id)).thenReturn(tests)

        val result = testService.getTestsBySnippet(testSnippet.id)

        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals(testCase.id, result[0].id)
        assertEquals(testCase2.id, result[1].id)
        verify(snippetRepository, times(1)).existsById(testSnippet.id)
        verify(testRepository, times(1)).findBySnippetId(testSnippet.id)
    }
}
