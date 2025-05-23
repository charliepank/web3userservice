package com.utility.web3userservice.controller

import com.ninjasquad.springmockk.MockkBean
import com.utility.web3userservice.config.TestSecurityConfig
import com.utility.web3userservice.service.ActivateTokenService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ActivateTokenController::class)
@Import(TestSecurityConfig::class)
class ActivateTokenControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var activateTokenService: ActivateTokenService

    @Test
    fun `createToken returns token string`() {
        // Arrange
        val userId = "test-user"
        val expectedToken = "generated-token"
        every { activateTokenService.createToken(userId) } returns expectedToken

        // Act & Assert
        mockMvc.perform(
            post("/api/token/create/$userId")
                .with(user("test-user"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(expectedToken))

        verify { activateTokenService.createToken(userId) }
    }

    @Test
    fun `activateToken returns true when activation successful`() {
        // Arrange
        val token = "valid-token"
        every { activateTokenService.activateToken(token) } returns true

        // Act & Assert
        mockMvc.perform(
            post("/api/token/activate/$token")
                .with(user("test-user"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))

        verify { activateTokenService.activateToken(token) }
    }

    @Test
    fun `activateToken returns false when activation fails`() {
        // Arrange
        val token = "invalid-token"
        every { activateTokenService.activateToken(token) } returns false

        // Act & Assert
        mockMvc.perform(
            post("/api/token/activate/$token")
                .with(user("test-user"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("false"))

        verify { activateTokenService.activateToken(token) }
    }

    @Test
    fun `validateToken returns true for valid token`() {
        // Arrange
        val token = "valid-token"
        every { activateTokenService.validateToken(token) } returns true

        // Act & Assert
        mockMvc.perform(
            get("/api/token/validate/$token")
                .with(user("test-user"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("true"))

        verify { activateTokenService.validateToken(token) }
    }

    @Test
    fun `validateToken returns false for invalid token`() {
        // Arrange
        val token = "invalid-token"
        every { activateTokenService.validateToken(token) } returns false

        // Act & Assert
        mockMvc.perform(
            get("/api/token/validate/$token")
                .with(user("test-user"))
        )
            .andExpect(status().isOk)
            .andExpect(content().string("false"))

        verify { activateTokenService.validateToken(token) }
    }
}