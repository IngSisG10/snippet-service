package com.ingsis.grupo10.snippet.dto

data class PaginatedUsersResponse(
    val users: List<UIUserResponse>,
    val count: Int,
    val page: Int,
    val page_size: Int,
)
