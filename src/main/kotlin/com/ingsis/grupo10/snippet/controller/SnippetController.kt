package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.SnippetUICreateRequest
import com.ingsis.grupo10.snippet.dto.filetype.FileTypeResponse
import com.ingsis.grupo10.snippet.dto.paginatedsnippets.PaginatedSnippetsResponse
import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import com.ingsis.grupo10.snippet.models.Test
import com.ingsis.grupo10.snippet.producer.FormatRequestProducer
import com.ingsis.grupo10.snippet.producer.LintRequestProducer
import com.ingsis.grupo10.snippet.service.SnippetService
import com.ingsis.grupo10.snippet.service.TestCaseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService,
    private val testCaseService: TestCaseService,
    private val authClient: AuthClient,
    private val lintRequestProducer: LintRequestProducer,
    private val formatRequestProducer: FormatRequestProducer,
) {
    @GetMapping("/descriptors")
    fun listSnippetDescriptors(
        @RequestParam page: Int,
        @RequestParam pageSize: Int,
        @RequestParam(required = false, defaultValue = "") name: String?,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<PaginatedSnippetsResponse> {
        val username = jwt.getClaimAsString("https://your-app.com/name")

        val result =
            snippetService.listSnippetDescriptors(
                userId = username,
                page = page,
                pageSize = pageSize,
                name = name,
            )

        return ResponseEntity.ok(result)
    }

    // getAll de toda la DB? No tiene sentido, deberia ser por el owner, o los shared.
    @GetMapping
    fun getAllSnippets(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) compliance: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false, defaultValue = "ASC") sortDirection: String?,
    ): ResponseEntity<List<SnippetSummaryDto>> =
        ResponseEntity.ok(
            snippetService.getAllSnippets(
                name = name,
                language = language,
                compliance = compliance,
                sortBy = sortBy,
                sortDirection = sortDirection,
            ),
        )

    @GetMapping("/{id}")
    fun getSnippetById(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        val snippet = snippetService.getSnippetById(id)
        return ResponseEntity.ok(snippet)
    }

    @PostMapping("/create")
    fun createSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: SnippetUICreateRequest,
    ): ResponseEntity<Any> {
        val userId = jwt.subject

        // Check if user exists in auth service
        val userExists = authClient.checkUserExists(userId)

        if (!userExists) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to "User not registered. Please access the application first."))
        }

        // Generate snippet ID upfront
        val snippetId = UUID.randomUUID()

        // Register snippet ownership in auth service
        val registered = authClient.registerSnippet(snippetId, userId)

        if (!registered) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to register snippet ownership"))
        }

        // Create snippet
        val created =
            try {
                snippetService.createSnippet(request, snippetId)
            } catch (ex: Exception) {
                // ROLLBACK: Unregister snippet if creation fails
                authClient.unregisterSnippet(snippetId, userId)
                throw ex
            }

        return ResponseEntity.ok(created)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val userId = jwt.subject

        // Check Permission
        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        snippetService.deleteSnippetById(id)
        authClient.unregisterSnippet(id, userId)

        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    fun updateSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetDetailDto> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val updated = snippetService.updateSnippet(id, request)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/lint")
    fun lintSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, String>> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Publicar mensaje al Redis Stream
        lintRequestProducer.publishLintRequest(id.toString())

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(mapOf("message" to "Lint request queued for processing"))
    }

    @PostMapping("/{id}/format")
    fun formatSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
    ): ResponseEntity<Map<String, String>> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        // Publicar mensaje al Redis Stream
        formatRequestProducer.publishFormatRequest(id.toString())

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(mapOf("message" to "Format request queued for processing"))
    }

    @GetMapping("/my-snippets")
    fun getMySnippets(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<SnippetSummaryDto>> {
        val userId = jwt.subject

        // Get snippet IDs from auth service FIRST
        val snippetIds = authClient.getUserAccessibleSnippets(userId)

        // Then get snippet details for each ID
        val snippets =
            snippetIds.mapNotNull { snippetId ->
                try {
                    snippetService.getSnippetById(snippetId).let { detail ->
                        SnippetSummaryDto(
                            id = detail.id,
                            name = detail.name,
                            language = detail.language,
                            version = detail.version,
                            createdAt = detail.createdAt,
                            compliance = null,
                        )
                    }
                } catch (ex: Exception) {
                    null // Skip deleted snippets
                }
            }

        return ResponseEntity.ok(snippets)
    }

    @GetMapping("/my-owned-snippets")
    fun getMyOwnedSnippets(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<SnippetSummaryDto>> {
        val userId = jwt.subject

        // Get only owned snippet IDs
        val snippetIds = authClient.getUserOwnedSnippets(userId)

        val snippets =
            snippetIds.mapNotNull { snippetId ->
                try {
                    snippetService.getSnippetById(snippetId).let { detail ->
                        SnippetSummaryDto(
                            id = detail.id,
                            name = detail.name,
                            language = detail.language,
                            version = detail.version,
                            createdAt = detail.createdAt,
                            compliance = null,
                        )
                    }
                } catch (ex: Exception) {
                    println("Error loading snippet $snippetId: ${ex.message}")
                    null
                }
            }

        return ResponseEntity.ok(snippets)
    }

    @GetMapping("/my-read-snippets")
    fun getMyReadSnippets(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<SnippetSummaryDto>> {
        val userId = jwt.subject

        // Get only read-access snippet IDs
        val snippetIds = authClient.getUserReadSnippets(userId)

        val snippets =
            snippetIds.mapNotNull { snippetId ->
                try {
                    snippetService.getSnippetById(snippetId).let { detail ->
                        SnippetSummaryDto(
                            id = detail.id,
                            name = detail.name,
                            language = detail.language,
                            version = detail.version,
                            createdAt = detail.createdAt,
                            compliance = null,
                        )
                    }
                } catch (ex: Exception) {
                    println("Error loading snippet $snippetId: ${ex.message}")
                    null
                }
            }

        return ResponseEntity.ok(snippets)
    }

    // TODO: add share snippet method.
    // TODO: Add auth verification

    // Rules
    @GetMapping("/rules/format")
    fun getFormattingRules(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<RuleDto>> {
        val userId = jwt.subject
        val rules = snippetService.getFormattingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @GetMapping("/rules/lint")
    fun getLintingRules(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<RuleDto>> {
        val userId = jwt.subject
        val rules = snippetService.getLintingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @PutMapping("/rules/format")
    fun updateFormattingRules(
        @RequestBody rules: List<RuleDto>,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        snippetService.updateFormattingRules(rules, jwt.subject)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/rules/lint")
    fun updateLintingRules(
        @RequestBody rules: Map<String, Any>,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        snippetService.updateLintingRules(rules, jwt.subject)
        return ResponseEntity.ok().build()
    }

    // todo: Test Cases

    @GetMapping("/testcases")
    fun getTestCases(): ResponseEntity<List<Test>> {
        val testCases = testCaseService.getTestCases()
        return ResponseEntity.ok(testCases)
    }

    @PostMapping("/testcases")
    fun postTestCase(
        @RequestBody testCases: Test,
    ): ResponseEntity<Void> {
        testCaseService.postTestCase(testCases)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/testcases/{id}")
    fun removeTestCase(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        testCaseService.removeTestCase(id)
        return ResponseEntity.ok().build()
    }

    // File types
    @GetMapping("/filetypes")
    fun getSupportedFileTypes(): ResponseEntity<List<FileTypeResponse>> {
        val fileTypes = snippetService.getSupportedFileTypes()
        return ResponseEntity.ok(fileTypes)
    }
}
