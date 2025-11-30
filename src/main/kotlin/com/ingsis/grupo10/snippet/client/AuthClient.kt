package com.ingsis.grupo10.snippet.client

import com.ingsis.grupo10.snippet.dto.FoundUsersDto
import com.ingsis.grupo10.snippet.dto.GrantPermissionRequest
import com.ingsis.grupo10.snippet.dto.snippets.PermissionCheckResponse
import com.ingsis.grupo10.snippet.dto.snippets.RegisterSnippetRequest
import com.ingsis.grupo10.snippet.dto.snippets.SnippetPermissionInfo
import com.ingsis.grupo10.snippet.dto.snippets.SnippetPermissionInformation
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.UUID

@Service
class AuthClient(
    @Qualifier("authWebClient") private val webClient: WebClient,
) {
    fun registerSnippet(
        snippetId: UUID,
        userId: String,
    ): Boolean {
        val request = RegisterSnippetRequest(snippetId, userId)

        return try {
            val response =
                webClient
                    .post()
                    .uri("/permissions/snippets")
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

    fun checkUserExists(userId: String): Boolean =
        try {
            val requestBody = mapOf("userId" to userId)

            val response =
                webClient
                    .post() // ← Changed from GET to POST
                    .uri("/users/exists") // ← No more path variable
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono<Map<String, Boolean>>()
                    .block()

            response?.get("exists") ?: false
        } catch (ex: Exception) {
            println("Error checking if user exists: ${ex.message}")
            false
        }

    fun checkPermission(
        snippetId: UUID,
        userId: String,
        permission: String = "READ",
    ): Boolean =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/snippets/$snippetId/check?userId=$userId&requiredPermission=$permission")
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
        userId: String,
    ): Boolean =
        try {
            val response =
                webClient
                    .delete()
                    .uri("/permissions/snippets/$snippetId?userId=$userId")
                    .retrieve()
                    .toBodilessEntity()
                    .block()

            response?.statusCode?.is2xxSuccessful == true
        } catch (ex: Exception) {
            println("Error unregistering snippet: ${ex.message}")
            false
        }

    fun getUserOwnedSnippets(userId: String): List<UUID> =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/owned-snippets?userId=$userId")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono<Array<SnippetPermissionInfo>>()
                    .block()

            response?.map { it.snippetId } ?: emptyList()
        } catch (ex: Exception) {
            println("Error getting owned snippets: ${ex.message}")
            ex.printStackTrace()
            emptyList()
        }

    // Get all snippets the user has any access to (READ, WRITE, or OWNER)
    fun getUserAccessibleSnippets(userId: String): List<UUID> =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/accessible-snippets?userId=$userId")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono<Array<SnippetPermissionInfo>>()
                    .block()

            response?.map { it.snippetId } ?: emptyList()
        } catch (ex: Exception) {
            println("Error getting accessible snippets: ${ex.message}")
            ex.printStackTrace()
            emptyList()
        }

    fun getUserReadSnippets(userId: String): List<UUID> =
        try {
            val response =
                webClient
                    .get()
                    .uri("/permissions/read-snippets?userId=$userId")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono<Array<SnippetPermissionInfo>>()
                    .block()
            response?.map { it.snippetId } ?: emptyList()
        } catch (ex: Exception) {
            println("Error getting accessible snippets: ${ex.message}")
            ex.printStackTrace()
            emptyList()
        }

    fun shareSnippet(
        snippetId: UUID,
        targetUserEmail: String,
    ): Boolean =
        try {
            val requestBody = GrantPermissionRequest(snippetId, targetUserEmail)

            val response =
                webClient
                    .post()
                    .uri("/permissions/snippets/grant")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block()

            response?.statusCode?.is2xxSuccessful == true
        } catch (ex: Exception) {
            println("Error sharing snippet: ${ex.message}")
            false
        }

    fun getUsers(
        userId: String,
        email: String?,
        page: Int?,
        pageSize: Int?,
    ): FoundUsersDto =
        try {
            webClient
                .get()
                .uri("/users/search?userId=$userId&email=$email&page=$page&pageSize=$pageSize")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono<FoundUsersDto>()
                .block()
                ?: throw RuntimeException("Empty response from auth service")
        } catch (ex: Exception) {
            println("Error fetching users: ${ex.message}")
            throw ex
        }

    fun getSnippetOwner(snippetId: UUID): SnippetPermissionInformation? =
        try {
            val response = webClient
                .get()
                .uri("/permissions/snippets/$snippetId/owner")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono<SnippetPermissionInformation>()
                .block()

            response
        } catch (ex: Exception) {
            println("Error getting snippet owner: ${ex.message}")
            null
        }
}
