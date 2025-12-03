package com.ingsis.grupo10.snippet.client

import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import com.ingsis.grupo10.snippet.dto.validation.ExecutionError
import com.ingsis.grupo10.snippet.dto.validation.ExecutionResult
import com.ingsis.grupo10.snippet.dto.validation.FormatResultDTO
import com.ingsis.grupo10.snippet.dto.validation.LintResultDTO
import com.ingsis.grupo10.snippet.dto.validation.ValidationError
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import org.springframework.core.io.FileSystemResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

@Service
class PrintScriptClient(
    private val webClient: WebClient,
) {
    fun validateSnippet(
        code: String,
        configJson: String,
        version: String,
    ): ValidationResult {
        val tempFilePath = createTempFile(prefix = "snippet", suffix = ".ps")
        tempFilePath.writeText(code)

        val tempConfigPath = createTempFile(prefix = "lint-config", suffix = ".json")
        tempConfigPath.writeText(configJson)

        try {
            val response = webClient
                .post()
                .uri("/api/printscript/verify?version=$version")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(
                    BodyInserters.fromMultipartData(
                        org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                            add("snippet", FileSystemResource(tempFilePath.toFile()))
                            add("config", FileSystemResource(tempConfigPath.toFile()))
                        }
                    )
                )
                .retrieve()
                .bodyToMono(LintResultDTO::class.java)
                .block() ?: throw RuntimeException("No response from PrintScript service")

            return if (response.errors.isEmpty()) {
                ValidationResult.Success
            } else {
                ValidationResult.Failed(
                    response.errors.map {
                        ValidationError(
                            message = it.message,
                            line = extractLineNumber(it.message),
                            column = extractColumnNumber(it.message),
                            rule = it.type,
                        )
                    },
                )
            }
        } finally {
            tempFilePath.deleteExisting()
            tempConfigPath.deleteExisting()
        }
    }


    fun executeSnippet(
        code: String,
        configJson: String,
        version: String,
    ): ExecutionResult {
        val tempFilePath = createTempFile(prefix = "snippet", suffix = ".ps")
        tempFilePath.writeText(code)

        val tempConfigPath = createTempFile(prefix = "lint-config", suffix = ".json")
        tempConfigPath.writeText(configJson)

        try {
            val rawResponse = webClient
                .post()
                .uri("/api/printscript/execute?version=$version")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(
                    BodyInserters.fromMultipartData(
                        org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                            add("snippet", FileSystemResource(tempFilePath.toFile()))
                            add("config", FileSystemResource(tempConfigPath.toFile()))
                        }
                    )
                )
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("No response from PrintScript service")

            if (rawResponse.startsWith("Error:")) {
                return ExecutionResult.Failed(
                    listOf(ExecutionError(message = rawResponse.removePrefix("Error:").trim())),
                )
            }

            val outputLines = rawResponse
                .lines()
                .map { it.trimEnd() }
                .filter { it.isNotEmpty() }

            return ExecutionResult.Success(output = outputLines)
        } finally {
            tempFilePath.deleteExisting()
            tempConfigPath.deleteExisting()
        }
    }


    private fun extractLineNumber(message: String): Int? {
        // Parse line number from error message
        // Expected format: "Error at line X, column Y: ..."
        val linePattern = Regex("line (\\d+)")
        return linePattern
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    private fun extractColumnNumber(message: String): Int? {
        // Parse column number from error message
        // Expected format: "Error at line X, column Y: ..."
        val columnPattern = Regex("column (\\d+)")
        return columnPattern
            .find(message)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    fun lintSnippet(
        code: String,
        version: String,
        lintConfig: String,
    ): LintResultDTO {
        val tempFilePath = createTempFile(prefix = "snippet", suffix = ".ps")
        tempFilePath.writeText(code)

        val tempConfigPath = createTempFile(prefix = "lint-config", suffix = ".json")
        tempConfigPath.writeText(lintConfig)

        try {
            val response = webClient
                .post()
                .uri("/api/printscript/verify?version=$version")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(
                    BodyInserters.fromMultipartData(
                        org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                            add("snippet", FileSystemResource(tempFilePath.toFile()))
                            add("config", FileSystemResource(tempConfigPath.toFile()))
                        }
                    )
                )
                .retrieve()
                .bodyToMono(LintResultDTO::class.java)
                .block() ?: throw RuntimeException("No response from PrintScript service")

            return response
        } finally {
            tempFilePath.deleteExisting()
            tempConfigPath.deleteExisting()
        }
    }

    fun formatSnippet(
        code: String,
        version: String,
        formatConfig: String,
    ): FormatResultDTO {
        val tempFilePath = createTempFile(prefix = "snippet", suffix = ".ps")
        tempFilePath.writeText(code)

        val tempConfigPath = createTempFile(prefix = "format-config", suffix = ".json")
        tempConfigPath.writeText(formatConfig)

        try {
            val formattedCode = webClient
                .post()
                .uri("/api/printscript/format?version=$version")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(
                    BodyInserters.fromMultipartData(
                        org.springframework.util.LinkedMultiValueMap<String, Any>().apply {
                            add("snippet", FileSystemResource(tempFilePath.toFile()))
                            add("config", FileSystemResource(tempConfigPath.toFile()))
                        }
                    )
                )
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: throw RuntimeException("No response from PrintScript service")

            return FormatResultDTO(formattedCode)
        } finally {
            tempFilePath.deleteExisting()
            tempConfigPath.deleteExisting()
        }
    }

    fun getFormatConfigRules(version: String): List<RuleDto> =
        try {
            webClient
                .get()
                .uri("/api/printscript/format/$version")
                .retrieve()
                .bodyToFlux(RuleDto::class.java)
                .collectList()
                .block()
                ?: emptyList()
        } catch (ex: Exception) {
            throw RuntimeException("Error fetching formatting rules from PrintScript service: ${ex.message}", ex)
        }

    fun getLintConfigRules(version: String): List<RuleDto> =
        try {
            webClient
                .get()
                .uri("/api/printscript/lint/$version")
                .retrieve()
                .bodyToFlux(RuleDto::class.java)
                .collectList()
                .block()
                ?: emptyList()
        } catch (ex: Exception) {
            throw RuntimeException("Error fetching linting rules from PrintScript service: ${ex.message}", ex)
        }
}
