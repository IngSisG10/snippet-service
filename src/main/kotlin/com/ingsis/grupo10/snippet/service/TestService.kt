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
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val printScriptClient: PrintScriptClient,
    private val assetClient: AssetClient,
    private val lintConfigService: LintConfigService,
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
        userId: String,
        request: RunTestRequest,
    ): TestResultResponse {
        val snippet =
            snippetRepository
                .findById(snippetId)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val configJson = lintConfigService.getConfigJson(userId)

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val finalCode =
            if (!request.input.isNullOrEmpty()) {
                replaceReadInputsSimple(code, request.input)
            } else {
                code
            }

        val executionResult =
            printScriptClient.executeSnippet(
                code = finalCode,
                configJson = configJson,
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

                val passed = expectedOutput.map { it.trim() } == actualOutput.map { it.trim() }

                return TestResultResponse(
                    status = if (passed) "success" else "fail",
                    output = actualOutput,
                )
            }
        }
    }

    private fun replaceReadInputsSimple(
        code: String,
        inputs: List<String>,
    ): String {
        if (inputs.isEmpty()) return code

        var inputIndex = 0
        val pattern = Regex("readInput\\(\\)")

        val result =
            code.replace(pattern) { match ->
                if (inputIndex >= inputs.size) {
                    throw IllegalArgumentException(
                        "More readInput() calls were found than inputs provided.",
                    )
                }

                val rawValue = inputs[inputIndex++]

                when {
                    rawValue.toIntOrNull() != null -> rawValue
                    rawValue.toDoubleOrNull() != null -> rawValue
                    rawValue == "true" || rawValue == "false" -> rawValue
                    else -> "\"" + rawValue.replace("\"", "\\\"") + "\""
                }
            }

        if (inputIndex != inputs.size) {
            throw IllegalArgumentException(
                "More inputs (${inputs.size}) were provided than readInput() calls in the code ($inputIndex).",
            )
        }

        return result
    }

    @Transactional
    fun runAllTestsForSnippet(
        userId: String,
        snippetId: UUID,
    ): List<TestResultResponse> {
        println("Running all tests for snippet: $snippetId")

        val snippet =
            snippetRepository
                .findById(snippetId)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val tests = testRepository.findBySnippetId(snippetId)

        if (tests.isEmpty()) {
            println("No tests found for snippet: $snippetId")
            return emptyList()
        }

        val configJson = lintConfigService.getConfigJson(userId)
        val (container, key) = parseCodeUrl(snippet.codeUrl)
        val code = assetClient.getAsset(container, key)

        return tests.mapNotNull { test ->
            try {
                val executionResult =
                    printScriptClient.executeSnippet(
                        code = code,
                        configJson = configJson,
                        version = snippet.version,
                    )

                when (executionResult) {
                    is ExecutionResult.Failed -> {
                        val errorMessages = executionResult.errors.map { it.message }
                        println("Test '${test.name}' failed during execution: $errorMessages")
                        TestResultResponse(
                            status = "error",
                            output = errorMessages,
                        )
                    }

                    is ExecutionResult.Success -> {
                        val passed = test.output.map { it.trim() } == executionResult.output.map { it.trim() }
                        println("Test '${test.name}' ${if (passed) "PASSED" else "FAILED"}")
                        TestResultResponse(
                            status = if (passed) "success" else "fail",
                            output = executionResult.output,
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error running test '${test.name}': ${e.message}")
                TestResultResponse(
                    status = "error",
                    output = listOf(e.message ?: "Unknown error"),
                )
            }
        }
    }
}
