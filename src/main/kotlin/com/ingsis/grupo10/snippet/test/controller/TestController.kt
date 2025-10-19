package com.ingsis.grupo10.snippet.test.controller

import com.ingsis.grupo10.snippet.test.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.test.dto.TestResponseDto
import com.ingsis.grupo10.snippet.test.service.TestService
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
class TestController(
    private val testService: TestService,
) {
    @PostMapping("/{snippetId}/tests")
    fun createTest(
        @PathVariable snippetId: UUID,
        @RequestBody request: TestCreateRequest,
    ): ResponseEntity<TestResponseDto> {
        val test = testService.createTest(snippetId, request)
        return ResponseEntity.ok(test)
    }

    @GetMapping("/{snippetId}/tests")
    fun getTestsBySnippet(
        @PathVariable snippetId: UUID,
    ): ResponseEntity<List<TestResponseDto>> {
        val tests = testService.getTestsBySnippet(snippetId)
        return ResponseEntity.ok(tests)
    }

    @GetMapping("/tests/{testId}")
    fun getTestById(
        @PathVariable testId: UUID,
    ): ResponseEntity<TestResponseDto> {
        val test = testService.getTestById(testId)
        return ResponseEntity.ok(test)
    }

    @PutMapping("/tests/{testId}")
    fun updateTest(
        @PathVariable testId: UUID,
        @RequestBody request: TestCreateRequest,
    ): ResponseEntity<TestResponseDto> {
        val test = testService.updateTest(testId, request)
        return ResponseEntity.ok(test)
    }

    @DeleteMapping("/tests/{testId}")
    fun deleteTest(
        @PathVariable testId: UUID,
    ): ResponseEntity<Void> {
        testService.deleteTest(testId)
        return ResponseEntity.noContent().build()
    }
}
