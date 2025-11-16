package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigResponse
import com.ingsis.grupo10.snippet.service.LintConfigService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
    fun getConfig(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<LintConfigResponse> {
        val userId = jwt.subject

//        val userId = UserContext.getCurrentUserId()
        val config = lintConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: LintConfigRequest,
    ): ResponseEntity<LintConfigResponse> {
        val userId = jwt.subject
//        val userId = UserContext.getCurrentUserId()

        val config = lintConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
