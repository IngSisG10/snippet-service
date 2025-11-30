package com.ingsis.grupo10.snippet.config.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.controller.LintConfigController
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigRequest
import com.ingsis.grupo10.snippet.dto.lintconfig.LintConfigResponse
import com.ingsis.grupo10.snippet.service.LintConfigService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(LintConfigController::class)
class LintConfigControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var lintConfigService: LintConfigService

    private val testUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Test
    fun `should get lint config`() {
        val response =
            LintConfigResponse(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                identifierFormat = "camel case",
                printlnExpressionAllowed = true,
                readInputExpressionAllowed = true,
            )

        `when`(lintConfigService.getConfig(testUserId.toString())).thenReturn(response)

        mockMvc
            .perform(get("/config/linting").with(jwt().jwt { it.subject(testUserId.toString()) }))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.identifierFormat").value("camel case"))
            .andExpect(jsonPath("$.printlnExpressionAllowed").value(true))
    }

    @Test
    fun `should update lint config`() {
        val request =
            LintConfigRequest(
                identifierFormat = "snake case",
                printlnExpressionAllowed = false,
            )

        val response =
            LintConfigResponse(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                identifierFormat = "snake case",
                printlnExpressionAllowed = false,
                readInputExpressionAllowed = true,
            )

        `when`(lintConfigService.updateConfig(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                put("/config/linting")
                    .with(jwt().jwt { it.subject(testUserId.toString()) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.identifierFormat").value("snake case"))
            .andExpect(jsonPath("$.printlnExpressionAllowed").value(false))
    }
}
