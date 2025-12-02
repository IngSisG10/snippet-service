package com.ingsis.grupo10.snippet.dto

data class FoundUsersDto(
    val totalCount: Int,
    val users: List<UIUserResponse>,
)
