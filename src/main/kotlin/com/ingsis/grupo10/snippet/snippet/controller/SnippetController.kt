package com.ingsis.grupo10.snippet.snippet.controller

import com.ingsis.grupo10.snippet.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.snippet.dto.SnippetResponseDto
import com.ingsis.grupo10.snippet.snippet.service.SnippetService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService,
) {
    // todo: file: Blob Storage

    @GetMapping
    fun getAllSnippets(): ResponseEntity<List<SnippetResponseDto>> = ResponseEntity.ok(snippetService.getAllSnippets())

    @GetMapping("/{id}")
    fun getSnippetById(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDto> {
        val snippet = snippetService.getSnippetById(id)
        return ResponseEntity.ok(snippet)
    }

    @PostMapping("/create")
    fun createSnippet(
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetResponseDto> {
        val created = snippetService.createSnippet(request)
        return ResponseEntity.ok(created)
    }

    @DeleteMapping("/{id}")
    fun deleteSnippet(
        @PathVariable id: UUID,
    ): ResponseEntity<SnippetResponseDto> {
        snippetService.deleteSnippetById(id)
        return ResponseEntity.ok().build()
    }

    @PutMapping("/{id}")
    fun updateSnippet(
        @PathVariable id: UUID,
        @RequestBody request: SnippetCreateRequest,
    ): ResponseEntity<SnippetResponseDto> = ResponseEntity.ok(snippetService.updateSnippet(id, request))
}
