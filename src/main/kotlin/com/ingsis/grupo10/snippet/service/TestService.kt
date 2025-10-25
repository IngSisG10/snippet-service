package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.dto.TestResponseDto
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.repository.TestRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
) {
    fun createTest(
        snippetId: UUID,
        request: TestCreateRequest,
    ): TestResponseDto {
        val snippet =
            snippetRepository
                .findById(snippetId)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val test =
            Test(
                id = UUID.randomUUID(),
                snippet = snippet,
                name = request.name,
                inputs = request.inputs,
                expectedOutputs = request.expectedOutputs,
            )

        testRepository.save(test)

        return TestResponseDto(
            id = test.id,
            snippetId = test.snippet.id,
            name = test.name,
            inputs = test.inputs,
            expectedOutputs = test.expectedOutputs,
        )
    }

    fun getTestsBySnippet(snippetId: UUID): List<TestResponseDto> {
        if (!snippetRepository.existsById(snippetId)) {
            throw IllegalArgumentException("Snippet not found")
        }

        return testRepository.findBySnippetId(snippetId).map {
            TestResponseDto(
                id = it.id,
                snippetId = it.snippet.id,
                name = it.name,
                inputs = it.inputs,
                expectedOutputs = it.expectedOutputs,
            )
        }
    }

    fun getTestById(testId: UUID): TestResponseDto {
        val test =
            testRepository
                .findById(testId)
                .orElseThrow { IllegalArgumentException("Test not found") }

        return TestResponseDto(
            id = test.id,
            snippetId = test.snippet.id,
            name = test.name,
            inputs = test.inputs,
            expectedOutputs = test.expectedOutputs,
        )
    }

    fun updateTest(
        testId: UUID,
        request: TestCreateRequest,
    ): TestResponseDto {
        val existingTest =
            testRepository
                .findById(testId)
                .orElseThrow { IllegalArgumentException("Test not found") }

        val updatedTest =
            existingTest.copy(
                name = request.name,
                inputs = request.inputs,
                expectedOutputs = request.expectedOutputs,
            )

        testRepository.save(updatedTest)

        return TestResponseDto(
            id = updatedTest.id,
            snippetId = updatedTest.snippet.id,
            name = updatedTest.name,
            inputs = updatedTest.inputs,
            expectedOutputs = updatedTest.expectedOutputs,
        )
    }

    fun deleteTest(testId: UUID) {
        if (!testRepository.existsById(testId)) {
            throw IllegalArgumentException("Test not found")
        }
        testRepository.deleteById(testId)
    }
}
