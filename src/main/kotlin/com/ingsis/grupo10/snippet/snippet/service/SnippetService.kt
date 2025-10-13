package com.ingsis.grupo10.snippet.snippet.service
import com.ingsis.grupo10.snippet.models.Log
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.models.SnippetLog
import com.ingsis.grupo10.snippet.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.snippet.dto.SnippetResponseDto
import com.ingsis.grupo10.snippet.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.snippet.repository.SnippetRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val languageRepository: LanguageRepository,
) {
    fun createSnippet(request: SnippetCreateRequest): SnippetResponseDto {
        val language =
            languageRepository.findByName(request.language.name)
                ?: throw IllegalArgumentException("Language not supported")

        // validationResult -> Se validaria aqui por parte del parser?

        val now = Instant.now().toString()

        val snippet =
            Snippet(
                id = UUID.randomUUID(), // Implementa una función para generar IDs únicos
                name = request.name,
                code = request.code,
                languageId = language,
                description = request.description,
                version = request.version, // Versión inicial
                ownerId = "owner-id-placeholder", // Reemplaza con el ID real del propietario
                createdAt = now,
                updatedAt = now,
                snippetLogs = emptySet(), // todo: ver tema de addLogToSnippet
            )

        snippetRepository.save(snippet)

        return SnippetResponseDto(
            id = snippet.id.toString(),
            name = snippet.name,
            description = snippet.description,
            language = snippet.languageId.name,
            version = snippet.version,
            ownerId = snippet.ownerId,
            createdAt = snippet.createdAt,
        )
    }

    // todo: quiza por este lado. Me suena raro hacerlo asi como metodo aparte.
    fun addLogToSnippet(
        snippetId: UUID,
        log: Log,
    ) {
        val snippet =
            snippetRepository
                .findById(snippetId)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val snippetLog =
            SnippetLog(
                id = UUID.randomUUID(),
                snippet = snippet,
                log = log,
                createdAt = Instant.now().toString(),
            )

        snippetRepository.save(snippet.copy(snippetLogs = snippet.snippetLogs + snippetLog))
    }
}
