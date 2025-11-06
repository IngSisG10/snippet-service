package com.ingsis.grupo10.snippet.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

@Service
class AssetClient(
    @Qualifier("assetWebClient") private val webClient: WebClient,
) {
    fun getAsset(
        container: String,
        key: String,
    ): String =
        webClient
            .get()
            .uri("/v1/asset/$container/$key")
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToMono(String::class.java)
            .block() ?: throw RuntimeException("Asset not found")

    fun createAsset(
        container: String,
        key: String,
        content: String,
    ): CreatedResult {
        val response =
            try {
                webClient
                    .post()
                    .uri("/v1/asset/$container/$key")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(BodyInserters.fromValue(content))
                    .retrieve()
                    .toBodilessEntity()
                    .block()
            } catch (ex: Exception) {
                return CreatedResult.Failure(null, ex.message)
            }

        return if (response?.statusCode?.is2xxSuccessful == true) {
            CreatedResult.Success("https://asset-service/v1/asset/$container/$key")
        } else {
            CreatedResult.Failure(response?.statusCode?.value(), "Asset creation failed")
        }
    }

    fun deleteAsset(
        container: String,
        key: String,
    ): Boolean {
        val response =
            webClient
                .delete()
                .uri("/v1/asset/$container/$key")
                .retrieve()
                .toBodilessEntity()
                .block()

        return response?.statusCode?.is2xxSuccessful == true
    }
}
