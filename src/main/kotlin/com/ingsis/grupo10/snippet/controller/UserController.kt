package com.ingsis.grupo10.snippet.controller

import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.PaginatedUsersResponse
import com.ingsis.grupo10.snippet.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val authClient: AuthClient,
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

    @PostMapping("/register-or-login")
    fun registerOrLogin(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<Map<String, Any>> {
        val userId = jwt.subject
        val email = jwt.getClaimAsString("email")
            ?: jwt.getClaimAsString("https://your-app.com/email")
        val name = jwt.getClaimAsString("name")
            ?: jwt.getClaimAsString("https://your-app.com/name")

        val success = authClient.registerOrLoginUser(userId, email, name)

        return if (success) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "User registered/logged in successfully",
                "userId" to userId
            ))
        } else {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf(
                    "success" to false,
                    "message" to "Failed to register user"
                ))
        }
    }

}
