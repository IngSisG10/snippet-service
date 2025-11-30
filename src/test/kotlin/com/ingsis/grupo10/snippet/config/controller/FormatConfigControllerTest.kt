package com.ingsis.grupo10.snippet.config.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsis.grupo10.snippet.controller.FormatConfigController
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigRequest
import com.ingsis.grupo10.snippet.dto.formatconfig.FormatConfigResponse
import com.ingsis.grupo10.snippet.service.FormatConfigService
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

@WebMvcTest(FormatConfigController::class)
class FormatConfigControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var formatConfigService: FormatConfigService

    private val testUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Test
    fun `should get format config`() {
        val response =
            FormatConfigResponse(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                spaceBeforeColon = false,
                spaceAfterColon = true,
                spaceAroundEquals = true,
                newlineBeforePrintln = 1,
                indentInsideBlock = 4,
            )

        `when`(formatConfigService.getConfig(testUserId.toString())).thenReturn(response)

        mockMvc
            .perform(get("/config/formatting").with(jwt().jwt { it.subject(testUserId.toString()) }))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(testUserId.toString()))
            .andExpect(jsonPath("$.spaceBeforeColon").value(false))
            .andExpect(jsonPath("$.spaceAfterColon").value(true))
            .andExpect(jsonPath("$.indentInsideBlock").value(4))
    }

    @Test
    fun `should update format config`() {
        val request =
            FormatConfigRequest(
                spaceBeforeColon = true,
                indentInsideBlock = 2,
            )

        val response =
            FormatConfigResponse(
                id = UUID.randomUUID(),
                userId = testUserId.toString(),
                spaceBeforeColon = true,
                spaceAfterColon = true,
                spaceAroundEquals = true,
                newlineBeforePrintln = 1,
                indentInsideBlock = 2,
            )

        `when`(formatConfigService.updateConfig(any(), any()))
            .thenReturn(response)

        mockMvc
            .perform(
                put("/config/formatting")
                    .with(jwt().jwt { it.subject(testUserId.toString()) })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.spaceBeforeColon").value(true))
            .andExpect(jsonPath("$.indentInsideBlock").value(2))
    }
}
