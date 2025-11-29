package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.rules.RuleConfigRequest
import com.ingsis.grupo10.snippet.dto.rules.RuleConfigResponse
import com.ingsis.grupo10.snippet.dto.rules.RuleDto
import com.ingsis.grupo10.snippet.service.RuleConfigService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rules")
class RulesConfigController(
    private val ruleConfigService: RuleConfigService,
) {
    @GetMapping("/format")
    fun getFormattingRules(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<RuleConfigResponse>> {
        val userId = jwt.subject
        val rules = ruleConfigService.getFormattingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @GetMapping("/lint")
    fun getLintingRules(
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<RuleDto>> {
        val userId = jwt.subject
        val rules = ruleConfigService.getLintingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @PutMapping("/format")
    fun updateFormattingRules(
        @RequestBody rules: List<RuleConfigRequest>,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<RuleConfigResponse>> {
        val userId = jwt.subject

        // Update the formatting rules
        val updatedRules = ruleConfigService.updateFormattingRules(rules, userId)

        ruleConfigService.generateFormatEvents(userId)

        return ResponseEntity.ok(updatedRules)
    }

    @PutMapping("/lint")
    fun updateLintingRules(
        @RequestBody rules: Map<String, Any>,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<Void> {
        ruleConfigService.updateLintingRules(rules, jwt.subject)
        return ResponseEntity.ok().build()
    }
}
