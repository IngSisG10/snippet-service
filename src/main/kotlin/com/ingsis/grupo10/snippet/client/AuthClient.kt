package com.ingsis.grupo10.snippet.client

import com.ingsis.grupo10.snippet.dto.snippets.PermissionCheckResponse
import com.ingsis.grupo10.snippet.dto.snippets.RegisterSnippetRequest
import com.ingsis.grupo10.snippet.dto.snippets.SnippetPermissionInfo
import com.ingsis.grupo10.snippet.dto.snippets.ValidateTokenResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

@Service
class AuthClient(
    @Qualifier("authWebClient")
    private val webClient: WebClient,
) {
    fun getCurrentUser(token: String): ValidateTokenResponse? =
        try {
            webClient
                .get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono<ValidateTokenResponse>()
                .block()
        } catch (ex: Exception) {
            println("Error validating token: ${ex.message}")
            null
        }

    fun registerSnippet(
        snippetId: UUID,
        ownerId: String,
        token: String,
    ): Boolean {
        val request = RegisterSnippetRequest(snippetId, ownerId)

        return try {
            val response =
                webClient
                    .post()
                    .uri("/permissions/snippets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block()

            response?.statusCode?.is2xxSuccessful == true
        } catch (ex: Exception) {
            println("Error registering snippet: ${ex.message}")
            false
        }
    }

    fun checkPermission(
        snippetId: UUID,
        token: String,
        permission: String = "READ",
    ): Boolean =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/snippets/$snippetId/check?requiredPermission=$permission")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono<PermissionCheckResponse>()
                    .block()

            response?.hasPermission ?: false
        } catch (ex: Exception) {
            println("Error checking permission: ${ex.message}")
            false
        }

    fun unregisterSnippet(
        snippetId: UUID,
        token: String,
    ): Boolean =
        try {
            val response =
                webClient
                    .delete()
                    .uri("/permissions/snippets/$snippetId")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .toBodilessEntity()
                    .block()

            response?.statusCode?.is2xxSuccessful == true
        } catch (ex: Exception) {
            println("Error unregistering snippet: ${ex.message}")
            false
        }

    fun getUserAccessibleSnippets(token: String): List<UUID> =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/my-snippets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono<Array<SnippetPermissionInfo>>()
                    .block()

            response?.map { it.snippetId } ?: emptyList()
        } catch (ex: Exception) {
            println("Error getting accessible snippets: ${ex.message}")
            emptyList()
        }
}
