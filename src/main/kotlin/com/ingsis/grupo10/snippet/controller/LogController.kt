package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.log.LintStatus
import com.ingsis.grupo10.snippet.dto.log.LogDto
import com.ingsis.grupo10.snippet.dto.log.TestExecutionResult
import com.ingsis.grupo10.snippet.service.LogService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/logs")
class LogController(
    private val logService: LogService,
) {
    // Traer todos los logs de un snippet, opcionalmente filtrados por tag
    @GetMapping("/snippets/{snippetId}")
    fun getSnippetLogs(
        @PathVariable snippetId: UUID,
        @RequestParam(required = false) tag: String?,
    ): ResponseEntity<List<LogDto>> {
        val logs = logService.getSnippetLogs(snippetId, tag)
        return ResponseEntity.ok(logs)
    }

    // Traer el último estado de linting de un snippet
    @GetMapping("/snippets/{snippetId}/lint-status")
    fun getLintStatus(
        @PathVariable snippetId: UUID,
    ): ResponseEntity<LintStatus> {
        val lintStatus = logService.getLatestLintStatus(snippetId)
        return ResponseEntity.ok(lintStatus)
    }

    // Traer la versión formateada de un snippet
    @GetMapping("/snippets/{snippetId}/formatted")
    fun getFormattedVersion(
        @PathVariable snippetId: UUID,
    ): ResponseEntity<String> {
        val formattedCode = logService.getFormattedVersion(snippetId)
        return if (formattedCode != null) {
            ResponseEntity.ok(formattedCode)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Traer el historial de ejecuciones de tests para un test específico
    @GetMapping("/tests/{testId}/executions")
    fun getTestExecutionHistory(
        @PathVariable testId: UUID,
    ): ResponseEntity<List<TestExecutionResult>> {
        val history = logService.getTestExecutionHistory(testId)
        return ResponseEntity.ok(history)
    }
}
