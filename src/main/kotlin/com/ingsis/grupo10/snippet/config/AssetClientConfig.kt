package com.ingsis.grupo10.snippet.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AssetClientConfig {
    @Value("\${services.asset.base-url}")
    private lateinit var assetServiceUrl: String

    @Bean
    fun assetWebClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(assetServiceUrl)
            .build()
}
