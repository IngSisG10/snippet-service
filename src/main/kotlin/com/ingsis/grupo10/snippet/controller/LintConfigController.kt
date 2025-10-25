package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigResponse
import com.ingsis.grupo10.snippet.service.LintConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/config/linting")
class LintConfigController(
    private val lintConfigService: LintConfigService,
) {
    @GetMapping
    fun getConfig(): ResponseEntity<LintConfigResponse> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val config = lintConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @RequestBody request: LintConfigRequest,
    ): ResponseEntity<LintConfigResponse> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val config = lintConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
