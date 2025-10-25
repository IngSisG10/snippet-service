package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.service.SnippetService
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
        val created = snippetService.createSnippet(request) // TODO: Agregar lógica para extraer userId de JWT
        return ResponseEntity.ok(created)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        snippetService.deleteSnippetById(id)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    fun updateSnippet(
        @PathVariable id: UUID,
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetDetailDto> = ResponseEntity.ok(snippetService.updateSnippet(id, request))

    @PostMapping("/{id}/lint")
    fun lintSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val snippet = snippetService.lintSnippet(id, userId)
        return ResponseEntity.ok(snippet)
    }

    @PostMapping("/{id}/format")
    fun formatSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetDetailDto> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val snippet = snippetService.formatSnippet(id, userId)
        return ResponseEntity.ok(snippet)
    }
}
