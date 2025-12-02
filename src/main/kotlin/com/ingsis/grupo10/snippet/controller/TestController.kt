package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.dto.TestResponseDto
import com.ingsis.grupo10.snippet.dto.tests.RunTestRequest
import com.ingsis.grupo10.snippet.dto.tests.TestResultResponse
import com.ingsis.grupo10.snippet.service.TestService
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tests")
class TestController(
    private val testService: TestService,
    private val authClient: AuthClient,
) {
    @GetMapping("/{snippetId}")
    fun getTestsBySnippet(
        @PathVariable snippetId: UUID,
    ): ResponseEntity<List<TestResponseDto>> {
        val tests = testService.getTestsBySnippet(snippetId)
        return ResponseEntity.ok(tests)
    }

//    @GetMapping("/{testId}")
//    fun getTestById(
//        @PathVariable testId: UUID,
//    ): ResponseEntity<TestResponseDto> {
//        val test = testService.getTestById(testId)
//        return ResponseEntity.ok(test)
//    }

    @PostMapping("/{snippetId}")
    fun createTest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable snippetId: UUID,
        @RequestBody request: TestCreateRequest,
    ): ResponseEntity<TestResponseDto> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(snippetId, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val test = testService.createTest(snippetId, request)
        return ResponseEntity.ok(test)
    }

    @PutMapping("/{testId}/{snippetId}")
    fun updateTest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable testId: UUID,
        @PathVariable snippetId: UUID,
        @RequestBody request: TestCreateRequest,
    ): ResponseEntity<TestResponseDto> {
        val test = testService.updateTest(testId, request)
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(snippetId, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return ResponseEntity.ok(test)
    }

    @DeleteMapping("/{testId}/{snippetId}"
    )
    fun deleteTest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable testId: UUID,
        @PathVariable snippetId: UUID,
    ): ResponseEntity<Void> {
        val userId = jwt.subject

        val hasOwnerPermission = authClient.checkPermission(snippetId, userId, "OWNER")

        if (!hasOwnerPermission) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        testService.deleteTest(testId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/run/{snippetId}")
    fun runTest(
        @PathVariable snippetId: UUID,
        @RequestBody request: RunTestRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<TestResultResponse> {
        val userId = jwt.subject
        val result = testService.runTest(snippetId, userId, request)
        return ResponseEntity.ok(result)
    }
}
