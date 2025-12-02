package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.GrantPermissionRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.SnippetUICreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetUIDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetUIFormatDto
import com.ingsis.grupo10.snippet.dto.SnippetUIUpdateRequest
import com.ingsis.grupo10.snippet.dto.filetype.FileTypeResponse
import com.ingsis.grupo10.snippet.dto.paginatedsnippets.PaginatedSnippetsResponse
import com.ingsis.grupo10.snippet.dto.tests.ExecutionDto
import com.ingsis.grupo10.snippet.producer.FormatRequestProducer
import com.ingsis.grupo10.snippet.producer.LintRequestProducer
import com.ingsis.grupo10.snippet.service.SnippetService
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetUIDetailDto> {
        val username = jwt.getClaimAsString("https://your-app.com/name")
        val snippet = snippetService.getUISnippetById(id, username)
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
                snippetService.createSnippet(request, userId, snippetId)
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
        @RequestBody request: SnippetUIUpdateRequest,
    ): ResponseEntity<SnippetDetailDto> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val updated = snippetService.updateSnippet(id, userId, request)

        // todo: Test automaticos
        // snippetService.generateTestEvents(id)

        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/format")
    fun formatSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetUIFormatDto> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(id, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val snippet = snippetService.formatSnippet(userId, id)
        return ResponseEntity.ok(snippet)
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

    // Rules

    // File types
    @GetMapping("/filetypes")
    fun getSupportedFileTypes(): ResponseEntity<List<FileTypeResponse>> {
        val fileTypes = snippetService.getSupportedFileTypes()
        return ResponseEntity.ok(fileTypes)
    }

    @PostMapping("/share")
    fun shareSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: GrantPermissionRequest,
    ): ResponseEntity<SnippetUIDetailDto> {
        val userId = jwt.subject
        val username = jwt.getClaimAsString("https://your-app.com/name")

        val hasOwnerPermission = authClient.checkPermission(request.snippetId, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val response = snippetService.shareSnippet(username, request.snippetId, request.targetUserEmail)

        return ResponseEntity.ok(response)
    }

    @PostMapping("/run/{id}")
    fun runSnippet(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
    ): ResponseEntity<ExecutionDto> {
        val userId = jwt.subject
        val response = snippetService.runSnippet(userId, id)
        return ResponseEntity.ok(response)
    }
}
