package com.ingsis.grupo10.snippet.health

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/")
    fun getHealth(): ResponseEntity<String> =
        ResponseEntity.ok("Snippet service is alive!")

    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<String> =
        ResponseEntity.ok("OK")

    // This endpoint requires authentication (JWT token)
    @GetMapping("/secret/health")
    fun authenticatedHealth(): ResponseEntity<String> =
        ResponseEntity.ok("You are authenticated and the service is alive!")
}