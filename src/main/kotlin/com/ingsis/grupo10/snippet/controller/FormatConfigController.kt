package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigResponse
import com.ingsis.grupo10.snippet.service.FormatConfigService
import com.ingsis.grupo10.snippet.util.UserContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/config/formatting")
class FormatConfigController(
    private val formatConfigService: FormatConfigService,
) {
    @GetMapping
    fun getConfig(): ResponseEntity<FormatConfigResponse> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val userId = UserContext.getCurrentUserId()
        val config = formatConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @RequestBody request: FormatConfigRequest,
    ): ResponseEntity<FormatConfigResponse> {
        // TODO: When auth-service is implemented, extract userId from JWT token
        // For now, use UserContext to get the current user ID
        val userId = UserContext.getCurrentUserId()
        val config = formatConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
