package com.ingsis.grupo10.snippet.snippet.service
import com.ingsis.grupo10.snippet.models.Snippet
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
    // En relacion al file -> solo consideralo como txt o un json
    // Para poder mandar esa informacion luego con la metadata que tenemos
    // El code es un file de ese estilo

    fun getSnippetById(id: UUID): SnippetResponseDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

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

    fun getAllSnippets(): List<SnippetResponseDto> =
        snippetRepository.findAll().map {
            SnippetResponseDto(
                id = it.id.toString(),
                name = it.name,
                description = it.description,
                language = it.languageId.name,
                version = it.version,
                ownerId = it.ownerId,
                createdAt = it.createdAt,
            )
        }

    // todo
    fun createSnippet(request: SnippetCreateRequest): SnippetResponseDto {
        val language =
            languageRepository.findByName(request.language.name)
                ?: throw IllegalArgumentException("Language not supported")

        // validationResult -> Se validaria aqui por parte del parser?
        // todo: vincularlo con el PrintScript Service
        // request a PrintScript Service y si es valido, lo ponga o no en la db.
        // Si no es valido, no lo guarda

        // lleve a: tener este msj de error.

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
                snippetLogs = emptySet(),
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

    fun deleteSnippetById(id: UUID) {
        if (!snippetRepository.existsById(id)) {
            throw IllegalArgumentException("Snippet not found")
        }
        snippetRepository.deleteById(id)
    }

    fun updateSnippet(
        id: UUID,
        request: SnippetCreateRequest,
    ): SnippetResponseDto {
        val existingSnippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        val language =
            request.language.let {
                languageRepository.findByName(it.name)
                    ?: throw IllegalArgumentException("Language not supported")
            }

        // todo: Conviene quiza crear uno nuevo directamente, en lugar de hacer un copy
        val updatedSnippet =
            existingSnippet.copy(
                name = request.name,
                description = request.description,
                code = request.code,
                languageId = language,
                version = request.version,
                updatedAt = Instant.now().toString(),
            )

        snippetRepository.save(updatedSnippet)

        return SnippetResponseDto(
            id = updatedSnippet.id.toString(),
            name = updatedSnippet.name,
            description = updatedSnippet.description,
            language = updatedSnippet.languageId.name,
            version = updatedSnippet.version,
            ownerId = updatedSnippet.ownerId,
            createdAt = updatedSnippet.createdAt,
        )
    }
}
