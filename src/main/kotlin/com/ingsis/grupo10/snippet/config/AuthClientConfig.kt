package com.ingsis.grupo10.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AuthClientConfig {
    @Value("\${auth.service.url}")
    private lateinit var authServiceUrl: String

    @Bean
    fun authWebClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(authServiceUrl)
            .build()
}
