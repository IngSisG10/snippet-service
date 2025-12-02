package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.dto.PaginatedUsersResponse
import com.ingsis.grupo10.snippet.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping("/friends")
    fun getFriends(
        @RequestParam name: String,
        @RequestParam page: Int,
        @RequestParam pageSize: Int,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<PaginatedUsersResponse> {
        val userId = jwt.subject
        val friends =
            userService.getFriends(
                userId = userId,
                email = name,
                page = page,
                pageSize = pageSize,
            )
        return ResponseEntity.ok(friends)
    }
}
