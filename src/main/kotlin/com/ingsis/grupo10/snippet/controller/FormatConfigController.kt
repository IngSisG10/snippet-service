package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigResponse
import com.ingsis.grupo10.snippet.service.FormatConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/config/formatting")
class FormatConfigController(
    private val formatConfigService: FormatConfigService,
) {
    @GetMapping
    fun getConfig(): ResponseEntity<FormatConfigResponse> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val config = formatConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @RequestBody request: FormatConfigRequest,
    ): ResponseEntity<FormatConfigResponse> {
        // TODO: Extract userId from JWT token when auth is implemented
        val userId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val config = formatConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
