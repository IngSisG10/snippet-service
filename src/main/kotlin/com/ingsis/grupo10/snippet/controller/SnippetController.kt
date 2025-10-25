package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.service.SnippetService
import com.ingsis.grupo10.snippet.util.UserContext
import org.springframework.http.ResponseEntity
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
) {
    // todo: file: Blob Storage

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
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val created = snippetService.createSnippet(request)
        return ResponseEntity.ok(created)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        snippetService.deleteSnippetById(id)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    fun updateSnippet(
        @PathVariable id: UUID,
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val updated = snippetService.updateSnippet(id, request)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/{id}/lint")
    fun lintSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val snippet = snippetService.lintSnippet(id)
        return ResponseEntity.ok(snippet)
    }

    @PostMapping("/{id}/format")
    fun formatSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val snippet = snippetService.formatSnippet(id)
        return ResponseEntity.ok(snippet)
    }

    /**
     * Gets all snippets owned by the current user.
     *
     * @return List of snippets owned by the current user
     */
    @GetMapping("/my-snippets")
    fun getMySnippets(): ResponseEntity<List<SnippetSummaryDto>> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val userId = UserContext.getCurrentUserIdAsString()
        val snippets = snippetService.getSnippetsByUser(userId)
        return ResponseEntity.ok(snippets)
    }
}
