package com.ingsis.grupo10.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PrintScriptClientConfig {
    @Value("\${printscript.service.url}")
    private lateinit var printScriptServiceUrl: String

    @Bean
    fun webClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(printScriptServiceUrl)
            .build()
}
