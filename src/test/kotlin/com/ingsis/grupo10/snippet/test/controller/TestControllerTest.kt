package com.ingsis.grupo10.snippet.test.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.controller.TestController
import com.ingsis.grupo10.snippet.dto.TestCreateRequest
import com.ingsis.grupo10.snippet.dto.TestResponseDto
import com.ingsis.grupo10.snippet.service.TestService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
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
import java.util.UUID

@WebMvcTest(TestController::class)
class TestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var testService: TestService

    private val snippetId = UUID.randomUUID()
    private val testId = UUID.randomUUID()

    @Test
    fun `should create test`() {
        val request =
            TestCreateRequest(
                name = "Test Case 1",
                inputs = "5",
                expectedOutputs = "5",
            )

        val response =
            TestResponseDto(
                id = testId,
                snippetId = snippetId,
                name = "Test Case 1",
                inputs = "5",
                expectedOutputs = "5",
            )

        `when`(testService.createTest(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                post("/snippets/$snippetId/tests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Case 1"))
            .andExpect(jsonPath("$.inputs").value("5"))
    }

    @Test
    fun `should get tests by snippet`() {
        val tests =
            listOf(
                TestResponseDto(
                    id = testId,
                    snippetId = snippetId,
                    name = "Test Case 1",
                    inputs = "5",
                    expectedOutputs = "5",
                ),
            )

        `when`(testService.getTestsBySnippet(snippetId)).thenReturn(tests)

        mockMvc
            .perform(get("/snippets/$snippetId/tests"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Test Case 1"))
    }

    @Test
    fun `should get test by id`() {
        val response =
            TestResponseDto(
                id = testId,
                snippetId = snippetId,
                name = "Test Case 1",
                inputs = "5",
                expectedOutputs = "5",
            )

        `when`(testService.getTestById(testId)).thenReturn(response)

        mockMvc
            .perform(get("/snippets/tests/$testId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Case 1"))
    }

    @Test
    fun `should update test`() {
        val request =
            TestCreateRequest(
                name = "Updated Test",
                inputs = "10",
                expectedOutputs = "10",
            )

        val response =
            TestResponseDto(
                id = testId,
                snippetId = snippetId,
                name = "Updated Test",
                inputs = "10",
                expectedOutputs = "10",
            )

        `when`(testService.updateTest(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                put("/snippets/tests/$testId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Test"))
    }

    @Test
    fun `should delete test`() {
        mockMvc
            .perform(delete("/snippets/tests/$testId"))
            .andExpect(status().isNoContent)
    }
}
