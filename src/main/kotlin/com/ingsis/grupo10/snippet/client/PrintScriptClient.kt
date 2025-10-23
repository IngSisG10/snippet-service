package com.ingsis.grupo10.snippet.client

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
        version: String,
    ): ValidationResult {
        val tempFilePath = createTempFile(prefix = "snippet", suffix = ".ps")
        tempFilePath.writeText(code)

        try {
            val response =
                webClient
                    .post()
                    .uri("/api/printscript/verify")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(
                        BodyInserters
                            .fromMultipartData("snippet", FileSystemResource(tempFilePath.toFile()))
                            .with("version", version)
                            .with("config", createDefaultLintConfig()), // JSON con reglas
                    ).retrieve()
                    .bodyToMono(LintResultDTO::class.java)
                    .block() ?: throw RuntimeException("No response from PrintScript service")

            return if (response.errors.isEmpty()) {
                ValidationResult.Success
            } else {
                ValidationResult.Failed(
                    response.errors.map {
                        ValidationError(
                            message = it.message,
                            line = extractLineNumber(it.message), // Parsear del mensaje
                            column = extractColumnNumber(it.message),
                            rule = it.type,
                        )
                    },
                )
            }
        } finally {
            tempFilePath.deleteExisting()
        }
    }

    private fun createDefaultLintConfig(): String =
        """
        {
            "identifier_format": "camel case"
        }
        """.trimIndent()

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
}
