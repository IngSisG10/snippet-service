package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigResponse
import com.ingsis.grupo10.snippet.service.LintConfigService
import com.ingsis.grupo10.snippet.util.UserContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config/linting")
class LintConfigController(
    private val lintConfigService: LintConfigService,
) {
    @GetMapping
    fun getConfig(): ResponseEntity<LintConfigResponse> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val userId = UserContext.getCurrentUserId()
        val config = lintConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @RequestBody request: LintConfigRequest,
    ): ResponseEntity<LintConfigResponse> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val userId = UserContext.getCurrentUserId()
        val config = lintConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
