package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.exception.SnippetValidationException
import com.ingsis.grupo10.snippet.extension.toDetailDto
import com.ingsis.grupo10.snippet.extension.toSnippet
import com.ingsis.grupo10.snippet.extension.toSummaryDto
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val languageRepository: LanguageRepository,
    private val printScriptClient: PrintScriptClient,
) {
    // En relacion al file -> solo consideralo como txt o un json
    // Para poder mandar esa informacion luego con la metadata que tenemos
    // El code es un file de ese estilo

    fun getSnippetById(id: UUID): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        return snippet.toDetailDto()
    }

    fun getAllSnippets(): List<SnippetSummaryDto> =
        snippetRepository.findAll().map {
            it.toSummaryDto()
        }

    fun createSnippet(
        request: SnippetCreateRequest,
        userId: String = "00000000-0000-0000-0000-000000000000",
    ): SnippetDetailDto {
        val validationResult =
            printScriptClient.validateSnippet(
                code = request.code,
                version = request.version,
            )

        when (validationResult) {
            is ValidationResult.Failed -> {
                throw SnippetValidationException(
                    "Syntax error while validating snippet",
                    validationResult.errors,
                )
            }

            ValidationResult.Success -> {
                val language =
                    languageRepository.findByName(request.languageName)
                        ?: throw IllegalArgumentException("Language not supported")

                val snippet = request.toSnippet(language, UUID.fromString(userId))

                val saved = snippetRepository.save(snippet)
                return saved.toDetailDto()
            }
        }
    }

    fun deleteSnippetById(id: UUID) {
        if (!snippetRepository.existsById(id)) {
            throw IllegalArgumentException("Snippet not found")
        }
        snippetRepository.deleteById(id)
    }

    fun updateSnippet(
        id: UUID,
        request: SnippetCreateRequest,
    ): SnippetDetailDto {
        val existingSnippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val validationResult =
            printScriptClient.validateSnippet(
                code = request.code,
                version = request.version,
            )

        when (validationResult) {
            is ValidationResult.Failed -> {
                throw SnippetValidationException(
                    "Syntax error while validating snippet",
                    validationResult.errors,
                )
            }

            ValidationResult.Success -> {
                val language =
                    languageRepository.findByName(request.languageName)
                        ?: throw IllegalArgumentException("Language not supported")

                val updatedSnippet =
                    existingSnippet.copy(
                        name = request.name,
                        description = request.description,
                        code = request.code,
                        language = language,
                        version = request.version,
                        updatedAt = LocalDateTime.now(),
                    )

                snippetRepository.save(updatedSnippet)
                return updatedSnippet.toDetailDto()
            }
        }
    }
}
