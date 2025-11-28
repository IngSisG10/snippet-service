package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.AssetClient
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.dto.TestResponseDto
import com.ingsis.grupo10.snippet.dto.tests.RunTestRequest
import com.ingsis.grupo10.snippet.dto.tests.TestResultResponse
import com.ingsis.grupo10.snippet.dto.validation.ExecutionResult
import com.ingsis.grupo10.snippet.exception.SnippetExecutionException
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.repository.TestRepository
import com.ingsis.grupo10.snippet.util.AssetUtils.parseCodeUrl
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val printScriptClient: PrintScriptClient,
    private val assetClient: AssetClient,
) {
    // todo: para poder ejecutar el test, debemos pegarle al execute del printscript
    // todo: para ello, necesitamos utilizar el PrintScriptClient y usar el endpoint de ejecucion

    // todo: Es probable que tengamos que matchear los outputs esperados (ui)
    //  con los outputs reales (la ejecucion desde printscript)

    fun getTestsBySnippet(snippetId: UUID): List<TestResponseDto> {
        if (!snippetRepository.existsById(snippetId)) {
            throw IllegalArgumentException("Snippet not found")
        }

        return testRepository.findBySnippetId(snippetId).map {
            TestResponseDto(
                id = it.id,
                name = it.name,
                input = it.input,
                output = it.output,
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
            name = test.name,
            input = test.input,
            output = test.output,
        )
    }

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
                input = request.input,
                output = request.output,
            )

        testRepository.save(test)

        return TestResponseDto(
            id = test.id,
            name = test.name,
            input = test.input,
            output = test.output,
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
                input = request.input,
                output = request.output,
            )

        testRepository.save(updatedTest)

        return TestResponseDto(
            id = updatedTest.id,
            name = updatedTest.name,
            input = updatedTest.input,
            output = updatedTest.output,
        )
    }

    fun deleteTest(testId: UUID) {
        if (!testRepository.existsById(testId)) {
            throw IllegalArgumentException("Test not found")
        }
        testRepository.deleteById(testId)
    }

    // "Call" Printscript to execute snippet and check if test passed (output coincides)
    fun runTest(
        snippetId: UUID,
        request: RunTestRequest,
    ): TestResultResponse {
        val snippet =
            snippetRepository
                .findById(snippetId)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val executionResult =
            printScriptClient.executeSnippet(
                code = code,
                input = request.input ?: emptyList(),
                version = snippet.version,
            )

        when (executionResult) {
            is ExecutionResult.Failed -> {
                throw SnippetExecutionException(
                    "Snippet execution failed",
                    executionResult.errors,
                )
            }

            is ExecutionResult.Success -> {
                val expectedOutput = request.output ?: emptyList()
                val actualOutput = executionResult.output

                val passed = expectedOutput == actualOutput

                return TestResultResponse(
                    status = if (passed) "success" else "fail",
                    output = actualOutput,
                )
            }
        }
    }
}
