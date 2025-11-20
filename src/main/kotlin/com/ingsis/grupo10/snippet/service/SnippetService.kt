package com.ingsis.grupo10.snippet.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.client.AssetClient
import com.ingsis.grupo10.snippet.client.PrintScriptClient
import com.ingsis.grupo10.snippet.dto.Created
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.filetype.FileTypeDto
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import com.ingsis.grupo10.snippet.dto.validation.ValidationResult
import com.ingsis.grupo10.snippet.exception.SnippetValidationException
import com.ingsis.grupo10.snippet.extension.created
import com.ingsis.grupo10.snippet.extension.toDetailDto
import com.ingsis.grupo10.snippet.models.Snippet
import com.ingsis.grupo10.snippet.repository.LanguageRepository
import com.ingsis.grupo10.snippet.repository.SnippetRepository
import com.ingsis.grupo10.snippet.util.AssetUtils.parseCodeUrl
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
    private val logService: LogService,
    private val lintConfigService: LintConfigService,
    private val formatConfigService: FormatConfigService,
    private val objectMapper: ObjectMapper,
) {
    fun getSnippetById(id: UUID): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        return snippet.toDetailDto()
    }

    fun createSnippet(
        request: SnippetCreateRequest,
        snippetId: UUID,
    ): Created {
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

                val assetResult =
                    assetClient.createAsset(
                        container = "snippets",
                        key = snippetId.toString(), // asociamos esta key con el ID del snippet
                        content = request.code,
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
                        description = request.description,
                        version = request.version,
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
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

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

                val (container, key) = parseCodeUrl(existingSnippet.codeUrl)

                assetClient.createAsset(
                    container = container,
                    key = key,
                    content = request.code,
                )

                val updatedSnippet =
                    Snippet(
                        id = existingSnippet.id,
                        name = request.name,
                        description = request.description,
                        codeUrl = existingSnippet.codeUrl,
                        language = language,
                        version = request.version,
                        createdAt = existingSnippet.createdAt,
                        updatedAt = LocalDateTime.now(),
                    )

                val saved = snippetRepository.save(updatedSnippet)

                logService.logValidation(saved, emptyList())

                return saved.toDetailDto()
            }
        }
    }

    @Transactional
    fun lintSnippet(id: UUID): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // TODO: Get user-specific lint config - for now use default
        val lintConfig = "{}" // Default config
//        val lintConfig = lintConfigService.getConfigJson(userUuid)

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
    fun formatSnippet(id: UUID): SnippetDetailDto {
        val snippet =
            snippetRepository
                .findById(id)
                .orElseThrow { IllegalArgumentException("Snippet not found") }

        // TODO: Get user-specific format config - for now use default
        val formatConfig = """{"enforce-spacing-around-equals": true}"""
//        val formatConfig = formatConfigService.getConfigJson(userUuid)

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

        return snippet.toDetailDto()
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

    // Rules
    fun getFormattingRules(userId: String): List<RuleDto> {
        val json = formatConfigService.getConfigJson(userId)
        val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>

        return mapToRuleList(map)
    }

    fun getLintingRules(userId: String): List<RuleDto> {
        val json = lintConfigService.getConfigJson(userId)
        val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
        return mapToRuleList(map)
    }

    fun updateFormattingRules(
        rules: List<RuleDto>,
        userId: String,
    ) {
        val request = rulesToFormatConfigRequest(rules)
        formatConfigService.updateConfig(userId, request)
    }

    fun updateLintingRules(
        rules: Map<String, Any>,
        userId: String,
    ) {
        val request = rulesToLintConfigRequest(rules)
        lintConfigService.updateConfig(userId, request)
    }

    // Helpers
    // todo: tiralo en helpers
    private fun mapToRuleList(config: Map<String, Any?>): List<RuleDto> =
        config.map { (key, value) ->
            RuleDto(
                id = key,
                name = formatKeyToHumanName(key),
                isActive = true, // fixme: Why? -> siempre activas por ahora
                value = value,
            )
        }

    private fun formatKeyToHumanName(key: String): String = key.split("_").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

    private fun rulesToFormatConfigRequest(rules: List<RuleDto>): FormatConfigRequest {
        var spaceBeforeColon: Boolean? = null
        var spaceAfterColon: Boolean? = null
        var spaceAroundEquals: Boolean? = null
        var newlineBeforePrintln: Int? = null
        var indentInsideBlock: Int? = null

        rules.forEach { rule ->
            when (rule.id) {
                "space_before_colon" -> spaceBeforeColon = rule.value as? Boolean
                "space_after_colon" -> spaceAfterColon = rule.value as? Boolean
                "space_around_equals" -> spaceAroundEquals = rule.value as? Boolean
                "newline_before_println" -> newlineBeforePrintln = (rule.value as? Number)?.toInt()
                "indent_inside_block" -> indentInsideBlock = (rule.value as? Number)?.toInt()
            }
        }

        return FormatConfigRequest(
            spaceBeforeColon = spaceBeforeColon,
            spaceAfterColon = spaceAfterColon,
            spaceAroundEquals = spaceAroundEquals,
            newlineBeforePrintln = newlineBeforePrintln,
            indentInsideBlock = indentInsideBlock,
        )
    }

    private fun rulesToLintConfigRequest(rules: Map<String, Any>): LintConfigRequest =
        LintConfigRequest(
            identifierFormat = rules["identifierFormat"] as? String,
            printlnExpressionAllowed = rules["printlnExpressionAllowed"] as? Boolean,
            readInputExpressionAllowed = rules["readInputExpressionAllowed"] as? Boolean,
        )

    // Test Cases
    fun getTestCases(): Map<String, Any> {
        TODO()
    }

    fun postTestCase(testCase: Map<String, Any>) {
        TODO()
    }

    fun removeTestCase(testCaseId: UUID) {
        TODO()
    }

    // File Types
    fun getSupportedFileTypes(): FileTypeDto {
        val languageRepository = languageRepository.findAll()
        val fileTypes = languageRepository.map { it.name }
        return FileTypeDto(fileTypes)
    }
}
