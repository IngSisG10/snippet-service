package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigResponse
import com.ingsis.grupo10.snippet.service.FormatConfigService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
    fun getConfig(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<FormatConfigResponse> {
        val userId = jwt.subject

//        val userId = UserContext.getCurrentUserId()
        val config = formatConfigService.getConfig(userId)
        return ResponseEntity.ok(config)
    }

    @PutMapping
    fun updateConfig(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: FormatConfigRequest,
    ): ResponseEntity<FormatConfigResponse> {
        val userId = jwt.subject

//        val userId = UserContext.getCurrentUserId()
        val config = formatConfigService.updateConfig(userId, request)
        return ResponseEntity.ok(config)
    }
}
