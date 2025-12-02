package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.AssetClient
import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.Created
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.SnippetUICreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetUIDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetUIFormatDto
import com.ingsis.grupo10.snippet.dto.SnippetUIUpdateRequest
import com.ingsis.grupo10.snippet.dto.filetype.FileTypeResponse
import com.ingsis.grupo10.snippet.dto.paginatedsnippets.PaginatedSnippetsResponse
import com.ingsis.grupo10.snippet.dto.paginatedsnippets.SnippetResponse
import com.ingsis.grupo10.snippet.dto.tests.ExecutionDto
import com.ingsis.grupo10.snippet.dto.validation.ExecutionResult
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.exception.SnippetExecutionException
import com.ingsis.grupo10.snippet.exception.SnippetValidationException
import com.ingsis.grupo10.snippet.extension.created
import com.ingsis.grupo10.snippet.extension.toDetailDto
import com.ingsis.grupo10.snippet.extension.toUIDetailDto
import com.ingsis.grupo10.snippet.extension.toUIFormatDto
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.util.AssetUtils.parseCodeUrl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val languageRepository: LanguageRepository,
    private val printScriptClient: PrintScriptClient,
    private val assetClient: AssetClient,
    private val authClient: AuthClient,
    private val logService: LogService,
    private val lintConfigService: LintConfigService,
    private val formatConfigService: FormatConfigService,
    private val testExecutionProducer: com.ingsis.grupo10.snippet.producer.TestExecutionProducer,
) {
    fun getSnippetById(id: UUID): SnippetDetailDto {
        val snippet = snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        return snippet.toDetailDto()
    }

    fun getUISnippetById(
        id: UUID,
        username: String,
    ): SnippetUIDetailDto {
        val snippet = snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        val content =
            run {
                val (container, key) = parseCodeUrl(snippet.codeUrl)
                assetClient.getAsset(container, key)
            }

        return snippet.toUIDetailDto(content, username)
    }

    fun createSnippet(
        request: SnippetUICreateRequest,
        userId: String,
        snippetId: UUID,
    ): Created {
        val configJson = lintConfigService.getConfigJson(userId)

        val validationResult =
            printScriptClient.validateSnippet(
                code = request.content,
                configJson = configJson,
                version = "1.1",
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
                    languageRepository.findByName(request.language)
                        ?: throw IllegalArgumentException("Language not supported")

                val assetResult =
                    assetClient.createAsset(
                        container = "snippets",
                        key = snippetId.toString(), // asociamos esta key con el ID del snippet
                        content = request.content,
                    )

                // Store as "container/key" format
                val codeUrl = "snippets/$snippetId"

                // y creamos el snippet con ese ID
                val snippet =
                    Snippet(
                        id = snippetId,
                        name = request.name,
                        language = language,
                        codeUrl = codeUrl,
                        description = "",
                        version = "1.1",
                        createdAt = LocalDateTime.now(),
                        updatedAt = LocalDateTime.now(),
                    )

                val saved = snippetRepository.save(snippet)

                logService.logValidation(saved, emptyList())

                return saved.created()
            }
        }
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

    fun deleteSnippetById(id: UUID) {
        val snippet = snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        snippetRepository.deleteById(id)
    }

    fun updateSnippet(
        id: UUID,
        userId: String,
        request: SnippetUIUpdateRequest,
    ): SnippetDetailDto {
        val configJson = lintConfigService.getConfigJson(userId)

        val existingSnippet =
            snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        val validationResult =
            printScriptClient.validateSnippet(
                code = request.content,
                configJson = configJson,
                version = "1.1",
            )

        when (validationResult) {
            is ValidationResult.Failed -> {
                throw SnippetValidationException(
                    "Syntax error while validating snippet",
                    validationResult.errors,
                )
            }

            ValidationResult.Success -> {
                val (container, key) = parseCodeUrl(existingSnippet.codeUrl)

                assetClient.createAsset(
                    container = container,
                    key = key,
                    content = request.content,
                )

                val updatedSnippet =
                    Snippet(
                        id = existingSnippet.id,
                        name = existingSnippet.name,
                        description = existingSnippet.description,
                        codeUrl = existingSnippet.codeUrl,
                        language = existingSnippet.language,
                        version = existingSnippet.version,
                        createdAt = existingSnippet.createdAt,
                        updatedAt = LocalDateTime.now(),
                    )

                val saved = snippetRepository.save(updatedSnippet)

                logService.logValidation(saved, emptyList())

                // Trigger test execution for all tests of this snippet
                testExecutionProducer.publishTestExecutionRequest(saved.id.toString())

                return saved.toDetailDto()
            }
        }
    }

    @Transactional
    fun lintSnippet(
        userId: String,
        id: UUID,
    ): SnippetDetailDto {
        val snippet = snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        val lintConfig = lintConfigService.getConfigJson(userId)

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

    @Transactional
    fun formatSnippet(
        userId: String,
        id: UUID,
    ): SnippetUIFormatDto {
        val snippet = snippetRepository.findById(id).orElseThrow { IllegalArgumentException("Snippet not found") }

        // TODO: Get user-specific format config - for now use default
        // val formatConfig = """{"enforce-spacing-around-equals": true}"""
        val formatConfig = formatConfigService.getConfigJson(userId)

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val formatResult =
            printScriptClient.formatSnippet(
                code = code,
                version = snippet.version,
                formatConfig = formatConfig,
            )

        // Update the asset with formatted code
        assetClient.createAsset(container, key, formatResult.formattedCode)

        logService.logFormatting(snippet, formatResult.formattedCode, formatConfig)

        return snippet.toUIFormatDto(formatResult.formattedCode)
    }

//    /**
//     * Gets all snippets owned by a specific user.
//     *
//     * @param userId The user ID to filter snippets by
//     * @return List of snippets owned by the user
//     */
// This now gets snippets by querying auth service for user's accessible snippets

//    fun getSnippetsByUser(userId: String): List<SnippetSummaryDto> {
//        val userSnippets = snippetRepository.findByOwnerId(userId)
//
//        return userSnippets.map { snippet ->
//            val lintStatus = logService.getLatestLintStatus(snippet.id)
//            SnippetSummaryDto(
//                id = snippet.id,
//                name = snippet.name,
//                language = snippet.language.name,
//                version = snippet.version,
//                createdAt = snippet.createdAt,
//                compliance = lintStatus.status,
//            )
//        }
//    }

    // List Descriptors
    // fixme: manejar casos en los cuales no tenemos "PaginatedSnippets" que mostrar.
    fun listSnippetDescriptors(
        userId: String,
        page: Int,
        pageSize: Int,
        name: String?,
    ): PaginatedSnippetsResponse {
        val pageable = PageRequest.of(page, pageSize)

        // TODO: Filter by user's snippets via auth-service if needed
        val result =
            snippetRepository.findByNameContainingIgnoreCase(
                name ?: "",
                pageable,
            )

        val snippetDtos =
            result.content.map {
                SnippetResponse(
                    id = it.id,
                    name = it.name,
                    description = it.description,
                    language = it.language.name,
                    version = it.version,
                    createdAt = it.createdAt.toString(),
                    author = userId,
                    compliance = logService.getLatestLintStatus(it.id).status,
                )
            }

        return PaginatedSnippetsResponse(
            page = page,
            pageSize = pageSize,
            count = result.totalElements,
            snippets = snippetDtos,
        )
    }

    fun getSupportedFileTypes(): List<FileTypeResponse> =
        languageRepository.findAll().map {
            FileTypeResponse(
                language = it.name,
                extension = "ps",
            )
        }

    fun shareSnippet(
        username: String,
        snippetId: UUID,
        targetUserEmail: String,
    ): SnippetUIDetailDto {
        val snippet =
            snippetRepository.findById(snippetId).orElseThrow { IllegalArgumentException("Snippet not found") }

        val isShareSuccessful =
            authClient.shareSnippet(
                snippetId = snippetId,
                targetUserEmail = targetUserEmail,
            )

        if (!isShareSuccessful) {
            throw IllegalStateException("Failed to share snippet")
        }

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val content = assetClient.getAsset(container, key)

        return snippet.toUIDetailDto(content, username)
    }

    fun runSnippet(
        userId: String,
        snippetId: UUID,
    ): ExecutionDto {
        val snippet = snippetRepository.findById(snippetId).orElseThrow { IllegalArgumentException("Snippet not found") }

        val configJson = lintConfigService.getConfigJson(userId)

        val (container, key) = parseCodeUrl(snippet.codeUrl)

        val code = assetClient.getAsset(container, key)

        val executionResult =
            printScriptClient.executeSnippet(
                code = code,
                configJson = configJson,
                version = snippet.version,
            )

        when (executionResult) {
            is ExecutionResult.Success -> {
                return ExecutionDto(
                    output = executionResult.output,
                    errors = emptyList(),
                )
            }
            is ExecutionResult.Failed -> {
                throw SnippetExecutionException(
                    "Snippet execution failed",
                    executionResult.errors,
                )
            }
        }
    }
}
