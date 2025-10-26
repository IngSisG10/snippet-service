package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.AssetClient
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.exception.SnippetValidationException
import com.ingsis.grupo10.snippet.extension.toDetailDto
import com.ingsis.grupo10.snippet.extension.toSnippet
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.util.AssetUtils.parseCodeUrl
import com.ingsis.grupo10.snippet.util.UserContext
import com.ingsis.grupo10.snippet.util.toUuidOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val languageRepository: LanguageRepository,
    private val printScriptClient: PrintScriptClient,
    private val assetClient: AssetClient,
    private val logService: LogService,
    private val lintConfigService: LintConfigService,
    private val formatConfigService: FormatConfigService,
) {
    fun getSnippetById(id: UUID): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        return snippet.toDetailDto()
    }

    fun getAllSnippets(
        name: String? = null,
        language: String? = null,
        compliance: String? = null,
        sortBy: String? = null,
        sortDirection: String? = "ASC",
    ): List<SnippetSummaryDto> {
        val allSnippets = snippetRepository.findAll()

        // Map to DTO with compliance status
        var snippets =
            allSnippets.map { snippet ->
                val lintStatus = logService.getLatestLintStatus(snippet.id)
                SnippetSummaryDto(
                    id = snippet.id,
                    name = snippet.name,
                    language = snippet.language.name,
                    version = snippet.version,
                    createdAt = snippet.createdAt,
                    compliance = lintStatus.status,
                )
            }

        // Apply filters
        name?.let { filterName ->
            snippets = snippets.filter { it.name.contains(filterName, ignoreCase = true) }
        }

        language?.let { filterLanguage ->
            snippets = snippets.filter { it.language.equals(filterLanguage, ignoreCase = true) }
        }

        compliance?.let { filterCompliance ->
            snippets = snippets.filter { it.compliance.equals(filterCompliance, ignoreCase = true) }
        }

        // Apply sorting
        snippets =
            when (sortBy?.lowercase()) {
                "name" ->
                    if (sortDirection?.uppercase() == "DESC") {
                        snippets.sortedByDescending { it.name }
                    } else {
                        snippets.sortedBy { it.name }
                    }
                "language" ->
                    if (sortDirection?.uppercase() == "DESC") {
                        snippets.sortedByDescending { it.language }
                    } else {
                        snippets.sortedBy { it.language }
                    }
                "compliance" ->
                    if (sortDirection?.uppercase() == "DESC") {
                        snippets.sortedByDescending { it.compliance ?: "" }
                    } else {
                        snippets.sortedBy { it.compliance ?: "" }
                    }
                "createdat" ->
                    if (sortDirection?.uppercase() == "DESC") {
                        snippets.sortedByDescending { it.createdAt }
                    } else {
                        snippets.sortedBy { it.createdAt }
                    }
                else -> snippets // No sorting or default order
            }

        return snippets
    }

    fun createSnippet(
        request: SnippetCreateRequest,
        userId: String? = null,
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

                // Use provided userId or get current user from context
                val ownerUuid =
                    if (userId != null) {
                        UserContext.toUuidOrThrow(userId, "Invalid userId format")
                    } else {
                        UserContext.getCurrentUserId()
                    }

                // Create asset into the bucket
                // fixme -> hardcodeado el container y la key
                val codeUrl =
                    assetClient.createAsset(container = "snippets", key = UUID.randomUUID().toString(), request.code)
                        ?: throw RuntimeException("Failed to upload snippet asset")

                val snippet = request.toSnippet(language, ownerUuid, codeUrl)

                val saved = snippetRepository.save(snippet)

                logService.logValidation(saved, emptyList())

                return saved.toDetailDto()
            }
        }
    }

    fun deleteSnippetById(
        id: UUID,
        userId: String? = null,
    ) {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // Validate user ownership if userId is provided
        if (userId != null) {
            val userUuid = UserContext.toUuidOrThrow(userId, "Invalid userId format")
            if (snippet.ownerId != userUuid) {
                throw IllegalArgumentException("User does not have permission to delete this snippet")
            }
        }

        snippetRepository.deleteById(id)
    }

    fun updateSnippet(
        id: UUID,
        request: SnippetCreateRequest,
        userId: String? = null,
    ): SnippetDetailDto {
        val existingSnippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // Validate user ownership if userId is provided
        if (userId != null) {
            val userUuid = UserContext.toUuidOrThrow(userId, "Invalid userId format")
            if (existingSnippet.ownerId != userUuid) {
                throw IllegalArgumentException("User does not have permission to update this snippet")
            }
        }

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

                val (container, key) = parseCodeUrl(existingSnippet.codeUrl)

                // Update asset in the bucket
                val updatedUrl =
                    assetClient.createAsset(
                        container = container,
                        key = key,
                        content = request.code, // newCode content
                    ) ?: throw RuntimeException("Failed to update snippet asset")

                val updatedSnippet =
                    existingSnippet.copy(
                        name = request.name,
                        description = request.description,
                        codeUrl = updatedUrl,
                        language = language,
                        version = request.version,
                        updatedAt = LocalDateTime.now(),
                        ownerId = existingSnippet.ownerId, // Keep existing ownerId for update
                    )

                val saved = snippetRepository.save(updatedSnippet)

                logService.logValidation(saved, emptyList())

                return saved.toDetailDto()
            }
        }
    }

    fun lintSnippet(
        id: UUID,
        userId: UUID? = null,
    ): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // Use provided userId or get current user from context
        val userUuid = userId ?: UserContext.getCurrentUserId()

        val lintConfig = lintConfigService.getConfigJson(userUuid)

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val lintResult =
            printScriptClient.lintSnippet(
                code = code,
                version = snippet.version,
                lintConfig = lintConfig,
            )

        logService.logLinting(snippet, lintResult)

        return snippet.toDetailDto()
    }

    fun formatSnippet(
        id: UUID,
        userId: UUID? = null,
    ): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // Use provided userId or get current user from context
        val userUuid = userId ?: UserContext.getCurrentUserId()

        val formatConfig = formatConfigService.getConfigJson(userUuid)

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val formatResult =
            printScriptClient.formatSnippet(
                code = code,
                version = snippet.version,
                formatConfig = formatConfig,
            )

        logService.logFormatting(snippet, formatResult.formattedCode, formatConfig)

        return snippet.toDetailDto()
    }

    /**
     * Gets all snippets owned by a specific user.
     *
     * @param userId The user ID to filter snippets by
     * @return List of snippets owned by the user
     */
    fun getSnippetsByUser(userId: String): List<SnippetSummaryDto> {
        val userUuid = UserContext.toUuidOrThrow(userId, "Invalid userId format")
        val userSnippets = snippetRepository.findByOwnerId(userUuid)

        return userSnippets.map { snippet ->
            val lintStatus = logService.getLatestLintStatus(snippet.id)
            SnippetSummaryDto(
                id = snippet.id,
                name = snippet.name,
                language = snippet.language.name,
                version = snippet.version,
                createdAt = snippet.createdAt,
                compliance = lintStatus.status,
            )
        }
    }
}
