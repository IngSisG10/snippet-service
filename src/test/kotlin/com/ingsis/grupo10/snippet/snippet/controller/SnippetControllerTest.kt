package com.ingsis.grupo10.snippet.snippet.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.controller.SnippetController
import com.ingsis.grupo10.snippet.dto.Created
import com.ingsis.grupo10.snippet.dto.SnippetCreateRequest
import com.ingsis.grupo10.snippet.dto.SnippetDetailDto
import com.ingsis.grupo10.snippet.dto.SnippetSummaryDto
import com.ingsis.grupo10.snippet.service.SnippetService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
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

@WebMvcTest(SnippetController::class)
class SnippetControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var snippetService: SnippetService

    private val testId = UUID.randomUUID()

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
            .perform(get("/snippets"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Test Snippet"))
            .andExpect(jsonPath("$[0].language").value("PrintScript"))
    }

    @Test
    fun `should get snippet by id`() {
        val snippetId = testId
        val container = "snippets"
        val codeUrl = "$container/$snippetId"

        val snippet =
            SnippetDetailDto(
                id = snippetId,
                name = "Test Snippet",
                description = "Test description",
                codeUrl = codeUrl,
                language = "PrintScript",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
            )

        `when`(snippetService.getSnippetById(snippetId)).thenReturn(snippet)

        mockMvc
            .perform(get("/snippets/$snippetId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.codeUrl").value(codeUrl))
    }

    @Test
    fun `should create snippet`() {
        val snippetId = testId
        val container = "snippets"
        val codeUrl = "$container/$snippetId"

        val request =
            SnippetCreateRequest(
                name = "New Snippet",
                description = "Description",
                code = "let x: number = 5;",
                languageName = "PrintScript",
                version = "1.1",
            )

        val response =
            Created(
                id = snippetId.toString(),
            )

        `when`(snippetService.createSnippet(anyOrNull(), anyOrNull())).thenReturn(response)

        mockMvc
            .perform(
                post("/snippets/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("New Snippet"))
            .andExpect(jsonPath("$.codeUrl").value(codeUrl))
    }

    @Test
    fun `should update snippet`() {
        val request =
            SnippetCreateRequest(
                name = "Updated Snippet",
                description = "Updated description",
                code = "let y: number = 10;",
                languageName = "PrintScript",
                version = "1.1",
            )

        val response =
            SnippetDetailDto(
                id = testId,
                name = "Updated Snippet",
                description = "Updated description",
                codeUrl = "let y: number = 10;", // fixme
                language = "PrintScript",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
            )

        `when`(snippetService.updateSnippet(any(), anyOrNull(), anyOrNull()))
            .thenReturn(response)

        mockMvc
            .perform(
                put("/snippets/$testId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Snippet"))
    }

    @Test
    fun `should delete snippet`() {
        mockMvc
            .perform(delete("/snippets/$testId"))
            .andExpect(status().isOk)
    }

    @Test
    fun `should lint snippet`() {
        val response =
            SnippetDetailDto(
                id = testId,
                name = "Test Snippet",
                description = "Test",
                codeUrl = "let x: number = 5;", // fixme
                language = "PrintScript",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
            )

        `when`(snippetService.lintSnippet(any(), anyOrNull()))
            .thenReturn(response)

        mockMvc
            .perform(post("/snippets/$testId/lint"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
    }

    @Test
    fun `should format snippet`() {
        val response =
            SnippetDetailDto(
                id = testId,
                name = "Test Snippet",
                description = "Test",
                codeUrl = "let x: number = 5;", // fixme
                language = "PrintScript",
                version = "1.1",
                ownerId = UUID.randomUUID(),
                createdAt = LocalDateTime.now(),
            )

        `when`(snippetService.formatSnippet(any(), anyOrNull()))
            .thenReturn(response)

        mockMvc
            .perform(post("/snippets/$testId/format"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
    }
}
