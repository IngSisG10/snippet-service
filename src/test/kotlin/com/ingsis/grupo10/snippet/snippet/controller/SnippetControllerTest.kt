package com.ingsis.grupo10.snippet.snippet.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.client.AuthClient
import com.ingsis.grupo10.snippet.dto.Created
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.dto.SnippetUICreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetUIDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetUIFormatDto
import com.ingsis.grupo10.snippet.dto.SnippetUIUpdateRequest
import com.ingsis.grupo10.snippet.producer.LintRequestProducer
import com.ingsis.grupo10.snippet.service.SnippetService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class SnippetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var snippetService: SnippetService

    @MockitoBean
    private lateinit var authClient: AuthClient

    @MockitoBean
    private lateinit var lintRequestProducer: LintRequestProducer

    private val testId = UUID.randomUUID()
    private val testUserId = "test-user-123"

    @Test
    fun `should get all snippets`() {
        val snippets =
            listOf(
                SnippetSummaryDto(
                    id = testId,
                    name = "Test Snippet",
                    language = "PrintScript",
                    version = "1.1",
                    createdAt = LocalDateTime.now(),
                    compliance = "valid",
                ),
            )

        `when`(snippetService.getAllSnippets(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(snippets)

        mockMvc
            .perform(get("/snippets").with(jwt().jwt { it.subject(testUserId) }))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Test Snippet"))
            .andExpect(jsonPath("$[0].language").value("PrintScript"))
    }

    @Test
    fun `should get snippet by id`() {
        val snippetId = testId
        val username = "test-user"

        val snippet =
            SnippetUIDetailDto(
                id = snippetId,
                name = "Test Snippet",
                content = "let x: number = 5;",
                extension = "ps",
                language = "PrintScript",
                compliance = LocalDateTime.now(),
                author = username,
            )

        `when`(snippetService.getUISnippetById(snippetId, username)).thenReturn(snippet)

        mockMvc
            .perform(
                get("/snippets/$snippetId").with(
                    jwt().jwt {
                        it.subject(testUserId)
                        it.claim("https://your-app.com/name", username)
                    },
                ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
    }

    @Test
    fun `should create snippet`() {
        `when`(authClient.checkUserExists(any())).thenReturn(true)
        `when`(authClient.registerSnippet(any(), any())).thenReturn(true)

        val request =
            SnippetUICreateRequest(
                name = "New Snippet",
                content = "let x: number = 5;",
                extension = "ps",
                language = "PrintScript",
            )

        val response =
            Created(
                id = "test-id",
            )

        `when`(snippetService.createSnippet(anyOrNull(), anyOrNull())).thenReturn(response)

        mockMvc
            .perform(
                post("/snippets/create")
                    .with(jwt().jwt { it.subject(testUserId) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("test-id"))
    }

    @Test
    fun `should update snippet`() {
        `when`(authClient.checkPermission(any(), any(), any())).thenReturn(true)

        val request =
            SnippetUIUpdateRequest(
                content = "let y: number = 10;",
            )

        val response =
            SnippetDetailDto(
                id = testId,
                name = "Updated Snippet",
                description = "Updated description",
                codeUrl = "snippets/$testId",
                language = "PrintScript",
                version = "1.1",
                createdAt = LocalDateTime.now(),
            )

        `when`(snippetService.updateSnippet(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                put("/snippets/$testId")
                    .with(jwt().jwt { it.subject(testUserId) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Snippet"))
    }

    @Test
    fun `should delete snippet`() {
        `when`(authClient.checkPermission(any(), any(), any())).thenReturn(true)

        mockMvc
            .perform(delete("/snippets/$testId").with(jwt().jwt { it.subject(testUserId) }))
            .andExpect(status().isOk)
    }

    @Test
    @Disabled("Endpoint is commented out in SnippetController")
    fun `should lint snippet`() {
        `when`(authClient.checkPermission(any(), any(), any())).thenReturn(true)

        mockMvc
            .perform(post("/snippets/$testId/lint").with(jwt().jwt { it.subject(testUserId) }))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.message").value("Lint request queued for processing"))
    }

    @Test
    fun `should format snippet`() {
        `when`(authClient.checkPermission(any(), any(), any())).thenReturn(true)

        val response =
            SnippetUIFormatDto(
                content = "let x: number = 5;",
            )

        `when`(snippetService.formatSnippet(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(post("/snippets/$testId/format").with(jwt().jwt { it.subject(testUserId) }))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").value("let x: number = 5;"))
    }
}
