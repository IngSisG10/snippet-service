package com.ingsis.grupo10.snippet.snippet.controller

import com.ingsis.grupo10.snippet.models.Language
import com.ingsis.grupo10.snippet.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.snippet.dto.SnippetResponseDto
import com.ingsis.grupo10.snippet.snippet.service.SnippetService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/snippets")
class SnippetController(
    private val snippetService: SnippetService,
) {
    // file: MultipartFile -> Como hacemos esto? Tenemos que hacerlo?

    @PostMapping("/create")
    fun createSnippet(
        @RequestParam name: String,
        @RequestParam description: String,
        @RequestParam language: Language,
        @RequestParam code: String,
        @RequestParam version: String,
    ): ResponseEntity<SnippetResponseDto> {
        val request =
            SnippetCreateRequest(
                name,
                description,
                code,
                language,
                version,
            )
        val created = snippetService.createSnippet(request)
        return ResponseEntity.ok(created)
    }
}
