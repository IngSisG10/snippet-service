package com.ingsis.grupo10.snippet.service

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.PaginatedUsersResponse
import org.springframework.stereotype.Service

@Service
class UserService(
    private val authClient: AuthClient,
) {
    fun getFriends(
        userId: String,
        email: String,
        page: Int,
        pageSize: Int,
    ): PaginatedUsersResponse {
        val result = authClient.getUsers(userId, email, page, pageSize)
        return PaginatedUsersResponse(
            users = result.users,
            count = result.totalCount,
            page = page,
            page_size = pageSize,
        )
    }
}
